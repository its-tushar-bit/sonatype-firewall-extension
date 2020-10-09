/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxTable,
  NxTableHead,
  NxTableRow,
  NxTableCell,
  NxTableBody
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import ReportTableRow from './ReportTableRow';

const policyThreatLevelSettings = {
  key: 'policyThreatLevel',
  sortingOrder: ['policyThreatLevel', 'policyName', 'derivedComponentName']
};

const policyNameSettings = {
  key: 'policyName',
  sortingOrder: ['policyName', '-policyThreatLevel', 'derivedComponentName']
};

const componentNameSettings = {
  key: 'derivedComponentName',
  sortingOrder: ['derivedComponentName', '-policyThreatLevel', 'policyName']
};

const getDirection = (sortConfig, key) => {
  return sortConfig && sortConfig.key === key ? sortConfig.dir : null;
};

export default function ReportContent(props) {

  const { selectedReport, sortConfiguration, setSortingParameters, setSorting } = props;
  const displayedEntries = selectedReport ? selectedReport.displayedEntries : [];

  function requestSort(settings) {
    let direction = 'asc';
    if (
      sortConfiguration &&
        sortConfiguration.key === settings.key &&
        sortConfiguration.dir === 'asc'
    ) {
      direction = 'desc';
    }
    const sortingOrder = settings.sortingOrder;
    if (direction === 'desc' && !sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = '-'.concat(sortingOrder[0]);
    }
    if (direction === 'asc' && sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = sortingOrder[0].substring(1);
    }
    setSortingParameters(settings.key, sortingOrder, direction);
    setSorting(sortingOrder, displayedEntries);
  }

  const dirPolicyThreatLevel = getDirection(sortConfiguration, 'policyThreatLevel');
  const dirPolicyName = getDirection(sortConfiguration, 'policyName');
  const dirComponentName = getDirection(sortConfiguration, 'derivedComponentName');

  return (
    <div className="nx-tile-content nx-scrollable nx-scrollable--report-table">
      <NxTable className="nx-table--scrollable">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell isSortable sortDir={dirPolicyThreatLevel} onClick={() =>
              requestSort(policyThreatLevelSettings)}>
              Threat
            </NxTableCell>
            <NxTableCell isSortable sortDir={dirPolicyName} onClick={() => requestSort(policyNameSettings)}>
              Policy
            </NxTableCell>
            <NxTableCell isSortable sortDir={dirComponentName} onClick={() => requestSort(componentNameSettings)}>
              Component
            </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody>
          {displayedEntries.length > 0 && displayedEntries.map(component => createRow(component))}
          {displayedEntries.length === 0 &&
            <NxTableRow>
              <NxTableCell colSpan={3} className="nx-cell--empty"><span>No Results</span></NxTableCell>
            </NxTableRow>
          }
        </NxTableBody>
      </NxTable>
    </div>
  );
}

const createRow = (component) => {
  return <ReportTableRow key={ component.hash } component={ component }/>;
};

ReportContent.propTypes = {
  selectedReport: PropTypes.shape({
    displayedEntries: PropTypes.arrayOf(PropTypes.shape({
      derivedComponentName: PropTypes.string,
      policyName: PropTypes.string,
      hash: PropTypes.string,
      derivedDependencyType: PropTypes.string,
      waived: PropTypes.bool,
      filenames: PropTypes.array,
      grandfathered: PropTypes.bool,
      policyThreatLevel: PropTypes.number
    }))
  }),
  sortConfiguration: PropTypes.shape({
    sortFields: PropTypes.arrayOf(PropTypes.string),
    dir: PropTypes.string
  }),
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func
};
