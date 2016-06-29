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
import React from 'react';
import UserHolder from './UserHolder';
import GroupHolder from './GroupHolder';

export default class HoldersList extends React.Component {
  static propTypes = {
    permissions: React.PropTypes.array.isRequired,
    users: React.PropTypes.array.isRequired,
    groups: React.PropTypes.array.isRequired,
    onToggleUser: React.PropTypes.func.isRequired,
    onToggleGroup: React.PropTypes.func.isRequired
  };

  renderTableHeader () {
    const cells = this.props.permissions.map(p => (
        <th key={p.key} className="permission-column text-center">
          {p.name}
          <i className="icon-help little-spacer-left"
             title={p.description}
             data-toggle="tooltip"/>
        </th>
    ));
    return (
        <thead>
          <tr>
            <td className="bordered-bottom">
              {this.props.children}
              </td>
            {cells}
          </tr>
        </thead>
    );
  }

  render () {
    const users = this.props.users.map(user => (
        <UserHolder
            key={'user-' + user.login}
            user={user}
            permissions={user.permissions}
            permissionsOrder={this.props.permissions}
            onToggle={this.props.onToggleUser}/>
    ));

    const groups = this.props.groups.map(group => (
        <GroupHolder
            key={'group-' + group.id}
            group={group}
            permissions={group.permissions}
            permissionsOrder={this.props.permissions}
            onToggle={this.props.onToggleGroup}/>
    ));

    return (
        <table className="data zebra permissions-table">
          {this.renderTableHeader()}
          <tbody>
            {users}
            {groups}
          </tbody>
        </table>
    );
  }
}
