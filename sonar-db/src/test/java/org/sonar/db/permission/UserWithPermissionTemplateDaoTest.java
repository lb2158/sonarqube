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
package org.sonar.db.permission;

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.permission.PermissionTemplateQuery.builder;

public class UserWithPermissionTemplateDaoTest {

  private static final Long TEMPLATE_ID = 50L;

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);

  DbSession dbSession = dbTester.getSession();

  PermissionTemplateDao dao = dbTester.getDbClient().permissionTemplateDao();

  @Test
  public void select_users() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUsers2(dbSession, builder().build(),
      TEMPLATE_ID, 0, 10)).containsOnly("user1", "user2", "user3");
    assertThat(dao.selectUsers2(dbSession, builder().withPermissionOnly().setPermission("user").build(),
      TEMPLATE_ID, 0, 10)).containsOnly("user1", "user2");
  }

  @Test
  public void return_no_users_on_unknown_template_key() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUsers2(dbSession, builder().setPermission("user").withPermissionOnly().build(), 999L, 0, 10)).isEmpty();
  }

  @Test
  public void select_only_user_with_permission() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUsers2(
      dbSession,
      builder().setPermission("user").withPermissionOnly().build(),
      TEMPLATE_ID, 0, 10)).containsOnly("user1", "user2");
  }

  @Test
  public void select_only_enable_users() {
    dbTester.prepareDbUnit(getClass(), "select_only_enable_users.xml");

    List<String> result = dao.selectUsers2(dbSession, builder().setPermission("user").build(), TEMPLATE_ID, 0, 10);
    assertThat(result).hasSize(2);

    // Disabled user should not be returned
    assertThat(result.stream().filter(input -> input.equals("disabledUser")).findFirst()).isEmpty();
  }

  @Test
  public void search_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    List<String> result = dao.selectUsers2(
      dbSession, builder().withPermissionOnly().setPermission("user").setSearchQuery("SEr1").build(),
      TEMPLATE_ID, 0, 10);
    assertThat(result).containsOnly("user1");

    result = dao.selectUsers2(
      dbSession, builder().withPermissionOnly().setPermission("user").setSearchQuery("user").build(),
      TEMPLATE_ID, 0, 10);
    assertThat(result).hasSize(2);
  }

  @Test
  public void should_be_sorted_by_user_name() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions_should_be_sorted_by_user_name.xml");

    assertThat(dao.selectUsers2(dbSession, builder().build(), TEMPLATE_ID, 0, 10)).containsOnly("user1", "user2", "user3");
  }

  @Test
  public void should_be_paginated() {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.selectUsers2(dbSession, builder().build(), TEMPLATE_ID, 0, 2)).containsOnly("user1", "user2");
    assertThat(dao.selectUsers2(dbSession, builder().build(), TEMPLATE_ID, 1, 2)).containsOnly("user2", "user3");
    assertThat(dao.selectUsers2(dbSession, builder().build(), TEMPLATE_ID, 2, 1)).containsOnly("user3");
  }

  @Test
  public void count_users() throws Exception {
    dbTester.prepareDbUnit(getClass(), "users_with_permissions.xml");

    assertThat(dao.countUsers2(dbSession, builder().build(), TEMPLATE_ID)).isEqualTo(3);
    assertThat(dao.countUsers2(dbSession, builder().withPermissionOnly().setPermission("user").build(), TEMPLATE_ID)).isEqualTo(2);
  }

}
