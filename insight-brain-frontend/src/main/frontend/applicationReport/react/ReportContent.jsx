/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { propOr } from 'ramda';
import {
  NxTable,
  NxTableHead,
  NxTableRow,
  NxTableCell,
  NxTableBody,
  NxFilterInput,
  NxButton,
  NxFontAwesomeIcon,
  NxToggle,
} from '@sonatype/react-shared-components';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';
import ReportTableRow from './ReportTableRow';
import {
  setSorting,
  setSortingParameters,
  setStringFieldFilter,
  goToComponentDetailsPage,
  selectComponent,
  toggleAggregateReportEntries,
  toggleShowFilterPopover,
  goToDependencyTreePage,
} from 'MainRoot/applicationReport/applicationReportActions';
import {
  selectIsAggregated,
  selectDisplayedComponentList,
  selectSubstringFilters,
  selectSortConfiguration,
  selectDependencyTreeIsAvailable,
  selectDependencyTreeUnavailableMessage,
} from 'MainRoot/applicationReport/applicationReportSelectors';

const policyThreatLevelSettings = {
  key: 'policyThreatLevel',
  sortingOrder: ['policyThreatLevel', 'policyName', 'derivedComponentName'],
};

const policyNameSettings = {
  key: 'policyName',
  sortingOrder: ['policyName', '-policyThreatLevel', 'derivedComponentName'],
};

const componentNameSettings = {
  key: 'derivedComponentName',
  sortingOrder: ['derivedComponentName', '-policyThreatLevel', 'policyName'],
};

const getDirection = (sortConfig, key) => {
  return sortConfig && sortConfig.key === key ? sortConfig.dir : null;
};

export default function ReportContent() {
  const dispatch = useDispatch();
  const toggleAggregateByComponent = () => dispatch(toggleAggregateReportEntries());
  const setSortingParams = (key, sortingOrder, direction) =>
    dispatch(setSortingParameters(key, sortingOrder, direction));
  const setSortingOrder = (order, entries) => dispatch(setSorting(order, entries));
  const setFieldFilter = (colName, filter) => dispatch(setStringFieldFilter(colName, filter));
  const goToCDPPage = (hash) => dispatch(goToComponentDetailsPage(hash));
  const setSelectedComponent = (idx) => dispatch(selectComponent(idx));
  const toggleShowFilter = () => dispatch(toggleShowFilterPopover());
  const goToDependencyTree = () => dispatch(goToDependencyTreePage());
  const isAggregated = useSelector(selectIsAggregated);
  const displayedEntries = useSelector(selectDisplayedComponentList);
  const substringFilters = useSelector(selectSubstringFilters);
  const sortConfiguration = useSelector(selectSortConfiguration);
  const dependencyTreeIsAvailable = useSelector(selectDependencyTreeIsAvailable);
  const dependencyTreeUnavailableMessage = useSelector(selectDependencyTreeUnavailableMessage);

  const getSubstringFiltersProp = (propName) => propOr('', propName, substringFilters);
  const policyNameFilter = getSubstringFiltersProp('policyName');
  const derivedComponentNameFilter = getSubstringFiltersProp('derivedComponentName');

  function requestSort(settings) {
    let direction = 'asc';
    if (sortConfiguration && sortConfiguration.key === settings.key && sortConfiguration.dir === 'asc') {
      direction = 'desc';
    }
    const sortingOrder = settings.sortingOrder;
    if (direction === 'desc' && !sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = '-'.concat(sortingOrder[0]);
    }
    if (direction === 'asc' && sortingOrder[0].startsWith('-')) {
      sortingOrder[0] = sortingOrder[0].substring(1);
    }
    setSortingParams(settings.key, sortingOrder, direction);
    setSortingOrder(sortingOrder, displayedEntries || []);
  }

  const filterPolicyName = (filter) => {
    setFieldFilter('policyName', filter);
  };

  const filterDerivedComponentName = (filter) => {
    setFieldFilter('derivedComponentName', filter);
  };

  const dirPolicyThreatLevel = getDirection(sortConfiguration, 'policyThreatLevel');
  const dirPolicyName = getDirection(sortConfiguration, 'policyName');
  const dirComponentName = getDirection(sortConfiguration, 'derivedComponentName');

  const createRows = () => {
    return displayedEntries.map((component, index) => {
      const { policyViolationId, hash } = component;

      const onRowClick = () => {
        setSelectedComponent(index);
        goToCDPPage(hash);
      };

      return <ReportTableRow key={policyViolationId || hash} component={component} onClick={onRowClick} />;
    });
  };

  const redirectToDependencyTree = () => {
    if (dependencyTreeIsAvailable) goToDependencyTree();
  };

  return (
    <section className="nx-tile iq-app-report__results-table-tile nx-viewport-sized__container">
      <div className="nx-tile-header">
        <div className="nx-tile-header__title">
          <NxToggle isChecked={isAggregated} onChange={toggleAggregateByComponent}>
            Aggregate by component
          </NxToggle>
        </div>
        <div className="nx-tile__actions">
          <NxButton
            onClick={redirectToDependencyTree}
            variant="tertiary"
            id="dependency-tree-button"
            className={dependencyTreeIsAvailable ? '' : 'disabled'}
            title={dependencyTreeUnavailableMessage}
          >
            View Dependency Tree
          </NxButton>
          <NxButton onClick={toggleShowFilter} variant="tertiary" id="filters-toggle-button">
            <NxFontAwesomeIcon icon={faFilter} />
            Filter
          </NxButton>
        </div>
      </div>
      <div className="nx-tile-content nx-viewport-sized__container">
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <NxTable className="nx-table--scrollable nx-table--fixed-layout">
            <NxTableHead>
              <NxTableRow>
                <NxTableCell
                  className="iq-app-report__threat-cell"
                  isSortable
                  sortDir={dirPolicyThreatLevel}
                  onClick={() => requestSort(policyThreatLevelSettings)}
                >
                  Threat
                </NxTableCell>
                <NxTableCell
                  className="iq-app-report__policy-name-cell"
                  isSortable
                  sortDir={dirPolicyName}
                  onClick={() => requestSort(policyNameSettings)}
                >
                  Policy
                </NxTableCell>
                <NxTableCell
                  className="iq-app-report__component-name-cell"
                  isSortable
                  sortDir={dirComponentName}
                  onClick={() => requestSort(componentNameSettings)}
                >
                  Component
                </NxTableCell>
                <NxTableCell chevron />
              </NxTableRow>
              <NxTableRow className="nx-table-row--filter-header">
                <NxTableCell colSpan={2}>
                  <NxFilterInput placeholder="policy name" onChange={filterPolicyName} value={policyNameFilter} />
                </NxTableCell>
                <NxTableCell>
                  <NxFilterInput
                    placeholder="component name"
                    onChange={filterDerivedComponentName}
                    value={derivedComponentNameFilter}
                  />
                </NxTableCell>
                <NxTableCell chevron />
              </NxTableRow>
            </NxTableHead>
            <NxTableBody emptyMessage="No Results">{createRows()}</NxTableBody>
          </NxTable>
        </div>
      </div>
    </section>
  );
}
