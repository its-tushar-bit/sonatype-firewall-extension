/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxButton,
  NxCheckbox,
  NxFilterInput,
  NxPagination,
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
  selectComponentDetailsPolicyNameFilter,
  selectComponentDetailsConstraintNameFilter,
} from '../firewallBulkWaiverSelectors';
import { loadComponentPolicyViolations } from 'MainRoot/firewall/firewallActions';
import { actions as policyViolationsActions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';
import {
  selectFirewallPolicyViolations,
  selectFirewallComponentDetailsPage,
} from 'MainRoot/firewall/firewallSelectors';
import BulkWaiveTableRow from './bulkWaiveTableRow/BulkWaiveTableRow';
import BulkWaiveTitle from '../bulkWaiveTitle/BulkWaiveTitle';
import RepositoryResultsComponentsFilter from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsComponentsTable/repositoryResultsComponentsFilter/RepositoryResultsComponentsFilter';
import FirewallPolicyViolationDetailsPopover from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationDetailsPopover';

const DEFAULT_PAGE_SIZE = 12;

export default function FirewallBulkWaivePage() {
  const dispatch = useDispatch();
  const routerParams = useSelector(selectRouterCurrentParams);
  const { repositoryId } = routerParams;
  // Dual-state pattern: local state drives rendering; Redux stores a checkpoint of the selection.
  // When the user navigates away (e.g. to adjust filters) and returns, the useEffect below
  // re-seeds local state from Redux so the selection is preserved across navigation.
  // Refs (checkboxStateRef, selectAllModeRef) mirror the local state to give stale-closure-safe
  // access inside useCallback/useEffect handlers without causing extra re-renders.
  const [checkboxState, setCheckboxState] = useState({});
  const [selectAllMode, setSelectAllMode] = useState(false);
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
  const componentDetailsPolicyNameFilter = useSelector(selectComponentDetailsPolicyNameFilter);
  const componentDetailsConstraintNameFilter = useSelector(selectComponentDetailsConstraintNameFilter);

  const filteredComponentViolations = useMemo(() => {
    let result = componentViolations ?? [];
    if (componentDetailsPolicyNameFilter) {
      const policyFilter = String(componentDetailsPolicyNameFilter).toLowerCase();
      result = result.filter((v) => v.policyName?.toLowerCase().includes(policyFilter));
    }
    if (componentDetailsConstraintNameFilter) {
      const constraintFilter = String(componentDetailsConstraintNameFilter).toLowerCase();
      result = result.filter((v) =>
        v.constraints?.[0]?.constraintName?.toLowerCase().includes(constraintFilter)
      );
    }
    return result;
  }, [componentViolations, componentDetailsPolicyNameFilter, componentDetailsConstraintNameFilter]);

  const violations = source === 'component-details' ? filteredComponentViolations : repositoryViolations;

  const repositoryLoading = useSelector(selectLoadingRepositoryComponents);
  const repositoryError = useSelector(selectErrorComponentsTable);
  const componentLoading = componentDetailsPage?.isLoadingPolicyViolations || false;
  const componentError = componentDetailsPage?.policyViolationsError || null;

  const loading = source === 'component-details' ? componentLoading : repositoryLoading;
  const error = source === 'component-details' ? componentError : repositoryError;

  const currentPage = useSelector(selectCurrentPage);
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

        dispatch(actions.setPageSize(DEFAULT_PAGE_SIZE));
        dispatch(actions.getRepositoryInformation(repositoryId));
        dispatch(actions.getRepositorySummary(repositoryId));
        dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
      }
    }

    return () => {
      if (repositoryId && source !== 'component-details') {
        dispatch(actions.setPageSize(DEFAULT_PAGE_SIZE));
      }
      if (source === 'component-details') {
        dispatch(firewallBulkWaiverActions.setComponentDetailsPolicyNameFilter(''));
        dispatch(firewallBulkWaiverActions.setComponentDetailsConstraintNameFilter(''));
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

  const normalizeViolation = (violation) => ({
    ...violation,
    threatLevel: violation.threatLevel ?? violation.policyThreatLevel,
    matchState: violation.matchState ?? violation.matchStateId ?? sourceContext?.matchState,
  });

  const effectiveTotalCount =
    source === 'component-details'
      ? violations?.length || 0
      : filteredTotalCount != null
      ? filteredTotalCount
      : totalComponentCount;

  const buildCurrentPageSelections = (nextCheckboxState, nextSelectAllMode) => {
    return (
      violations?.filter((violation) => {
        if (nextSelectAllMode) {
          return nextCheckboxState[violation.policyViolationId] !== false;
        }

        return nextCheckboxState[violation.policyViolationId] === true;
      }) || []
    ).map(normalizeViolation);
  };

  const buildPersistedSelectedViolations = (nextCheckboxState, nextSelectAllMode) => {
    const currentPageSelections = buildCurrentPageSelections(nextCheckboxState, nextSelectAllMode);

    if (nextSelectAllMode) {
      if (source === 'component-details') {
        return currentPageSelections;
      }

      if (currentPageSelections.length > 0) {
        return currentPageSelections.slice(0, 5);
      }

      return storedSelectedViolations
        .filter((violation) => nextCheckboxState[violation.policyViolationId] !== false)
        .slice(0, 5);
    }

    const currentPageIds = new Set((violations || []).map((violation) => violation.policyViolationId));
    const persistedSelections = storedSelectedViolations.filter(
      (violation) => !currentPageIds.has(violation.policyViolationId)
    );
    const existingIds = new Set(persistedSelections.map((violation) => violation.policyViolationId));

    currentPageSelections.forEach((violation) => {
      if (!existingIds.has(violation.policyViolationId)) {
        persistedSelections.push(violation);
      }
    });

    return persistedSelections;
  };

  const buildSelectedCount = (nextCheckboxState, nextSelectAllMode) => {
    if (nextSelectAllMode) {
      const deselectedCount = Object.values(nextCheckboxState).filter((value) => value === false).length;
      return effectiveTotalCount - deselectedCount;
    }

    return buildPersistedSelectedViolations(nextCheckboxState, false).length;
  };

  const persistSelectionState = (nextCheckboxState, nextSelectAllMode = selectAllMode) => {
    const persistedSelectedViolations = buildPersistedSelectedViolations(nextCheckboxState, nextSelectAllMode);
    const persistedSelectedCount = buildSelectedCount(nextCheckboxState, nextSelectAllMode);

    dispatch(firewallBulkWaiverActions.setSelectedViolations(persistedSelectedViolations));
    dispatch(firewallBulkWaiverActions.setSelectedCount(persistedSelectedCount));
    dispatch(firewallBulkWaiverActions.setSelectAllMode(nextSelectAllMode));
    dispatch(firewallBulkWaiverActions.setCheckboxState(nextCheckboxState));

    return { persistedSelectedViolations, persistedSelectedCount };
  };

  const handleSort = (sortableField) => {
    if (source !== 'component-details') {
      if (selectAllMode) {
        handleClearAllFilteredViolations();
      } else {
        persistSelectionState(checkboxState, false);
      }
      dispatch(actions.setSorting(sortableField));
      dispatch(actions.setCurrentPage(1));
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

  const handlePageChange = (pageNumber) => {
    persistSelectionState(checkboxState, selectAllMode);
    // NxPagination uses 0-based indexing, but backend expects 1-based
    dispatch(actions.setCurrentPage(pageNumber + 1));
    dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
  };

  const policyFilterTimeoutRef = useRef(null);
  const componentFilterTimeoutRef = useRef(null);
  const checkboxStateRef = useRef(checkboxState);
  const selectAllModeRef = useRef(selectAllMode);
  const persistSelectionStateRef = useRef(persistSelectionState);

  useEffect(() => {
    checkboxStateRef.current = checkboxState;
  }, [checkboxState]);

  useEffect(() => {
    selectAllModeRef.current = selectAllMode;
  }, [selectAllMode]);

  // No deps array: intentionally runs every render to keep the ref current with the latest function.
  useEffect(() => {
    persistSelectionStateRef.current = persistSelectionState;
  });

  const handleFilterChange = useCallback(
    (filterName, value, timeoutRef) => {
      if (selectAllModeRef.current) {
        setSelectAllMode(false);
        setCheckboxState({});
        dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
        dispatch(firewallBulkWaiverActions.setSelectedCount(0));
        dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
        dispatch(firewallBulkWaiverActions.setCheckboxState({}));
      } else {
        persistSelectionStateRef.current(checkboxStateRef.current, false);
      }

      dispatch(actions.setFilter({ filterName, filterValue: value }));
      dispatch(actions.setCurrentPage(1));

      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current);
      }

      timeoutRef.current = setTimeout(() => {
        dispatch(actions.getRepositoryComponentsForBulkWaive(repositoryId));
      }, 500);
    },
    [dispatch, repositoryId]
  );

  const handlePolicyFilterChange = useCallback(
    (value) => handleFilterChange('POLICY_NAME', value, policyFilterTimeoutRef),
    [handleFilterChange]
  );

  const handleComponentFilterChange = useCallback(
    (value) => handleFilterChange('COMPONENT_COORDINATES', value, componentFilterTimeoutRef),
    [handleFilterChange]
  );

  const handleComponentDetailsPolicyFilterChange = useCallback(
    (value) => {
      dispatch(firewallBulkWaiverActions.setComponentDetailsPolicyNameFilter(value));
    },
    [dispatch]
  );

  const handleComponentDetailsConstraintFilterChange = useCallback(
    (value) => {
      dispatch(firewallBulkWaiverActions.setComponentDetailsConstraintNameFilter(value));
    },
    [dispatch]
  );

  const openFilterPopover = () => {
    dispatch(actions.setShowFilterPopover(true));
  };

  const handleSelectAll = (event) => {
    event?.stopPropagation();
    event?.preventDefault();

    if (!violations?.length) return;

    const allCurrentPageSelected = violations.length > 0 && violations.every((violation) => {
      const { policyViolationId } = violation;
      return selectAllMode ? checkboxState[policyViolationId] !== false : checkboxState[policyViolationId] === true;
    });

    if (allCurrentPageSelected) {
      const newState = { ...checkboxState };

      violations.forEach((violation) => {
        if (selectAllMode) {
          newState[violation.policyViolationId] = false;
        } else {
          delete newState[violation.policyViolationId];
        }
      });

      setCheckboxState(newState);
      persistSelectionState(newState, selectAllMode);
    } else {
      const newState = { ...checkboxState };

      violations.forEach((violation) => {
        if (selectAllMode) {
          delete newState[violation.policyViolationId];
        } else {
          newState[violation.policyViolationId] = true;
        }
      });

      setCheckboxState(newState);
      persistSelectionState(newState, selectAllMode);
    }
  };

  const handleSelectAllFilteredViolations = () => {
    const nextCheckboxState = {};

    setSelectAllMode(true);
    setCheckboxState(nextCheckboxState);
    persistSelectionState(nextCheckboxState, true);
  };

  const handleClearAllFilteredViolations = () => {
    setSelectAllMode(false);
    setCheckboxState({});
    dispatch(firewallBulkWaiverActions.setSelectedViolations([]));
    dispatch(firewallBulkWaiverActions.setSelectedCount(0));
    dispatch(firewallBulkWaiverActions.setSelectAllMode(false));
    dispatch(firewallBulkWaiverActions.setCheckboxState({}));
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
    persistSelectionState(checkboxState, selectAllMode);
    dispatch(stateGo('firewall.bulkWaiveConfiguration', { repositoryId }));
  };

  const createRows = () => {
    return violations?.map((component, index) => {
      const policyViolationId = component.policyViolationId;
      const policyName = component.policyName;
      const threatLevel = component.threatLevel ?? component.policyThreatLevel;
      const onRowClick = () => {
        dispatch(policyViolationsActions.setViolationsDetailRowClicked());
        dispatch(policyViolationsActions.toggleShowViolationsDetailPopover());
        dispatch(
          policyViolationsActions.setSelectedPolicyViolation({
            ...component,
            policyViolationId,
            policyName,
            policyThreatLevel: threatLevel,
          })
        );
      };
      const onCheckboxClick = (event) => {
        event.stopPropagation();
        event.preventDefault();

        if (selectAllMode) {
          const newState = { ...checkboxState };
          if (newState[policyViolationId] === false) {
            delete newState[policyViolationId];
          } else {
            newState[policyViolationId] = false;
          }
          setCheckboxState(newState);
          persistSelectionState(newState, true);
        } else {
          const newState = { ...checkboxState };
          if (newState[policyViolationId]) {
            delete newState[policyViolationId];
          } else {
            newState[policyViolationId] = true;
          }
          setCheckboxState(newState);
          persistSelectionState(newState, false);
        }
      };
      const condition = getViolationCondition(component);
      const isRowChecked = selectAllMode
        ? checkboxState[policyViolationId] !== false
        : checkboxState[policyViolationId] === true;

      const normalizedComponent = normalizeViolation(component);

      return (
        <BulkWaiveTableRow
          key={policyViolationId || `row-${index}`}
          component={normalizedComponent}
          condition={condition}
          onClick={onRowClick}
          onCheckboxClick={onCheckboxClick}
          isChecked={isRowChecked}
          isComponentBulkWaive={source === 'component-details'}
          checkboxId={`checkbox-${policyViolationId}`}
        />
      );
    });
  };

  const deselectedCount = Object.values(checkboxState).filter((value) => value === false).length;

  const selectedCount = selectAllMode ? effectiveTotalCount - deselectedCount : buildSelectedCount(checkboxState, false);

  const isAllSelected =
    selectAllMode || (violations?.length > 0 && violations.every((v) => checkboxState[v.policyViolationId]));

  const pageSize = componentsRequestBody?.pageSize || DEFAULT_PAGE_SIZE;
  const totalPages = filteredTotalCount != null ? Math.max(1, Math.ceil(filteredTotalCount / pageSize)) : 1;
  const showingCount = violations?.length || 0;
  const totalVisibleCount =
    source === 'component-details'
      ? showingCount
      : filteredTotalCount != null
      ? filteredTotalCount
      : totalComponentCount;
  const shouldShowSelectAllFilteredAction =
    source !== 'component-details' &&
    totalVisibleCount != null &&
    (selectAllMode || totalVisibleCount > showingCount);

  return (
    <>
      {source !== 'component-details' && (
        <RepositoryResultsComponentsFilter repositoryId={repositoryId} isBulkWaivePage={true} />
      )}
      <FirewallPolicyViolationDetailsPopover />
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
            </NxTile.Header>

            <NxTile.Content>
              {!loading && !error && (
                <div className="fw-bulk-waive-page__toolbar">
                  <div className="fw-bulk-waive-page__summary">
                    <div className="fw-bulk-waive__selected-count">
                      {selectedCount} {selectedCount === 1 ? 'violation' : 'violations'} selected
                    </div>
                    {source !== 'component-details' && totalVisibleCount != null && (
                      <div className="fw-bulk-waive-page__results-count">
                        Showing {showingCount} of {totalVisibleCount} results
                      </div>
                    )}
                  </div>
                  <div className="fw-bulk-waive-page__actions">
                    {source !== 'component-details' && (
                      <NxButton onClick={openFilterPopover} variant="tertiary" id="fw-bulk-waive-filter-button">
                        <NxFontAwesomeIcon icon={faFilter} />
                        <span>Filter</span>
                      </NxButton>
                    )}
                    {shouldShowSelectAllFilteredAction && (
                      <NxButton
                        variant="tertiary"
                        id="fw-bulk-waive-select-all-filtered"
                        onClick={selectAllMode ? handleClearAllFilteredViolations : handleSelectAllFilteredViolations}
                      >
                        {selectAllMode ? 'Unselect all violations' : `Select all ${totalVisibleCount} violations`}
                      </NxButton>
                    )}
                  </div>
                </div>
              )}

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
                          {source === 'component-details' ? 'CONSTRAINT' : 'COMPONENT'}
                        </NxTable.Cell>
                        <NxTable.Cell className="fw-bulk-waive__condition-name-cell">CONDITION</NxTable.Cell>
                        <NxTable.Cell />
                      </NxTable.Row>
                      <NxTableRow isFilterHeader>
                        <NxTable.Cell colSpan={2} />
                        <NxTable.Cell className="fw-bulk-waive__filter-policy">
                          <NxFilterInput
                            placeholder="policy name"
                            value={
                              source === 'component-details'
                                ? String(componentDetailsPolicyNameFilter ?? '')
                                : searchFiltersValues?.POLICY_NAME || ''
                            }
                            onChange={
                              source === 'component-details'
                                ? handleComponentDetailsPolicyFilterChange
                                : handlePolicyFilterChange
                            }
                          />
                        </NxTable.Cell>
                        {source !== 'component-details' ? (
                          <NxTable.Cell className="fw-bulk-waive__filter-component" colSpan={3}>
                            <NxFilterInput
                              placeholder="component name"
                              value={searchFiltersValues?.COMPONENT_COORDINATES || ''}
                              onChange={handleComponentFilterChange}
                            />
                          </NxTable.Cell>
                        ) : (
                          <>
                            <NxTable.Cell className="fw-bulk-waive__filter-constraint">
                              <NxFilterInput
                                placeholder="constraint name"
                                value={String(componentDetailsConstraintNameFilter ?? '')}
                                onChange={handleComponentDetailsConstraintFilterChange}
                              />
                            </NxTable.Cell>
                            <NxTable.Cell colSpan={2} />
                          </>
                        )}
                      </NxTableRow>
                    </NxTable.Head>
                    <NxTable.Body emptyMessage="No violations to display">{createRows()}</NxTable.Body>
                  </NxTable>
                </NxTableContainer>
              </NxLoadWrapper>

              {source !== 'component-details' && !loading && totalPages > 1 && (
                <div className="fw-bulk-waive-page__pagination">
                  <NxPagination currentPage={currentPage - 1} pageCount={totalPages} onChange={handlePageChange} />
                </div>
              )}

              <div className="fw-bulk-waive-page__footer">
                <div className="fw-bulk-waive-page__footer-actions">
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
