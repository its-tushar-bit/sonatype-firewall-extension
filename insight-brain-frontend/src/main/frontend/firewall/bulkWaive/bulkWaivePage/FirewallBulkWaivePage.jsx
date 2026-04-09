/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxCheckbox,
  NxFilterInput,
  NxIndeterminatePagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableContainer,
  NxTableHead,
  NxTableRow,
  NxTile,
  NxH2,
  NxPageMain,
  NxLoadWrapper,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faFilter } from '@fortawesome/free-solid-svg-icons';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import {
  selectRepositoryComponents,
  selectLoadingRepositoryComponents,
  selectErrorComponentsTable,
  selectSearchFiltersValues,
  selectComponentsRequestBody,
  selectTotalComponentCount,
  selectFilteredTotalCount,
  selectCurrentPage,
  selectHasMoreResults,
  selectAggregate,
} from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import { actions as firewallBulkWaiverActions } from '../firewallBulkWaiverSlice';
import {
  selectFirewallBulkWaiverSelectedViolations,
  selectFirewallSelectedCount,
  selectFirewallSelectAllMode,
  selectFirewallCheckboxState,
  selectBulkWaiveSource,
  selectBulkWaiveComponentPathname,
  selectBulkWaiveComponentDisplayName,
  selectOriginalAggregateState,
  selectSourceContext,
} from '../firewallBulkWaiverSelectors';
import { loadComponentPolicyViolations } from 'MainRoot/firewall/firewallActions';
import {
  selectFirewallPolicyViolations,
  selectFirewallComponentDetailsPage,
} from 'MainRoot/firewall/firewallSelectors';
import BulkWaiveTableRow from './bulkWaiveTableRow/BulkWaiveTableRow';
import BulkWaiveTitle from '../bulkWaiveTitle/BulkWaiveTitle';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';

export default function FirewallBulkWaivePage() {
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const { repositoryId } = routerParams;
  const [checkboxState, setCheckboxState] = useState({});
  const [selectAllMode, setSelectAllMode] = useState(false);
  const [selectedViolationId, setSelectedViolationId] = useState(null);
  const [isCancelling, setIsCancelling] = useState(false);

  const source = useSelector(selectBulkWaiveSource);
  const componentPathname = useSelector(selectBulkWaiveComponentPathname);
  const componentDisplayName = useSelector(selectBulkWaiveComponentDisplayName);
  const originalAggregateState = useSelector(selectOriginalAggregateState);
  const sourceContext = useSelector(selectSourceContext);

  const currentAggregate = useSelector(selectAggregate);

  const repositoryViolations = useSelector(selectRepositoryComponents);
  const componentViolations = useSelector(selectFirewallPolicyViolations);
  const componentDetailsPage = useSelector(selectFirewallComponentDetailsPage);

  const violations = source === 'component-details' ? componentViolations : repositoryViolations;

  const repositoryLoading = useSelector(selectLoadingRepositoryComponents);
  const repositoryError = useSelector(selectErrorComponentsTable);
  const componentLoading = componentDetailsPage?.isLoadingPolicyViolations || false;
  const componentError = componentDetailsPage?.policyViolationsError || null;

  const loading = source === 'component-details' ? componentLoading : repositoryLoading;
  const error = source === 'component-details' ? componentError : repositoryError;

  const currentPage = useSelector(selectCurrentPage);
  const hasMoreResults = useSelector(selectHasMoreResults);

  const totalComponentCount = useSelector(selectTotalComponentCount);
  const filteredTotalCount = useSelector(selectFilteredTotalCount);
  const searchFiltersValues = useSelector(selectSearchFiltersValues);
  const componentsRequestBody = useSelector(selectComponentsRequestBody);
  const storedSelectedViolations = useSelector(selectFirewallBulkWaiverSelectedViolations);
  const storedSelectedCount = useSelector(selectFirewallSelectedCount);
  const storedSelectAllMode = useSelector(selectFirewallSelectAllMode);
  const storedCheckboxState = useSelector(selectFirewallCheckboxState);

  useEffect(() => {
    if (isCancelling) {
      return;
    }

    dispatch(firewallBulkWaiverActions.setAllFilteredViolations([]));

    if (repositoryId) {
      if (source === 'component-details') {
        if (componentPathname) {
          dispatch(loadComponentPolicyViolations(componentPathname, repositoryId));
        }
      } else {
        dispatch(firewallBulkWaiverActions.setOriginalAggregateState(currentAggregate));

        if (currentAggregate) {
          dispatch(actions.toggleAggregate());
        }

        dispatch(actions.setPageSize(12));
        dispatch(actions.getRepositoryInformation(repositoryId));
        dispatch(actions.getRepositorySummary(repositoryId));
        dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
      }
    }

    return () => {
      if (repositoryId && source !== 'component-details') {
        dispatch(actions.setPageSize(12));
      }
      if (policyFilterTimeoutRef.current) {
        clearTimeout(policyFilterTimeoutRef.current);
      }
      if (componentFilterTimeoutRef.current) {
        clearTimeout(componentFilterTimeoutRef.current);
      }
    };
  }, [dispatch, repositoryId, source, componentPathname, isCancelling]);

  useEffect(() => {
    if (storedSelectedCount > 0) {
      setSelectAllMode(storedSelectAllMode);
      setCheckboxState(storedCheckboxState || {});
    }
  }, [storedSelectedCount, storedSelectAllMode, storedCheckboxState]);

  useEffect(() => {
    if (selectAllMode || Object.keys(checkboxState).length > 0) {
      setSelectAllMode(false);
      setCheckboxState({});
    }
  }, [
    componentsRequestBody?.searchFilters,
    componentsRequestBody?.matchStateFilters,
    componentsRequestBody?.violationStateFilters,
    componentsRequestBody?.threatLevelFilters,
  ]); // Don't include selectAllMode or checkboxState in deps to avoid infinite loop

  const getViolationCondition = (component) => {
    if (!component) {
      return 'No condition specified';
    }
    if (!component.constraints) {
      return 'No condition specified';
    }
    const reasons = component.constraints
      .flatMap((constraint) =>
        constraint.conditions ? constraint.conditions.map((condition) => condition.conditionReason) : []
      )
      .filter(Boolean);
    return reasons.length > 0 ? reasons.join(', ') : 'No condition specified';
  };

  const handleSort = (sortableField) => {
    if (source !== 'component-details') {
      dispatch(actions.setSorting(sortableField));
      dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
    }
  };

  const getSortDir = (fieldName) => {
    const sortField = componentsRequestBody?.sortFields?.[0];
    if (sortField?.sortableField === fieldName) {
      return sortField.asc ? 'asc' : 'desc';
    }
    return null;
  };

  const handleNextPage = () => {
    if (hasMoreResults) {
      saveCurrentPageSelections();
      dispatch(actions.increasePage());
      dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
    }
  };

  const handlePreviousPage = () => {
    if (currentPage > 1) {
      saveCurrentPageSelections();
      dispatch(actions.decreasePage());
      dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
    }
  };

  const saveCurrentPageSelections = () => {
    const currentPageSelections =
      violations?.filter((v) => {
        if (selectAllMode) {
          return checkboxState[v.policyViolationId] !== false;
        } else {
          return checkboxState[v.policyViolationId] === true;
        }
      }) || [];

    const normalizedSelections = currentPageSelections.map((violation) => ({
      ...violation,
      threatLevel: violation.threatLevel ?? violation.policyThreatLevel,
    }));

    const existingIds = new Set(storedSelectedViolations.map((v) => v.policyViolationId));
    const newSelections = normalizedSelections.filter((v) => !existingIds.has(v.policyViolationId));
    const allSelectedViolations = [...storedSelectedViolations, ...newSelections];

    const finalSelections = allSelectedViolations.filter((v) => {
      const isInCurrentPage = violations.some((cv) => cv.policyViolationId === v.policyViolationId);

      if (selectAllMode) {
        // In selectAllMode, violations are selected unless explicitly unchecked (false)
        return checkboxState[v.policyViolationId] !== false;
      } else {
        // In non-selectAll mode, keep only violations that are explicitly selected in checkboxState
        // or violations from other pages (not in current page)
        if (isInCurrentPage) {
          return checkboxState[v.policyViolationId] === true;
        }
        return true;
      }
    });

    dispatch(firewallBulkWaiverActions.setSelectedViolations(finalSelections));
    dispatch(firewallBulkWaiverActions.setCheckboxState(checkboxState));
  };

  const policyFilterTimeoutRef = useRef(null);
  const componentFilterTimeoutRef = useRef(null);

  const handlePolicyFilterChange = useCallback(
    (value) => {
      dispatch(actions.setFilter({ filterName: 'POLICY_NAME', filterValue: value }));

      if (policyFilterTimeoutRef.current) {
        clearTimeout(policyFilterTimeoutRef.current);
      }

      policyFilterTimeoutRef.current = setTimeout(() => {
        dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
      }, 500);
    },
    [dispatch, repositoryId]
  );

  const handleComponentFilterChange = useCallback(
    (value) => {
      dispatch(actions.setFilter({ filterName: 'COMPONENT_COORDINATES', filterValue: value }));

      if (componentFilterTimeoutRef.current) {
        clearTimeout(componentFilterTimeoutRef.current);
      }

      componentFilterTimeoutRef.current = setTimeout(() => {
        dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
      }, 500);
    },
    [dispatch, repositoryId]
  );

  const openFilterPopover = () => {
    dispatch(actions.setShowFilterPopover(true));
  };

  const handleSelectAll = (event) => {
    event?.stopPropagation();
    event?.preventDefault();

    if (selectAllMode) {
      setSelectAllMode(false);
      setCheckboxState({});
      dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
      dispatch(firewallBulkWaiverActions.setSelectedCount(0));
      dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
      dispatch(firewallBulkWaiverActions.setCheckboxState({}));
    } else {
      setSelectAllMode(true);
      setCheckboxState({});
    }
  };

  const handleCancel = () => {
    setIsCancelling(true);

    setCheckboxState({});
    setSelectAllMode(false);
    dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
    dispatch(firewallBulkWaiverActions.setSelectedCount(0));
    dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
    dispatch(firewallBulkWaiverActions.setCheckboxState({}));
    dispatch(firewallBulkWaiverActions.setAllFilteredViolations([]));
    dispatch(firewallBulkWaiverActions.clearSourceContext());

    if (source === 'component-details' && sourceContext?.repositoryId) {
      dispatch(firewallBulkWaiverActions.clearOriginalAggregateState());
      dispatch(
        stateGo('firewall.componentDetailsPage.violations', {
          repositoryId: sourceContext.repositoryId,
          componentIdentifier: sourceContext.componentIdentifier,
          componentHash: sourceContext.componentHash,
          matchState: sourceContext.matchState,
          pathname: sourceContext.pathname,
        })
      );
    } else {
      if (originalAggregateState !== null && currentAggregate !== originalAggregateState) {
        dispatch(actions.toggleAggregate());
        dispatch(actions.clearFilters());
      }

      dispatch(firewallBulkWaiverActions.clearOriginalAggregateState());
      dispatch(stateGo('firewall.repository-report', { repositoryId }));
    }
  };

  const handleNext = () => {
    const normalizeViolation = (violation) => ({
      ...violation,
      threatLevel: violation.threatLevel ?? violation.policyThreatLevel,
    });

    let currentPageSelections;
    if (selectAllMode) {
      currentPageSelections = violations
        .filter((v) => checkboxState[v.policyViolationId] !== false)
        .map(normalizeViolation);
    } else {
      currentPageSelections = violations
        .filter((v) => checkboxState[v.policyViolationId] === true)
        .map(normalizeViolation);
    }

    const shouldStoreActualViolations = source === 'component-details' || !selectAllMode;

    let allSelectedViolations;
    if (shouldStoreActualViolations) {
      const existingIds = new Set(storedSelectedViolations.map((v) => v.policyViolationId));
      const newSelections = currentPageSelections.filter((v) => !existingIds.has(v.policyViolationId));
      const combinedSelections = [...storedSelectedViolations, ...newSelections];

      // Filter out any violations from the current page that are not selected
      const currentPageIds = new Set(violations.map((v) => v.policyViolationId));
      allSelectedViolations = combinedSelections.filter((v) => {
        const isInCurrentPage = currentPageIds.has(v.policyViolationId);
        if (isInCurrentPage) {
          if (selectAllMode) {
            return checkboxState[v.policyViolationId] !== false;
          } else {
            return checkboxState[v.policyViolationId] === true;
          }
        }
        return true;
      });
    } else {
      allSelectedViolations = currentPageSelections.slice(0, 5);
    }

    dispatch(firewallBulkWaiverActions.setSelectedViolations(allSelectedViolations));
    dispatch(firewallBulkWaiverActions.setSelectedCount(selectedCount));
    dispatch(firewallBulkWaiverActions.setSelectAllMode(selectAllMode));
    dispatch(firewallBulkWaiverActions.setCheckboxState(checkboxState));

    dispatch(stateGo('firewall.bulkWaiveConfiguration', { repositoryId }));
  };

  const createRows = () => {
    return violations?.map((component, index) => {
      const policyViolationId = component.policyViolationId;
      const policyName = component.policyName;
      const pathname = component.pathname;
      const threatLevel = component.threatLevel ?? component.policyThreatLevel;
      const onRowClick = () => {
        setSelectedViolationId(policyViolationId);
      };
      const onCheckboxClick = (event) => {
        event.stopPropagation();
        event.preventDefault();

        if (selectAllMode) {
          setCheckboxState((prev) => {
            const newState = { ...prev };
            if (newState[policyViolationId] === false) {
              delete newState[policyViolationId];
            } else {
              newState[policyViolationId] = false;
            }
            return newState;
          });
        } else {
          setCheckboxState((prev) => {
            const newState = { ...prev };
            if (newState[policyViolationId]) {
              delete newState[policyViolationId];
            } else {
              newState[policyViolationId] = true;
            }
            return newState;
          });
        }
      };
      const condition = getViolationCondition(component);
      const isRowChecked = selectAllMode
        ? checkboxState[policyViolationId] !== false
        : checkboxState[policyViolationId] === true;

      const normalizedComponent = {
        ...component,
        threatLevel,
      };

      return (
        <BulkWaiveTableRow
          key={policyViolationId || `row-${index}`}
          component={normalizedComponent}
          condition={condition}
          onClick={onRowClick}
          onCheckboxClick={onCheckboxClick}
          isChecked={isRowChecked}
          isCdpBulkWaive={false}
          checkboxId={`checkbox-${policyViolationId}`}
        />
      );
    });
  };

  const deselectedCount = Object.values(checkboxState).filter((value) => value === false).length;

    const hasActiveFilters =
    componentsRequestBody?.searchFilters?.length > 0 ||
    componentsRequestBody?.matchStateFilters?.length > 0 ||
    componentsRequestBody?.violationStateFilters?.length > 0 ||
    (componentsRequestBody?.threatLevelFilters &&
      (componentsRequestBody.threatLevelFilters[0] !== 0 || componentsRequestBody.threatLevelFilters[1] !== 10));

  const effectiveTotalCount =
    source === 'component-details' || hasActiveFilters
      ? violations?.length || 0 
      : filteredTotalCount != null && filteredTotalCount > 0
      ? filteredTotalCount 
      : totalComponentCount; 

  const selectedCount = selectAllMode
    ? effectiveTotalCount - deselectedCount
    : Object.values(checkboxState).filter((value) => value === true).length;

  const isAllSelected =
    selectAllMode || (violations.length > 0 && violations.every((v) => checkboxState[v.policyViolationId]));

  return (
    <>
      {source !== 'component-details' && (
        <RepositoryResultsComponentsFilter repositoryId={repositoryId} isBulkWaivePage={true} />
      )}
      <NxPageMain id="fw-bulk-waive-page-main" className="nx-viewport-sized__container">
        <div className="fw-bulk-waive-page">
          <BulkWaiveTitle />
          <NxTile className="nx-viewport-sized__container">
            <NxTile.Header>
              <NxTile.HeaderTitle>
                <NxH2>
                  Choose violations to Waive
                  {source === 'component-details' && componentDisplayName && ` for ${componentDisplayName}`}
                </NxH2>
              </NxTile.HeaderTitle>

              {source !== 'component-details' && (
                <NxTile.HeaderActions>
                  <NxButton onClick={openFilterPopover} variant="tertiary" id="fw-bulk-waive-filter-button">
                    <NxFontAwesomeIcon icon={faFilter} />
                    <span>Filter</span>
                  </NxButton>
                </NxTile.HeaderActions>
              )}
            </NxTile.Header>

            <NxTile.Content>
              <NxLoadWrapper
                loading={loading}
                error={error}
                retryHandler={() => {
                  if (source === 'component-details') {
                    dispatch(loadComponentPolicyViolations(componentPathname, repositoryId));
                  } else {
                    dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
                  }
                }}
              >
                <NxTableContainer>
                  <NxTable>
                    <NxTable.Head>
                      <NxTable.Row>
                        <NxTable.Cell className="fw-bulk-waive__toggle-cell">
                          <NxCheckbox
                            checkboxId="select-all"
                            isChecked={isAllSelected}
                            onClick={handleSelectAll}
                            disabled={violations?.length === 0}
                          />
                        </NxTable.Cell>
                        <NxTable.Cell
                          className="fw-bulk-waive__threat-cell"
                          isSortable={source !== 'component-details'}
                          sortDir={source !== 'component-details' ? getSortDir('POLICY_THREAT_LEVEL') : null}
                          onClick={source !== 'component-details' ? () => handleSort('POLICY_THREAT_LEVEL') : undefined}
                        >
                          THREAT
                        </NxTable.Cell>
                        <NxTable.Cell
                          className="fw-bulk-waive__policy-name-cell"
                          isSortable={source !== 'component-details'}
                          sortDir={source !== 'component-details' ? getSortDir('POLICY_NAME') : null}
                          onClick={source !== 'component-details' ? () => handleSort('POLICY_NAME') : undefined}
                        >
                          POLICY
                        </NxTable.Cell>
                        <NxTable.Cell
                          className="fw-bulk-waive__component-name-cell"
                          isSortable={source !== 'component-details'}
                          sortDir={source !== 'component-details' ? getSortDir('COMPONENT_COORDINATES') : null}
                          onClick={
                            source !== 'component-details' ? () => handleSort('COMPONENT_COORDINATES') : undefined
                          }
                        >
                          COMPONENT
                        </NxTable.Cell>
                        <NxTable.Cell className="fw-bulk-waive__condition-name-cell">CONDITION</NxTable.Cell>
                        <NxTable.Cell />
                      </NxTable.Row>
                      {source !== 'component-details' && (
                        <NxTableRow isFilterHeader>
                          <NxTable.Cell colSpan={2} />
                          <NxTable.Cell className="fw-bulk-waive__filter-policy">
                            <NxFilterInput
                              placeholder="policy name"
                              value={searchFiltersValues?.POLICY_NAME || ''}
                              onChange={handlePolicyFilterChange}
                            />
                          </NxTable.Cell>
                          <NxTable.Cell className="fw-bulk-waive__filter-component" colSpan={3}>
                            <NxFilterInput
                              placeholder="component name"
                              value={searchFiltersValues?.COMPONENT_COORDINATES || ''}
                              onChange={handleComponentFilterChange}
                            />
                          </NxTable.Cell>
                        </NxTableRow>
                      )}
                    </NxTable.Head>
                    <NxTable.Body emptyMessage="No violations to display">{createRows()}</NxTable.Body>
                  </NxTable>
                </NxTableContainer>
              </NxLoadWrapper>

              {source !== 'component-details' && !loading && (currentPage > 1 || hasMoreResults) && (
                <NxIndeterminatePagination
                  onPrevPageSelect={handlePreviousPage}
                  onNextPageSelect={handleNextPage}
                  isFirstPage={currentPage === 1}
                  isLastPage={!hasMoreResults}
                />
              )}

              <div
                style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '16px' }}
              >
                <div className="fw-bulk-waive__selected-count">
                  {selectedCount} {selectedCount === 1 ? 'violation' : 'violations'} selected
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                  <NxButton variant="tertiary" onClick={handleCancel}>
                    Cancel
                  </NxButton>
                  <NxButton variant="primary" disabled={selectedCount === 0} onClick={handleNext}>
                    Next
                  </NxButton>
                </div>
              </div>
            </NxTile.Content>
          </NxTile>
        </div>
      </NxPageMain>
    </>
  );
}
