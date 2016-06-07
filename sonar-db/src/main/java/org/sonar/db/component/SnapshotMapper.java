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
package org.sonar.db.component;

import java.util.List;
import javax.annotation.CheckForNull;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

public interface SnapshotMapper {

  @CheckForNull
  SnapshotDto selectByKey(long id);

  List<SnapshotDto> selectByIds(@Param("ids") List<Long> ids);

  void insert(SnapshotDto snapshot);

  @CheckForNull
  SnapshotDto selectLastSnapshot(Long resourceId);

  List<SnapshotDto> selectLastSnapshotByComponentIds(List<Long> componentIds);

  int countLastSnapshotByComponentUuid(String componentUuid);

  @CheckForNull
  SnapshotDto selectLastSnapshotByComponentUuid(String componentUuid);

  List<SnapshotDto> selectSnapshotsByQuery(@Param("query") SnapshotQuery query);

  List<SnapshotDto> selectPreviousVersionSnapshots(@Param(value = "componentId") Long componentId, @Param(value = "lastVersion") String lastVersion);

  List<SnapshotDto> selectOldestSnapshots(@Param(value = "componentId") Long componentId, RowBounds rowBounds);

  List<SnapshotDto> selectSnapshotAndChildrenOfScope(@Param(value = "snapshot") Long resourceId, @Param(value = "scope") String scope);

  int updateSnapshotAndChildrenLastFlagAndStatus(@Param(value = "root") Long rootId, @Param(value = "pathRootId") Long pathRootId,
    @Param(value = "path") String path, @Param(value = "isLast") boolean isLast, @Param(value = "status") String status);

  int updateSnapshotAndChildrenLastFlag(@Param(value = "root") Long rootId, @Param(value = "pathRootId") Long pathRootId,
    @Param(value = "path") String path, @Param(value = "isLast") boolean isLast);

  List<ViewsSnapshotDto> selectSnapshotBefore(@Param("componentId") long componentId, @Param("date") long date);

  ViewsSnapshotDto selectLatestSnapshot(@Param("componentId") long componentId);
}
