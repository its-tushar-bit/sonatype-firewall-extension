/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { keys, includes, replace } from 'ramda';
import { NxTable, NxOverflowTooltip } from '@sonatype/react-shared-components';
import { NOT_ENABLED, NOT_APPLICABLE } from 'MainRoot/OrgsAndPolicies/retentionSlice';

export const timeShortener = (value) => {
  const mapper = new Map([
    [/years?/, 'y'],
    [/months?/, 'mo'],
    [/weeks?/, 'w'],
  ]);

  for (const [regExp, replacer] of mapper) {
    if (value.match(regExp)) {
      return replace(regExp, replacer, value);
    }
  }

  return value;
};

function getMaxAge(report) {
  if (report.enablePurging) {
    if (report.maxAge?.trimmedValue) {
      const age = `${report.maxAge.trimmedValue} ${report.maxAgeUnit}`;
      return timeShortener(age);
    }
    return NOT_APPLICABLE;
  }
  return NOT_ENABLED;
}

function getMaxReports(report) {
  if (report.enablePurging) {
    if (report.maxCount?.trimmedValue) {
      return report.maxCount.trimmedValue;
    }
    return NOT_APPLICABLE;
  }
  return NOT_ENABLED;
}

export default function RetentionTable({ stages }) {
  const stageNames = keys(stages);

  return (
    <NxTable>
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>Max</NxTable.Cell>
          {stageNames?.map((name) => {
            const value = includes(name, ['continuous-monitoring', 'stage-release']) ? (
              <div className="retention-tile__truncate-wrapper">
                <NxOverflowTooltip>
                  <div className="nx-truncate-ellipsis">{name}</div>
                </NxOverflowTooltip>
              </div>
            ) : (
              name
            );

            return (
              <NxTable.Cell key={name} className={`retention-table__col ${name}`}>
                {value}
              </NxTable.Cell>
            );
          })}
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body>
        <NxTable.Row>
          <NxTable.Cell>
            <b>Age</b>
          </NxTable.Cell>
          {stageNames.map((name) => (
            <NxTable.Cell key={`Age-${name}`} className={`retention-table__col ${name}`}>
              {getMaxAge(stages[name])}
            </NxTable.Cell>
          ))}
        </NxTable.Row>
        <NxTable.Row>
          <NxTable.Cell>
            <b>Reports</b>
          </NxTable.Cell>
          {stageNames.map((name) => (
            <NxTable.Cell key={`Reports-${name}`} className={`retention-table__col ${name}`}>
              {getMaxReports(stages[name])}
            </NxTable.Cell>
          ))}
        </NxTable.Row>
      </NxTable.Body>
    </NxTable>
  );
}

RetentionTable.propTypes = {
  stages: PropTypes.object,
};
