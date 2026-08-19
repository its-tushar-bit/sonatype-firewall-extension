/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxFontAwesomeIcon,
  NxTable,
  NxTooltip,
  NxPagination,
  NxFilterInput,
  NX_STANDARD_DEBOUNCE_TIME,
  NxTile,
  NxToggle,
} from '@sonatype/react-shared-components';
import { debounce } from 'debounce';
import { isNil } from 'ramda';
import { faArrowDownWideShort } from '@fortawesome/pro-solid-svg-icons';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';
import { defaultIntegrationParamsMap, validIntegrationTypes } from './utils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function PrioritiesPageTable() {
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadTableData());

  const {
    loadingTableData,
    loadErrorTableData,
    priorities,
    page,
    pageCount,
    publicAppId: storedPublicId,
    scanId: storedScanId,
    componentNameFilter: componentNameFilterValue,
    filterOnPolicyActions: filterOnPolicyActionsValue,
    hasDefaultFilters,
    integrationType: storedIntegrationType,
    scanIdFromLatestBuildStageEvaluation,
    hasAutoWaiversConfigured,
    hasUserInteractedWithFilter,
  } = useSelector(selectPrioritiesPageSlice);

  const metadata = useSelector(selectApplicationReportMetaData);
  const { forMonitoring } = metadata || {};

  const currentRouteName = useSelector(selectCurrentRouteName);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;
  const currentParams = useSelector(selectRouterCurrentParams);
  const { publicAppId, scanId, filterOnPolicyActions, componentNameFilter, integrationType } = currentParams;

  const isIntegrationView = currentRouteName === 'prioritiesPageFromIntegrations';

  const checkAndGetValidIntegrationRoute = () => {
    if (isIntegrationView && !isNil(integrationType)) {
      return validIntegrationTypes.includes(integrationType) ? integrationType : 'cli';
    }
    return '';
  };

  const derivedActionFilter = filterOnPolicyActions === 'true' ? true : false;
  const derivedComponentName = componentNameFilter || '';

  const setPage = (page) => dispatch(actions.setPage(page));

  const priorityTooltip = `Priority of actionable items based on the policy action,
      component reachability status, and threat score severity.`;

  const setFilters = () => {
    dispatch(actions.setIntegrationType(null));
    dispatch(actions.setFilterOnPolicyActions(derivedActionFilter));
    dispatch(actions.setComponentNameFilter(derivedComponentName));
  };

  const setIntegrationViewFilters = () => {
    const integrationType = checkAndGetValidIntegrationRoute();
    const { filterOnPolicyActions } = defaultIntegrationParamsMap[integrationType];

    dispatch(
      stateGo(currentRouteName, {
        ...currentParams,
        filterOnPolicyActions: filterOnPolicyActions ? true : '',
        integrationType,
      })
    );

    dispatch(actions.setFilterOnPolicyActions(filterOnPolicyActions));
  };

  const setContinuousMonitoringViewFilters = () => {
    dispatch(actions.setIntegrationType(null));
    dispatch(actions.setFilterOnPolicyActions(false));
    dispatch(
      stateGo(currentRouteName, {
        ...currentParams,
        filterOnPolicyActions: '',
      })
    );
  };

  useEffect(() => {
    if (isIntegrationView) {
      dispatch(actions.setIntegrationType(integrationType));

      if (hasDefaultFilters) {
        setIntegrationViewFilters();
      }
    } else if (forMonitoring) {
      setContinuousMonitoringViewFilters();
    } else {
      setFilters();
    }

    //If page is viewed for a different applicationId and scanId, reset pagination
    if (publicAppId !== storedPublicId || scanId !== storedScanId) {
      setPage(0);
    }
    doLoad();
  }, [page]);

  // Wwhen integration type changes, set the default filters
  useEffect(() => {
    if (isIntegrationView && !hasDefaultFilters && integrationType !== storedIntegrationType) {
      dispatch(actions.setHasDefaultFilters(true));
    }
  }, [integrationType]);

  useEffect(() => {
    // Only restore focus if user has interacted with the input (not on first render)
    if (hasUserInteractedWithFilter) {
      const inputId = 'priorities-component-name-filter';
      const inputEl = document.getElementById(inputId);
      if (inputEl) {
        inputEl.focus();
      }
    }
  }, [componentNameFilterValue]);

  const removeDefaultFilters = () => {
    if (hasDefaultFilters) {
      dispatch(actions.setHasDefaultFilters(false));
    }
  };

  const filterByComponentName = (filter) => {
    removeDefaultFilters();
    dispatch(actions.setHasUserInteractedWithFilter(true)); // Mark that user has interacted (persists across remounts)
    dispatch(actions.setComponentNameFilter(filter));
    debouncedFilterComponentNameChange(filter);
  };

  const debouncedFilterComponentNameChange = useCallback(
    debounce((value) => {
      dispatch(
        stateGo(currentRouteName, {
          ...currentParams,
          componentNameFilter: value,
        })
      );
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  const handleActionToggleChange = () => {
    removeDefaultFilters();
    dispatch(actions.setFilterOnPolicyActions(!filterOnPolicyActionsValue));
    // if initial toggle state is false, clicking the toggle adds the actionFilter query param
    // if initial toggle state is true, clicking on the toggle removes the actionFilter query param
    dispatch(
      stateGo(currentRouteName, {
        ...currentParams,
        filterOnPolicyActions: !filterOnPolicyActionsValue ? true : '',
      })
    );
  };

  const getEmptyMessage = () => {
    if (componentNameFilterValue) return 'No Results';
    return filterOnPolicyActionsValue
      ? 'No violations with Fail/Warn policy actions were found during this evaluation.'
      : 'All clear! No violations were found during this evaluation.';
  };

  return (
    <NxTile>
      <div className="iq-priorities-page-filter-row">
        <NxFilterInput
          id="priorities-component-name-filter"
          placeholder="Filter by component"
          onChange={filterByComponentName}
          value={componentNameFilterValue}
        />
        <NxTooltip title={forMonitoring && 'Continous Monitoring'}>
          <NxToggle onChange={handleActionToggleChange} isChecked={filterOnPolicyActionsValue} disabled={forMonitoring}>
            Fail/Warn Policy Actions only
          </NxToggle>
        </NxTooltip>
      </div>
      <NxTile.Content>
        <div className="nx-table-container">
          <NxTable id="iq-priorities-table" className="iq-priorities-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell aria-label="Priority" className="nx-cell--num">
                  <NxTooltip title={priorityTooltip}>
                    <NxFontAwesomeIcon
                      className="iq-priorities-table__priority-column-header"
                      icon={faArrowDownWideShort}
                    />
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell>Component</NxTable.Cell>
                <NxTable.Cell>Build Action</NxTable.Cell>
                <NxTable.Cell>Reachability</NxTable.Cell>
                <NxTable.Cell>Suggested Remediation</NxTable.Cell>
                <NxTable.Cell>Next Step</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              isLoading={loadingTableData}
              retryHandler={doLoad}
              error={loadErrorTableData}
              emptyMessage={getEmptyMessage()}
            >
              <DataRows
                dataset={priorities}
                scanIdFromLatestBuildStageEvaluation={scanIdFromLatestBuildStageEvaluation}
                hasAutoWaiversConfigured={hasAutoWaiversConfigured}
              />
            </NxTable.Body>
          </NxTable>
          <div className="nx-table-container__footer">
            <NxPagination
              aria-controls="iq-priorities-table"
              pageCount={pageCount}
              currentPage={currentPage}
              onChange={setPage}
            />
          </div>
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

function DataRows({ dataset, scanIdFromLatestBuildStageEvaluation, hasAutoWaiversConfigured }) {
  const routerState = useRouterState();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const currentRouteName = useSelector(selectCurrentRouteName);

  const getCurrentPrioritiesContainer = () => {
    if (currentRouteName === 'prioritiesPageFromDashboard') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard';
    } else if (currentRouteName === 'prioritiesPageFromReports') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromReports';
    } else if (currentRouteName === 'prioritiesPageFromIntegrations') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromIntegrations';
    }
    return 'prioritiesPageContainer';
  };

  const prioritiesState = `${getCurrentPrioritiesContainer()}.componentDetails.overview`;
  const violationsState = `${getCurrentPrioritiesContainer()}.componentDetails.violations`;
  const getComponentHref = (hash) => routerState.href(prioritiesState, { hash, publicId: publicAppId, scanId });
  const getViolationHref = (hash) => routerState.href(violationsState, { hash, publicId: publicAppId, scanId });
  const getPrioritiesHref = (scanId) => routerState.href('prioritiesPageFromReports', { publicAppId, scanId });

  return (dataset ?? []).map((component) => {
    const { componentHash } = component;

    return (
      <PrioritiesPageRow
        key={componentHash}
        component={component}
        componentHref={getComponentHref(componentHash)}
        violationsHref={getViolationHref(componentHash)}
        latestBuildPrioritiesHref={getPrioritiesHref(scanIdFromLatestBuildStageEvaluation)}
        hasAutoWaiversConfigured={hasAutoWaiversConfigured}
      />
    );
  });
}
