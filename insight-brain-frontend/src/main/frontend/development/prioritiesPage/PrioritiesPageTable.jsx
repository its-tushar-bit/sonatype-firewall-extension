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
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { selectComponent } from 'MainRoot/applicationReport/applicationReportActions';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { debounce } from 'debounce';
import { isNil } from 'ramda';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';

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
  } = useSelector(selectPrioritiesPageSlice);

  const hasPolicyAction = priorities?.find((priority) => priority.action === 'fail' || priority.action === 'warn');

  const metadata = useSelector(selectApplicationReportMetaData);
  const { forMonitoring } = metadata || {};

  const currentRouteName = useSelector(selectCurrentRouteName);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;
  const { publicAppId, scanId, filterOnPolicyActions, componentNameFilter } = useSelector(selectRouterCurrentParams);

  const derivedActionFilter = filterOnPolicyActions === 'true' ? true : false;
  const derivedComponentName = isNil(componentNameFilter) ? '' : componentNameFilter;

  const setPage = (page) => dispatch(actions.setPage(page));

  const priorityTooltip = `Priority of actionable items based on the policy action, component reachability status, and threat score severity.`;

  const filterByComponentName = (filter) => {
    dispatch(actions.setComponentNameFilter(filter));
    debouncedFilterComponentNameChange(filter);
  };

  useEffect(() => {
    if (forMonitoring) {
      dispatch(actions.setFilterOnPolicyActions(false));
      dispatch(
        stateGo(currentRouteName, {
          publicAppId,
          scanId,
          filterOnPolicyActions: '',
          componentNameFilter: derivedComponentName,
        })
      );
    } else {
      dispatch(actions.setFilterOnPolicyActions(derivedActionFilter));
    }
    dispatch(actions.setComponentNameFilter(derivedComponentName));

    //If page is viewed for a different applicationId and scanId, reset pagination
    if (publicAppId !== storedPublicId || scanId !== storedScanId) {
      setPage(0);
    }
    doLoad();
  }, [page]);

  const debouncedFilterComponentNameChange = useCallback(
    debounce((value) => {
      dispatch(
        stateGo(currentRouteName, {
          publicAppId,
          scanId,
          filterOnPolicyActions: filterOnPolicyActionsValue ? true : '',
          componentNameFilter: value,
        })
      );
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  const handleActionToggleChange = () => {
    dispatch(actions.setFilterOnPolicyActions(!filterOnPolicyActionsValue));
    // if initial toggle state is false, clicking the toggle adds the actionFilter query param
    // if initial toggle state is true, clicking on the toggle removes the actionFilter query param
    dispatch(
      stateGo(currentRouteName, {
        publicAppId,
        scanId,
        filterOnPolicyActions: !filterOnPolicyActionsValue ? true : '',
        componentNameFilter: derivedComponentName,
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
          <NxTable className="iq-priorities-page-table nx-table--fixed-layout">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell className="iq-priorities-page-priority-header-cell">
                  <NxTooltip title={priorityTooltip}>
                    <span>
                      Priority <NxFontAwesomeIcon className="iq-priorities-page-table-info-icon" icon={faInfoCircle} />
                    </span>
                  </NxTooltip>
                </NxTable.Cell>
                <NxTable.Cell>Component</NxTable.Cell>
                <NxTable.Cell>Reason for priority</NxTable.Cell>
                <NxTable.Cell className="iq-priorities-page-suggested-fix-header-cell">Suggested fix</NxTable.Cell>
                <NxTable.Cell chevron />
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body
              isLoading={loadingTableData}
              retryHandler={doLoad}
              error={loadErrorTableData}
              emptyMessage={getEmptyMessage()}
            >
              <DataRows dataset={priorities} hasPolicyAction={!!hasPolicyAction} />
            </NxTable.Body>
          </NxTable>
          <div className="nx-table-container__footer">
            <NxPagination
              aria-controls="pagination-table"
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

function DataRows({ dataset, hasPolicyAction }) {
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const setSelectedComponent = (idx) => dispatch(selectComponent(idx));
  const currentRouteName = useSelector(selectCurrentRouteName);

  const getCurrentPrioritiesContainer = () => {
    if (currentRouteName === 'prioritiesPageFromDashboard') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromDashboard';
    } else if (currentRouteName === 'prioritiesPageFromReports') {
      return 'componentDetailsPageWithinPrioritiesPageContainerFromReports';
    }
    return 'prioritiesPageContainer';
  };

  const prioritiesState = `${getCurrentPrioritiesContainer()}.componentDetails.overview`;

  const dispatchComponentDetailsPage = (hash) =>
    dispatch(stateGo(prioritiesState, { hash, publicId: publicAppId, scanId }));
  if (!dataset) return [];

  return dataset.map((component, index) => {
    const { componentHash } = component;

    const onRowClick = () => {
      setSelectedComponent(index);
      dispatchComponentDetailsPage(componentHash);
    };

    return (
      <PrioritiesPageRow
        key={componentHash}
        component={component}
        onClick={onRowClick}
        hasPolicyAction={hasPolicyAction}
      />
    );
  });
}
