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
package org.sonar.db.version.v60;

import com.google.common.annotations.VisibleForTesting;
import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.DdlChange;
import org.sonar.db.version.DropColumnsBuilder;

/**
 * Drop the following columns from the project_measures table :
 * - rule_id
 * - rule_category_id
 * - tendency
 * - url
 * - measure_date
 * - url
 * - rule_priority
 * - Characteristic_id
 */
public class RemoveUnusedMeasuresColumns extends DdlChange {

  public RemoveUnusedMeasuresColumns(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    context.execute(generateSql());
  }

  @VisibleForTesting
  String generateSql() {
    return new DropColumnsBuilder(getDatabase().getDialect(), "project_measures",
      "rule_id", "rules_category_id", "tendency", "measure_date", "url", "rule_priority", "characteristic_id")
      .build();
  }

}