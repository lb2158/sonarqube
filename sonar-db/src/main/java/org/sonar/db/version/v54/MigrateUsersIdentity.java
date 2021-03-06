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
package org.sonar.db.version.v54;

import java.sql.SQLException;
import org.sonar.api.utils.System2;
import org.sonar.db.Database;
import org.sonar.db.version.BaseDataChange;
import org.sonar.db.version.MassUpdate;
import org.sonar.db.version.Select;
import org.sonar.db.version.SqlStatement;

/**
 * Update all users to feed external_identity_provider with 'sonarqube' and external_identity with the login
 */
public class MigrateUsersIdentity extends BaseDataChange {

  private final System2 system2;

  public MigrateUsersIdentity(Database db, System2 system2) {
    super(db);
    this.system2 = system2;
  }

  @Override
  public void execute(Context context) throws SQLException {
    MassUpdate update = context.prepareMassUpdate().rowPluralName("users");
    update.select("SELECT u.id, u.login FROM users u");
    update.update("UPDATE users SET external_identity_provider=?, external_identity=?, updated_at=? WHERE id=? " +
      "AND external_identity_provider IS NULL AND external_identity IS NULL");
    update.execute(new MigrationHandler(system2.now()));
  }

  private static class MigrationHandler implements MassUpdate.Handler {
    private final long now;

    public MigrationHandler(long now) {
      this.now = now;
    }

    @Override
    public boolean handle(Select.Row row, SqlStatement update) throws SQLException {
      update.setString(1, "sonarqube");
      update.setString(2, row.getString(2));
      update.setLong(3, now);
      update.setLong(4, row.getLong(1));
      return true;
    }
  }
}
