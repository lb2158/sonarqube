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
package org.sonar.api.batch.sensor.internal;

import com.google.common.annotations.Beta;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import org.sonar.api.SonarProduct;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarQubeVersion;
import org.sonar.api.batch.fs.InputModule;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputModule;
import org.sonar.api.batch.fs.internal.DefaultTextPointer;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.coverage.CoverageType;
import org.sonar.api.batch.sensor.coverage.NewCoverage;
import org.sonar.api.batch.sensor.coverage.internal.DefaultCoverage;
import org.sonar.api.batch.sensor.cpd.NewCpdTokens;
import org.sonar.api.batch.sensor.cpd.internal.DefaultCpdTokens;
import org.sonar.api.batch.sensor.error.AnalysisError;
import org.sonar.api.batch.sensor.error.NewAnalysisError;
import org.sonar.api.batch.sensor.error.internal.DefaultAnalysisError;
import org.sonar.api.batch.sensor.highlighting.NewHighlighting;
import org.sonar.api.batch.sensor.highlighting.TypeOfText;
import org.sonar.api.batch.sensor.highlighting.internal.DefaultHighlighting;
import org.sonar.api.batch.sensor.highlighting.internal.SyntaxHighlightingRule;
import org.sonar.api.batch.sensor.issue.Issue;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssue;
import org.sonar.api.batch.sensor.measure.Measure;
import org.sonar.api.batch.sensor.measure.NewMeasure;
import org.sonar.api.batch.sensor.measure.internal.DefaultMeasure;
import org.sonar.api.batch.sensor.symbol.NewSymbolTable;
import org.sonar.api.batch.sensor.symbol.internal.DefaultSymbolTable;
import org.sonar.api.config.Settings;
import org.sonar.api.internal.SonarRuntimeFactory;
import org.sonar.api.measures.Metric;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.Version;
import org.sonar.duplications.internal.pmd.TokensLine;

/**
 * Utility class to help testing {@link Sensor}. This is not an API and method signature may evolve.
 * 
 * Usage: call {@link #create(File)} to create an "in memory" implementation of {@link SensorContext} with a filesystem initialized with provided baseDir.
 * <p>
 * You have to manually register inputFiles using:
 * <pre>
 *   sensorContextTester.fileSystem().add(new DefaultInputFile("myProjectKey", "src/Foo.java")
      .setLanguage("java")
      .initMetadata("public class Foo {\n}"));
 * </pre>
 * <p>
 * Then pass it to your {@link Sensor}. You can then query elements provided by your sensor using methods {@link #allIssues()}, ...
 * 
 */
@Beta
public class SensorContextTester implements SensorContext {

  private Settings settings;
  private DefaultFileSystem fs;
  private ActiveRules activeRules;
  private InMemorySensorStorage sensorStorage;
  private InputModule module;
  private SonarQubeVersion sqVersion;

  private SensorContextTester(Path moduleBaseDir) {
    this.settings = new Settings();
    this.fs = new DefaultFileSystem(moduleBaseDir);
    this.activeRules = new ActiveRulesBuilder().build();
    this.sensorStorage = new InMemorySensorStorage();
    this.module = new DefaultInputModule("projectKey");
    this.sqVersion = SonarRuntimeFactory.create(System2.INSTANCE, SonarProduct.SONARQUBE, SonarQubeSide.SCANNER);
  }

  public static SensorContextTester create(File moduleBaseDir) {
    return new SensorContextTester(moduleBaseDir.toPath());
  }

  public static SensorContextTester create(Path moduleBaseDir) {
    return new SensorContextTester(moduleBaseDir);
  }

  @Override
  public Settings settings() {
    return settings;
  }

  public SensorContextTester setSettings(Settings settings) {
    this.settings = settings;
    return this;
  }

  @Override
  public DefaultFileSystem fileSystem() {
    return fs;
  }

  public SensorContextTester setFileSystem(DefaultFileSystem fs) {
    this.fs = fs;
    return this;
  }

  @Override
  public ActiveRules activeRules() {
    return activeRules;
  }

  public SensorContextTester setActiveRules(ActiveRules activeRules) {
    this.activeRules = activeRules;
    return this;
  }

  /**
   * Default value is the version of this API. You can override it
   * using {@link #setSonarQubeVersion(Version)} to test your Sensor behavior.
   */
  @Override
  public Version getSonarQubeVersion() {
    return sqVersion.getApiVersion();
  }

  @Override
  public Version getRuntimeApiVersion() {
    return sqVersion.getApiVersion();
  }

  @Override
  public SonarProduct getRuntimeProduct() {
    return sqVersion.getProduct();
  }

  public SensorContextTester setRuntime(Version version, SonarProduct product, SonarQubeSide sonarQubeSide) {
    this.sqVersion = new SonarQubeVersion(version, product, sonarQubeSide);
    return this;
  }

  @Override
  public InputModule module() {
    return module;
  }

  @Override
  public <G extends Serializable> NewMeasure<G> newMeasure() {
    return new DefaultMeasure<>(sensorStorage);
  }

  public Collection<Measure> measures(String componentKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).values();
  }

  public <G extends Serializable> Measure<G> measure(String componetKey, Metric<G> metric) {
    return measure(componetKey, metric.key());
  }

  public <G extends Serializable> Measure<G> measure(String componentKey, String metricKey) {
    return sensorStorage.measuresByComponentAndMetric.row(componentKey).get(metricKey);
  }

  @Override
  public NewIssue newIssue() {
    return new DefaultIssue(sensorStorage);
  }

  public Collection<Issue> allIssues() {
    return sensorStorage.allIssues;
  }
  
  public Collection<AnalysisError> allAnalysisErrors() {
    return sensorStorage.allAnalysisErrors;
  }

  @CheckForNull
  public Integer lineHits(String fileKey, CoverageType type, int line) {
    DefaultCoverage defaultCoverage = sensorStorage.coverageByComponentAndType.get(fileKey, type);
    if (defaultCoverage == null) {
      return null;
    }
    return defaultCoverage.hitsByLine().get(line);
  }

  @CheckForNull
  public Integer conditions(String fileKey, CoverageType type, int line) {
    DefaultCoverage defaultCoverage = sensorStorage.coverageByComponentAndType.get(fileKey, type);
    if (defaultCoverage == null) {
      return null;
    }
    return defaultCoverage.conditionsByLine().get(line);
  }

  @CheckForNull
  public Integer coveredConditions(String fileKey, CoverageType type, int line) {
    DefaultCoverage defaultCoverage = sensorStorage.coverageByComponentAndType.get(fileKey, type);
    if (defaultCoverage == null) {
      return null;
    }
    return defaultCoverage.coveredConditionsByLine().get(line);
  }

  @CheckForNull
  public List<TokensLine> cpdTokens(String componentKey) {
    DefaultCpdTokens defaultCpdTokens = sensorStorage.cpdTokensByComponent.get(componentKey);
    return defaultCpdTokens != null ? defaultCpdTokens.getTokenLines() : null;
  }

  @Override
  public NewHighlighting newHighlighting() {
    return new DefaultHighlighting(sensorStorage);
  }

  @Override
  public NewCoverage newCoverage() {
    return new DefaultCoverage(sensorStorage);
  }

  @Override
  public NewCpdTokens newCpdTokens() {
    return new DefaultCpdTokens(settings, sensorStorage);
  }

  @Override
  public NewSymbolTable newSymbolTable() {
    return new DefaultSymbolTable(sensorStorage);
  }
  
  @Override
  public NewAnalysisError newAnalysisError() {
    return new DefaultAnalysisError(sensorStorage);
  }

  /**
   * Return list of syntax highlighting applied for a given position in a file. The result is a list because in theory you
   * can apply several styles to the same range.
   * @param componentKey Key of the file like 'myProjectKey:src/foo.php'
   * @param line Line you want to query
   * @param lineOffset Offset you want to query.
   * @return List of styles applied to this position or empty list if there is no highlighting at this position.
   */
  public List<TypeOfText> highlightingTypeAt(String componentKey, int line, int lineOffset) {
    DefaultHighlighting syntaxHighlightingData = sensorStorage.highlightingByComponent.get(componentKey);
    if (syntaxHighlightingData == null) {
      return Collections.emptyList();
    }
    List<TypeOfText> result = new ArrayList<>();
    DefaultTextPointer location = new DefaultTextPointer(line, lineOffset);
    for (SyntaxHighlightingRule sortedRule : syntaxHighlightingData.getSyntaxHighlightingRuleSet()) {
      if (sortedRule.range().start().compareTo(location) <= 0 && sortedRule.range().end().compareTo(location) > 0) {
        result.add(sortedRule.getTextType());
      }
    }
    return result;
  }

  /**
   * Return list of symbol references ranges for the symbol at a given position in a file.
   * @param componentKey Key of the file like 'myProjectKey:src/foo.php'
   * @param line Line you want to query
   * @param lineOffset Offset you want to query.
   * @return List of references for the symbol (potentially empty) or null if there is no symbol at this position.
   */
  @CheckForNull
  public Collection<TextRange> referencesForSymbolAt(String componentKey, int line, int lineOffset) {
    DefaultSymbolTable symbolTable = sensorStorage.symbolsPerComponent.get(componentKey);
    if (symbolTable == null) {
      return null;
    }
    DefaultTextPointer location = new DefaultTextPointer(line, lineOffset);
    for (Map.Entry<TextRange, Set<TextRange>> symbol : symbolTable.getReferencesBySymbol().entrySet()) {
      if (symbol.getKey().start().compareTo(location) <= 0 && symbol.getKey().end().compareTo(location) > 0) {
        return symbol.getValue();
      }
    }
    return null;
  }

}
