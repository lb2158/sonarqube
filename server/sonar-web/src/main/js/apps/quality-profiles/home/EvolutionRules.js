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
import moment from 'moment';
import sortBy from 'lodash/sortBy';
import { searchRules } from '../../../api/rules';
import { translateWithParameters, translate } from '../../../helpers/l10n';
import { getRulesUrl } from '../../../helpers/urls';

const RULES_LIMIT = 3;

const PERIOD_START_MOMENT = moment().subtract(1, 'month');

function parseRules (r) {
  const { rules, actives } = r;
  return rules.map(rule => {
    const activations = actives[rule.key];
    return { ...rule, activations: activations ? activations.length : 0 };
  });
}

export default class EvolutionRules extends React.Component {
  state = {};

  componentDidMount () {
    this.mounted = true;
    this.loadLatestRules();
  }

  componentWillUnmount () {
    this.mounted = false;
  }

  loadLatestRules () {
    const data = {
      'available_since': PERIOD_START_MOMENT.format('YYYY-MM-DD'),
      s: 'createdAt',
      asc: false,
      ps: RULES_LIMIT,
      f: 'name,langName,actives'
    };

    searchRules(data).then(r => {
      if (this.mounted) {
        this.setState({
          latestRules: sortBy(parseRules(r), 'langName'),
          latestRulesTotal: r.total
        });
      }
    });
  }

  render () {
    if (!this.state.latestRulesTotal) {
      return null;
    }

    const newRulesUrl = getRulesUrl({
      'available_since': PERIOD_START_MOMENT.format('YYYY-MM-DD')
    });

    return (
        <div className="quality-profile-box quality-profiles-evolution-rules">
          <div className="clearfix">
            <strong className="pull-left">
              {translate('quality_profiles.latest_new_rules')}
            </strong>

            {this.state.latestRulesTotal > RULES_LIMIT && (
                <a className="pull-right small text-muted"
                   href={newRulesUrl}>
                  {translate('see_all')}
                </a>
            )}
          </div>
          <ul>
            {this.state.latestRules.map(rule => (
                <li key={rule.key} className="spacer-top">
                  <div className="text-ellipsis">
                    <a className="link-no-underline"
                       href={getRulesUrl({ 'rule_key': rule.key })}>
                      {' '}
                      {rule.name}
                    </a>
                    <div className="note">
                      {rule.activations ? (
                          translateWithParameters(
                              'quality_profiles.latest_new_rules.activated',
                              rule.langName,
                              rule.activations
                          )
                      ) : (
                          translateWithParameters(
                              'quality_profiles.latest_new_rules.not_activated',
                              rule.langName
                          )
                      )}
                    </div>
                  </div>
                </li>
            ))}
          </ul>
        </div>
    );
  }
}
