/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useRef } from 'react';
import { propOr } from 'ramda';
import { useSelector, useDispatch } from 'react-redux';
import BulkWaiveTableRow from './BulkWaiveTableRow';
import {
  NxButton,
  NxButtonBar,
  NxCheckbox,
  NxFilterInput,
  NxPageMain,
  NxTile,
  NxH2,
  NxTable,
  NxFontAwesomeIcon,
  NxLoadWrapper,
} from '@sonatype/react-shared-components';
import { faFilter } from '@fortawesome/free-solid-svg-icons';
import { cancelBulkWaive, goToWaiverConfigurationPage } from './waiverActions';
import { actions as waiverActions } from './waiverSlice';
import {
  selectSortConfiguration,
  selectSubstringFilters,
  selectAllComponentsList,
  selectLoadError,
  selectIsLoading,
  selectDisplayedComponentList,
  selectIsAggregated,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  setSorting,
  setSortingParameters,
  setStringFieldFilter,
  setReportParameters,
  loadReportIfNeeded,
  toggleShowFilterPopover,
  toggleAggregateReportEntries,
} from 'MainRoot/applicationReport/applicationReportActions';
import { actions as policyViolationsActions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';
import PolicyViolationDetailsPopover from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationDetailsPopover';
import ReportFilterPopover from 'MainRoot/applicationReport/ReportFilterPopover';
import { selectBulkWaiverCheckboxState } from './bulkWaiverSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import BulkWaiveTitle from './BulkWaiveTitle';
import { selectHasBulkWaivers } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';

const getDirection = (sortConfig, key) => {
  return sortConfig && sortConfig.key === key ? sortConfig.dir : null;
};

export default function BulkWaivePage() {
  const dispatch = useDispatch();
  const hasToggledAggregation = useRef(false);
  const rawEntries = useSelector(selectAllComponentsList);
  const substringFilters = useSelector(selectSubstringFilters);
  const loadError = useSelector(selectLoadError);
  const isLoading = useSelector(selectIsLoading);
  const isAggregated = useSelector(selectIsAggregated);
  const getSubstringFiltersProp = (propName) => propOr('', propName, substringFilters);
  const policyNameFilter = getSubstringFiltersProp('policyName');
  const derivedComponentNameFilter = getSubstringFiltersProp('derivedComponentName');
  const constraintFilter = getSubstringFiltersProp('constraintName');
  const sortConfiguration = useSelector(selectSortConfiguration);
  const routerParams = useSelector(selectRouterCurrentParams);
  const hasBulkWaivers = useSelector(selectHasBulkWaivers);
  const isCdpBulkWaive = !!routerParams.hash;
  const constraintOrComponentName = isCdpBulkWaive ? 'constraintName' : 'derivedComponentName';

  const policyThreatLevelSettings = {
    key: 'policyThreatLevel',
    sortingOrder: ['policyThreatLevel', 'policyName', constraintOrComponentName],
  };

  const policyNameSettings = {
    key: 'policyName',
    sortingOrder: ['policyName', '-policyThreatLevel', constraintOrComponentName],
  };

  const componentNameSettings = {
    key: 'derivedComponentName',
    sortingOrder: ['derivedComponentName', '-policyThreatLevel', 'policyName'],
  };

  const constraintNameSettings = {
    key: 'constraintName',
    sortingOrder: ['constraintName', '-policyThreatLevel', 'policyName'],
  };

  const retryHandler = () => {
    dispatch(loadReportIfNeeded());
  };

  const originalDisplayedEntries = useSelector(selectDisplayedComponentList);

  // At the CDP level, we only want to display the entries that match the hash
  // Also filter to only show open violations
  const displayedEntries = originalDisplayedEntries?.filter((entry) => {
    const cdpFilter = isCdpBulkWaive ? entry.hash === routerParams.hash : true;
    const openViolationFilter = entry.derivedViolationState === 'open';
    return cdpFilter && openViolationFilter;
  });

  const getViolationCondition = (policyViolationId) => {
    if (!rawEntries || !policyViolationId) {
      return 'No condition specified';
    }
    const violation = rawEntries.find((entry) => entry.policyViolationId === policyViolationId);
    if (!violation?.constraints) {
      return 'No condition specified';
    }
    // Extract reasons from constraints like PolicyViolationsTableRow does
    const reasons = violation.constraints
      .flatMap((constraint) =>
        constraint.conditions ? constraint.conditions.map((condition) => condition.conditionReason) : []
      )
      .filter(Boolean);
    return reasons.length > 0 ? reasons.join(', ') : 'No condition specified';
  };

  const checkboxState = useSelector(selectBulkWaiverCheckboxState);
  const selectAllChecked =
    displayedEntries?.length > 0 && displayedEntries.every((entry) => checkboxState[entry.policyViolationId] === true);

  // Override applicationReportRoot's filter reset (same pattern as ReportPage)
  useEffect(() => {
    const { publicId, scanId, unknownjs, embeddable, policyViolationId } = routerParams;
    if (publicId && scanId) {
      hasToggledAggregation.current = false;
      dispatch(setReportParameters(publicId, scanId, unknownjs, embeddable, policyViolationId));
      dispatch(loadReportIfNeeded());
    }
  }, [routerParams.publicId, routerParams.scanId, dispatch]);

  // Ensure we always show non-aggregated data on this page
  useEffect(() => {
    if (isAggregated && !hasToggledAggregation.current) {
      dispatch(toggleAggregateReportEntries());
      hasToggledAggregation.current = true;
    }
  }, [isAggregated, dispatch]);

  const setSortingParams = (key, sortingOrder, direction) =>
    dispatch(setSortingParameters(key, sortingOrder, direction));
  const setSortingOrder = (order, entries) => dispatch(setSorting(order, entries));

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

  const createRows = () => {
    return displayedEntries?.map((component, index) => {
      const { policyViolationId } = component;
      const onRowClick = () => {
        dispatch(policyViolationsActions.setSelectedPolicyViolationId(policyViolationId));
        dispatch(policyViolationsActions.toggleShowViolationsDetailPopover());
      };
      const onRowCheckboxClick = (event) => {
        event.stopPropagation();
        event.preventDefault();
        dispatch(waiverActions.toggleBulkWaiveCheckbox(policyViolationId));
      };
      const condition = getViolationCondition(component.policyViolationId);
      return (
        <BulkWaiveTableRow
          key={`${index}-${policyViolationId}`}
          component={component}
          condition={condition}
          onClick={onRowClick}
          onCheckboxClick={onRowCheckboxClick}
          isChecked={!!checkboxState[policyViolationId]}
          isCdpBulkWaive={isCdpBulkWaive}
        />
      );
    });
  };

  const setFieldFilter = (colName, filter) => dispatch(setStringFieldFilter(colName, filter));

  const filterByPolicyName = (filter) => {
    setFieldFilter('policyName', filter);
  };

  const filterByDerivedComponentName = (filter) => {
    setFieldFilter('derivedComponentName', filter);
  };

  const filterByConstraint = (filter) => {
    setFieldFilter('constraintName', filter);
  };

  const toggleShowFilter = () => dispatch(toggleShowFilterPopover());

  const dirPolicyName = getDirection(sortConfiguration, 'policyName');
  const dirPolicyThreatLevel = getDirection(sortConfiguration, 'policyThreatLevel');
  const dirComponentName = getDirection(sortConfiguration, 'derivedComponentName');
  const dirConstraintName = getDirection(sortConfiguration, 'constraintName');

  // Helper bindings for conditional values
  const constraintOrComponentFilter = isCdpBulkWaive ? constraintFilter : derivedComponentNameFilter;
  const constraintOrComponentFilterFn = isCdpBulkWaive ? filterByConstraint : filterByDerivedComponentName;
  const constraintOrComponentSortDir = isCdpBulkWaive ? dirConstraintName : dirComponentName;
  const constraintOrComponentSettings = isCdpBulkWaive ? constraintNameSettings : componentNameSettings;
  const constraintOrComponentCellClass = isCdpBulkWaive
    ? 'iq-bulk-waive__constraint-name-cell'
    : 'iq-bulk-waive__component-name-cell';
  const constraintOrComponentFilterId = isCdpBulkWaive ? 'report-constraint-filter' : 'report-component-name-filter';
  const constraintOrComponentPlaceholder = isCdpBulkWaive ? 'constraint name' : 'component name';
  const constraintOrComponentHeaderText = isCdpBulkWaive ? 'Constraint' : 'Component';

  const numberOfSelections = () => {
    return Object.values(checkboxState).filter((value) => value === true).length;
  };

  const toggleSelectAll = (event) => {
    event.stopPropagation();
    event.preventDefault();

    if (!displayedEntries) return;

    const displayedIds = displayedEntries.map((entry) => entry.policyViolationId);
    const allDisplayedSelected = displayedIds.every((id) => checkboxState[id] === true);

    // Toggle all displayed entries: if all are selected, unselect them; otherwise select all
    dispatch(
      waiverActions.toggleSelectAllCheckbox({
        ids: displayedIds,
        shouldSelect: !allDisplayedSelected,
      })
    );
  };

  const cancelClick = () => {
    dispatch(cancelBulkWaive());
  };

  const nextClick = () => {
    const selectedViolations = rawEntries.filter((entry) => checkboxState[entry.policyViolationId]);
    dispatch(waiverActions.setSelectedViolations(selectedViolations));
    dispatch(goToWaiverConfigurationPage());
  };

  const hiddenCount = Object.entries(checkboxState).filter(
    ([id, checked]) =>
      checked === true && (!displayedEntries || !displayedEntries.some((entry) => entry.policyViolationId === id))
  ).length;
  const hiddenCountMessage = hiddenCount > 0 ? `(${hiddenCount} hidden)` : '';
  const selectedCountMessage = `${numberOfSelections()} ${
    numberOfSelections() === 1 ? 'violation' : 'violations'
  } selected`;

  return (
    <>
      <PolicyViolationDetailsPopover />
      <ReportFilterPopover />
      <NxPageMain id="bulk-waive-page-container" className="nx-viewport-sized__container">
        <NxLoadWrapper error={loadError} loading={isLoading} retryHandler={retryHandler}>
          <BulkWaiveTitle />
          <NxTile
            className={`nx-viewport-sized__container${!hasBulkWaivers ? ' iq-banner-flush-top' : ''}`}
          >
            {!hasBulkWaivers && (
              <EnterpriseFullWidthBanner
                description="Efficiently manage multiple policy violations at once by creating waivers in bulk to save time and reduce repetitive work."
              />
            )}
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>Choose violations to Waive</NxH2>
              </NxTile.HeaderTitle>
              <NxTile.HeaderActions>
                <NxButton onClick={toggleShowFilter} variant="tertiary" id="filters-toggle-button">
                  <NxFontAwesomeIcon icon={faFilter} />
                  <span>Filter</span>
                </NxButton>
              </NxTile.HeaderActions>
            </NxTile.Header>
            <NxTile.Content className="nx-viewport-sized__container">
              <div className="nx-table-container nx-scrollable nx-viewport-sized__scrollable">
                <NxTable id="bulk-waive-table" className="nx-table--fixed-layout">
                  <NxTable.Head>
                    <NxTable.Row>
                      <NxTable.Cell className="iq-bulk-waive__select-all-cell">
                        <NxCheckbox isChecked={selectAllChecked} onClick={toggleSelectAll} />
                      </NxTable.Cell>
                      <NxTable.Cell
                        className="iq-bulk-waive__threat-cell"
                        isSortable
                        sortDir={dirPolicyThreatLevel}
                        onClick={() => requestSort(policyThreatLevelSettings)}
                      >
                        Threat
                      </NxTable.Cell>
                      <NxTable.Cell
                        className="iq-bulk-waive__policy-name-cell"
                        isSortable
                        sortDir={dirPolicyName}
                        onClick={() => requestSort(policyNameSettings)}
                      >
                        Policy
                      </NxTable.Cell>
                      <NxTable.Cell
                        className={constraintOrComponentCellClass}
                        isSortable
                        sortDir={constraintOrComponentSortDir}
                        onClick={() => requestSort(constraintOrComponentSettings)}
                      >
                        {constraintOrComponentHeaderText}
                      </NxTable.Cell>

                      <NxTable.Cell className="iq-bulk-waive__condition-name-cell">Condition</NxTable.Cell>
                      <NxTable.Cell chevron />
                    </NxTable.Row>
                  </NxTable.Head>
                  <NxTable.Body
                    emptyMessage="No Results"
                    error={loadError}
                    isLoading={isLoading}
                    retryHandler={retryHandler}
                  >
                    <NxTable.Row className="nx-table-row--filter-header">
                      <NxTable.Cell colSpan={3}>
                        <div className="iq-bulk-waive__filter-row">
                          <NxFilterInput
                            id="report-policy-name-filter"
                            placeholder="policy name"
                            onChange={filterByPolicyName}
                            value={policyNameFilter}
                          />
                        </div>
                      </NxTable.Cell>
                      <NxTable.Cell colSpan={3}>
                        <NxFilterInput
                          id={constraintOrComponentFilterId}
                          placeholder={constraintOrComponentPlaceholder}
                          onChange={constraintOrComponentFilterFn}
                          value={constraintOrComponentFilter}
                        />
                      </NxTable.Cell>
                    </NxTable.Row>
                    {isNilOrEmpty(displayedEntries) ? (
                      <NxTable.Row className="iq-bulk-waive__no-results-row">
                        <NxTable.Cell colSpan={6}>No Results</NxTable.Cell>
                      </NxTable.Row>
                    ) : (
                      createRows()
                    )}
                  </NxTable.Body>
                </NxTable>
                <div className="nx-table-container__footer">
                  <NxButtonBar>
                    <div className="iq-bulk-waive__selected-count">
                      {selectedCountMessage} {hiddenCountMessage}
                    </div>
                    <NxButton id="bulk-waive-selection-cancel-button" variant="tertiary" onClick={cancelClick}>
                      Cancel
                    </NxButton>
                    <NxButton
                      id="bulk-waive-selection-next-button"
                      variant="primary"
                      disabled={numberOfSelections() === 0}
                      onClick={nextClick}
                    >
                      Next
                    </NxButton>
                  </NxButtonBar>
                </div>
              </div>
            </NxTile.Content>
          </NxTile>
        </NxLoadWrapper>
      </NxPageMain>
    </>
  );
}
