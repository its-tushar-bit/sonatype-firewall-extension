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
    optionalComponentNameFilter,
    optionalActionFilter,
  } = useSelector(selectPrioritiesPageSlice);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);

  const setPage = (page) => dispatch(actions.setPage(page));

  const priorityTooltip = `Priority of actionable items based on this application's policy, component reachability status, recommendation availability, and threat score severity.`;

  const filterByComponentName = (filter) => {
    dispatch(actions.setComponentNameFilter(filter));
    debouncedFilterComponentNameChange(filter);
  };

  useEffect(() => {
    //If page is viewed for a different applicationId and scanId, reset pagination
    if (publicAppId !== storedPublicId || scanId !== storedScanId) {
      setPage(0);
    }
    doLoad();
  }, [page]);

  const debouncedFilterComponentNameChange = useCallback(
    debounce((value) => {
      dispatch(actions.loadTableData(value));
    }, NX_STANDARD_DEBOUNCE_TIME),
    []
  );

  const handleActionToggleChange = () => {
    dispatch(actions.setOptionalActionFilter(!optionalActionFilter));
    doLoad();
  };

  const getEmptyMessage = () => {
    if (optionalComponentNameFilter) return 'No Results';
    return optionalActionFilter
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
          value={optionalComponentNameFilter}
        />

        <NxToggle onChange={handleActionToggleChange} isChecked={optionalActionFilter}>
          Fail/Warn Policy Actions only
        </NxToggle>
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
              <DataRows dataset={priorities} />
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

function DataRows({ dataset }) {
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

    return <PrioritiesPageRow key={componentHash} component={component} onClick={onRowClick} />;
  });
}
