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
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * @since 3.7
 */
public interface PermissionTemplateMapper {

  void insert(PermissionTemplateDto permissionTemplate);

  void update(PermissionTemplateDto permissionTemplate);

  void delete(long templateId);

  void deleteUserPermissions(long templateId);

  void deleteUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void deleteGroupPermissions(long templateId);

  void deleteGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  PermissionTemplateDto selectByUuid(String templateUuid);

  List<PermissionTemplateUserDto> selectUserPermissionsByTemplateId(long templateId);

  List<PermissionTemplateGroupDto> selectGroupPermissionsByTemplateId(long templateId);

  void insertUserPermission(PermissionTemplateUserDto permissionTemplateUser);

  void insertGroupPermission(PermissionTemplateGroupDto permissionTemplateGroup);

  void deleteByGroupId(long groupId);

  List<GroupWithPermissionDto> selectGroups(@Param("query") PermissionQuery query, @Param("templateId") long templateId, @Param("anyoneGroup") String anyoneGroup,
    @Param("projectAdminPermission") String projectAdminPermission, RowBounds rowBounds);

  List<UserWithPermissionDto> selectUsers(@Param("query") PermissionQuery query, @Param("templateId") long templateId, RowBounds rowBounds);

  PermissionTemplateDto selectByName(String name);

  int countUsers(@Param("query") PermissionQuery query, @Param("templateId") long templateId);

  int countGroups(@Param("query") PermissionQuery query, @Param("templateId") long templateId, @Param("anyoneGroup") String anyoneGroup,
    @Param("projectAdminPermission") String projectAdminPermission, @Nullable @Param("groupName") String groupName);

  List<PermissionTemplateDto> selectAll(@Param("nameMatch") String nameMatch);

  int countAll(@Param("nameMatch") String nameMatch);

  void usersCountByTemplateIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  void groupsCountByTemplateIdAndPermission(Map<String, Object> parameters, ResultHandler resultHandler);

  List<String> selectPotentialPermissionsByUserIdAndTemplateId(@Param("userId") @Nullable Long currentUserId, @Param("templateId") long templateId);
}
