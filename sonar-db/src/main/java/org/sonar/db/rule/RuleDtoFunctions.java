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

package org.sonar.db.rule;

import com.google.common.base.Function;
import javax.annotation.Nonnull;

public class RuleDtoFunctions {
  private RuleDtoFunctions() {
    // prevent instantiation
  }

  public static Function<RuleDto, Integer> toId() {
    return ToId.INSTANCE;
  }

  private enum ToId implements Function<RuleDto, Integer> {
    INSTANCE;

    @Override
    public Integer apply(@Nonnull RuleDto rule) {
      return rule.getId();
    }
  }
}
