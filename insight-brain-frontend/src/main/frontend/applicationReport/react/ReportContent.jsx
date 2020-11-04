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
  NxTableBody,
  NxFilterInput
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import ReportTableRow from './ReportTableRow';
import { propOr } from 'ramda';

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

  const {
    selectedReport,
    substringFilters,
    sortConfiguration,
    setSortingParameters,
    setSorting,
    setStringFieldFilter
  } = props;
  const displayedEntries = selectedReport ? selectedReport.displayedEntries : [];
  const getSubstringFiltersProp = propName => propOr('', propName, substringFilters);
  const policyNameFilter = getSubstringFiltersProp('policyName');
  const derivedComponentNameFilter = getSubstringFiltersProp('derivedComponentName');

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

  const filterPolicyName = (filter) => {
    setStringFieldFilter('policyName', filter);
  };

  const filterDerivedComponentName = (filter) => {
    setStringFieldFilter('derivedComponentName', filter);
  };

  const dirPolicyThreatLevel = getDirection(sortConfiguration, 'policyThreatLevel');
  const dirPolicyName = getDirection(sortConfiguration, 'policyName');
  const dirComponentName = getDirection(sortConfiguration, 'derivedComponentName');

  return (
    <section className="nx-tile iq-app-report__results-table-tile">
      <div className="nx-tile-content nx-scrollable nx-scrollable--table-container">
        <NxTable className="nx-table--scrollable nx-table--fixed-layout">
          <NxTableHead>
            <NxTableRow>
              <NxTableCell className="iq-app-report__threat-cell"
                           isSortable
                           sortDir={dirPolicyThreatLevel}
                           onClick={() => requestSort(policyThreatLevelSettings)}>
                Threat
              </NxTableCell>
              <NxTableCell className="iq-app-report__policy-name-cell"
                           isSortable
                           sortDir={dirPolicyName}
                           onClick={() => requestSort(policyNameSettings)}>
                Policy
              </NxTableCell>
              <NxTableCell className="iq-app-report__component-name-cell"
                           isSortable
                           sortDir={dirComponentName}
                           onClick={() => requestSort(componentNameSettings)}>
                Component
              </NxTableCell>
            </NxTableRow>
            <NxTableRow className="nx-table-row--filter-header">
              <NxTableCell colSpan={2}>
                <NxFilterInput placeholder="policy name" onChange={filterPolicyName} value={policyNameFilter} />
              </NxTableCell>
              <NxTableCell>
                <NxFilterInput placeholder="component name"
                               onChange={filterDerivedComponentName}
                               value={derivedComponentNameFilter} />
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody emptyMessage="No Results">
            {displayedEntries.map(createRow)}
          </NxTableBody>
        </NxTable>
      </div>
    </section>
  );
}

const createRow = (component, index) => {
  return <ReportTableRow key={ index } index={ index } component={ component }/>;
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
    dir: PropTypes.string,
    key: PropTypes.string
  }),
  substringFilters: PropTypes.shape({
    policyName: PropTypes.string,
    derivedComponentName: PropTypes.string
  }),
  // actions
  setSorting: PropTypes.func,
  setSortingParameters: PropTypes.func,
  setStringFieldFilter: PropTypes.func
};
