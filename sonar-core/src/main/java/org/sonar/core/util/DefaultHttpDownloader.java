/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.util;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.sonar.api.config.Settings;
import org.sonar.api.platform.Server;
import org.sonar.api.utils.HttpDownloader;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.log.Loggers;

import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.sonar.core.util.FileUtils.deleteQuietly;

/**
 * This component downloads HTTP files
 *
 * @since 2.2
 */
public class DefaultHttpDownloader extends HttpDownloader {

  private final BaseHttpDownloader downloader;
  private final Integer readTimeout;
  private final Integer connectTimeout;

  public DefaultHttpDownloader(Server server, Settings settings) {
    this(server, settings, null);
  }

  public DefaultHttpDownloader(Server server, Settings settings, @Nullable Integer readTimeout) {
    this(server, settings, null, readTimeout);
  }

  public DefaultHttpDownloader(Server server, Settings settings, @Nullable Integer connectTimeout, @Nullable Integer readTimeout) {
    this.readTimeout = readTimeout;
    this.connectTimeout = connectTimeout;
    downloader = new BaseHttpDownloader(new SystemFacade(), settings, server.getVersion());
  }

  public DefaultHttpDownloader(Settings settings) {
    this(settings, null);
  }

  public DefaultHttpDownloader(Settings settings, @Nullable Integer readTimeout) {
    this(settings, null, readTimeout);
  }

  public DefaultHttpDownloader(Settings settings, @Nullable Integer connectTimeout, @Nullable Integer readTimeout) {
    this.readTimeout = readTimeout;
    this.connectTimeout = connectTimeout;
    downloader = new BaseHttpDownloader(new SystemFacade(), settings, null);
  }

  @Override
  protected String description(URI uri) {
    return String.format("%s (%s)", uri.toString(), getProxySynthesis(uri));
  }

  @Override
  protected String[] getSupportedSchemes() {
    return new String[] {"http", "https"};
  }

  @Override
  protected byte[] readBytes(URI uri) {
    return download(uri);
  }

  @Override
  protected String readString(URI uri, Charset charset) {
    try {
      return IOUtils.toString(downloader.newInputSupplier(uri, this.connectTimeout, this.readTimeout).getInput(), charset);
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  public String downloadPlainText(URI uri, String encoding) {
    return readString(uri, Charset.forName(encoding));
  }

  @Override
  public byte[] download(URI uri) {
    try {
      return ByteStreams.toByteArray(downloader.newInputSupplier(uri, this.connectTimeout, this.readTimeout).getInput());
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  public String getProxySynthesis(URI uri) {
    return BaseHttpDownloader.getProxySynthesis(uri);
  }

  @Override
  public InputStream openStream(URI uri) {
    try {
      return downloader.newInputSupplier(uri, this.connectTimeout, this.readTimeout).getInput();
    } catch (IOException e) {
      throw failToDownload(uri, e);
    }
  }

  @Override
  public void download(URI uri, File toFile) {
    try {
      copyInputStreamToFile(downloader.newInputSupplier(uri, this.connectTimeout, this.readTimeout).getInput(), toFile);
    } catch (IOException e) {
      deleteQuietly(toFile);
      throw failToDownload(uri, e);
    }
  }

  private SonarException failToDownload(URI uri, IOException e) {
    throw new SonarException(String.format("Fail to download: %s (%s)", uri, getProxySynthesis(uri)), e);
  }

  /**
   * Facade to allow unit tests to verify calls to {@link System}.
   * This class could be replaced by {@link org.sonar.api.utils.System2},
   * but it sounds overkill to define {@link #setDefaultAuthenticator(Authenticator)}.
   */
  static class SystemFacade {
    public void setProperty(String key, String value) {
      System.setProperty(key, value);
    }

    public void setDefaultAuthenticator(Authenticator authenticator) {
      Authenticator.setDefault(authenticator);
    }
  }

  static class BaseHttpDownloader {

    private static final String GET = "GET";
    private static final String HTTP_PROXY_USER = "http.proxyUser";
    private static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";
    private static final String HTTP_PROXY_HOST = "http.proxyHost";
    private static final String HTTPS_PROXY_HOST = "https.proxyHost";
    private static final String HTTP_PROXY_PORT = "http.proxyPort";
    private static final String HTTPS_PROXY_PORT = "https.proxyPort";

    private static final List<String> PROXY_SETTINGS = ImmutableList.of(
      HTTP_PROXY_HOST, HTTP_PROXY_PORT, "http.nonProxyHosts",
      HTTPS_PROXY_HOST, HTTPS_PROXY_PORT,
      "http.auth.ntlm.domain", "socksProxyHost", "socksProxyPort");

    private String userAgent;

    BaseHttpDownloader(SystemFacade system, Settings settings, @Nullable String userAgent) {
      initProxy(system, settings);
      initUserAgent(userAgent);
    }

    private void initProxy(SystemFacade system, Settings settings) {
      // propagate system properties
      for (String key : PROXY_SETTINGS) {
        if (settings.hasKey(key)) {
          system.setProperty(key, settings.getString(key));
        }
      }
      // defaults of HTTPS properties are the values of HTTP properties
      setSystemPropertyToDefaultIfNotSet(system, settings, HTTPS_PROXY_HOST, HTTP_PROXY_HOST);
      setSystemPropertyToDefaultIfNotSet(system, settings, HTTPS_PROXY_PORT, HTTP_PROXY_PORT);

      // register credentials
      String login = settings.getString(HTTP_PROXY_USER);
      if (isNotEmpty(login)) {
        system.setDefaultAuthenticator(new ProxyAuthenticator(login, settings.getString(HTTP_PROXY_PASSWORD)));
      }
    }

    private static void setSystemPropertyToDefaultIfNotSet(SystemFacade system, Settings settings, String httpsProperty, String httpProperty) {
      if (!settings.hasKey(httpsProperty) && settings.hasKey(httpProperty)) {
        system.setProperty(httpsProperty, settings.getString(httpProperty));
      }
    }

    private void initUserAgent(@Nullable String sonarVersion) {
      userAgent = sonarVersion == null ? "SonarQube" : String.format("SonarQube %s", sonarVersion);
      System.setProperty("http.agent", userAgent);
    }

    private static String getProxySynthesis(URI uri) {
      return getProxySynthesis(uri, ProxySelector.getDefault());
    }

    @VisibleForTesting
    static String getProxySynthesis(URI uri, ProxySelector proxySelector) {
      List<Proxy> proxies = proxySelector.select(uri);
      if (proxies.size() == 1 && proxies.get(0).type().equals(Proxy.Type.DIRECT)) {
        return "no proxy";
      }

      List<String> descriptions = Lists.newArrayList();
      for (Proxy proxy : proxies) {
        if (proxy.type() != Proxy.Type.DIRECT) {
          descriptions.add(proxy.type() + " proxy: " + proxy.address());
        }
      }

      return Joiner.on(", ").join(descriptions);
    }

    public HttpInputSupplier newInputSupplier(URI uri, @Nullable Integer connectTimeoutMillis, @Nullable Integer readTimeoutMillis) {
      return newInputSupplier(uri, GET, connectTimeoutMillis, readTimeoutMillis);
    }

    public HttpInputSupplier newInputSupplier(URI uri, String requestMethod, @Nullable Integer connectTimeoutMillis, @Nullable Integer readTimeoutMillis) {
      return newInputSupplier(uri, requestMethod, null, null, connectTimeoutMillis, readTimeoutMillis);
    }

    public HttpInputSupplier newInputSupplier(URI uri, String requestMethod, String login, String password, @Nullable Integer connectTimeoutMillis,
      @Nullable Integer readTimeoutMillis) {
      int read = readTimeoutMillis != null ? readTimeoutMillis : TIMEOUT_MILLISECONDS;
      int connect = connectTimeoutMillis != null ? connectTimeoutMillis : TIMEOUT_MILLISECONDS;
      return new HttpInputSupplier(uri, requestMethod, userAgent, login, password, connect, read);
    }

    private static class HttpInputSupplier {
      private final String login;
      private final String password;
      private final URI uri;
      private final String userAgent;
      private final int connectTimeoutMillis;
      private final int readTimeoutMillis;
      private final String requestMethod;

      HttpInputSupplier(URI uri, String requestMethod, String userAgent, String login, String password, int connectTimeoutMillis, int readTimeoutMillis) {
        this.uri = uri;
        this.requestMethod = requestMethod;
        this.userAgent = userAgent;
        this.login = login;
        this.password = password;
        this.readTimeoutMillis = readTimeoutMillis;
        this.connectTimeoutMillis = connectTimeoutMillis;
      }

      /**
       * @throws IOException any I/O error, not limited to the network connection
       * @throws HttpException if HTTP response code > 400
       */
      public InputStream getInput() throws IOException {
        Loggers.get(getClass()).debug("Download: " + uri + " (" + getProxySynthesis(uri, ProxySelector.getDefault()) + ")");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod(requestMethod);
        HttpsTrust.INSTANCE.trust(connection);

        // allow both GZip and Deflate (ZLib) encodings
        connection.setRequestProperty("Accept-Encoding", "gzip");
        if (!Strings.isNullOrEmpty(login)) {
          String encoded = Base64.encodeBase64String((login + ":" + password).getBytes(StandardCharsets.UTF_8));
          connection.setRequestProperty("Authorization", "Basic " + encoded);
        }
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", userAgent);

        // establish connection, get response headers
        connection.connect();

        // obtain the encoding returned by the server
        String encoding = connection.getContentEncoding();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
          InputStream errorResponse = null;
          try {
            errorResponse = connection.getErrorStream();
            if (errorResponse != null) {
              String errorResponseContent = IOUtils.toString(errorResponse);
              throw new HttpException(uri, responseCode, errorResponseContent);
            }
            throw new HttpException(uri, responseCode);

          } finally {
            IOUtils.closeQuietly(errorResponse);
          }
        }

        InputStream resultingInputStream;
        // create the appropriate stream wrapper based on the encoding type
        if (encoding != null && "gzip".equalsIgnoreCase(encoding)) {
          resultingInputStream = new GZIPInputStream(connection.getInputStream());
        } else {
          resultingInputStream = connection.getInputStream();
        }
        return resultingInputStream;
      }
    }
  }

  static class ProxyAuthenticator extends Authenticator {
    private final PasswordAuthentication auth;

    ProxyAuthenticator(String user, @Nullable String password) {
      auth = new PasswordAuthentication(user, password == null ? new char[0] : password.toCharArray());
    }

    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
      return auth;
    }
  }

}
