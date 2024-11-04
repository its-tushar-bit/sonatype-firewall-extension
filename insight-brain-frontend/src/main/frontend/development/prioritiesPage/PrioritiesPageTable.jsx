/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useCallback, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxAccordion,
  NxFontAwesomeIcon,
  NxTable,
  NxTooltip,
  useToggle,
  NxPagination,
  NxFilterInput,
  NX_STANDARD_DEBOUNCE_TIME,
} from '@sonatype/react-shared-components';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { selectComponent } from 'MainRoot/applicationReport/applicationReportActions';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { debounce } from 'debounce';
import { TABLE_PAGE_SIZE } from './slices/prioritiesPageSlice';

export default function PrioritiesPageTable() {
  const [showPriorityFindings, toggleShowPriorityFindings] = useToggle(true);

  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadTableData());

  const {
    loadingTableData,
    loadErrorTableData,
    topPrioritiesData,
    additionalPrioritiesData,
    page,
    pageCount,
    publicAppId: storedPublicId,
    scanId: storedScanId,
    optionalComponentNameFilter,
  } = useSelector(selectPrioritiesPageSlice);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;
  const isFirstPage = page === 1;

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);

  const hasZeroFindings = isNilOrEmpty(topPrioritiesData) && isNilOrEmpty(additionalPrioritiesData);
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

  return (
    <>
      <div className="nx-table-container">
        <NxTable className="iq-priorities-page-table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>
                <NxTooltip title={priorityTooltip}>
                  <span>
                    Priority <NxFontAwesomeIcon className="iq-priorities-page-table-info-icon" icon={faInfoCircle} />
                  </span>
                </NxTooltip>
              </NxTable.Cell>
              <NxTable.Cell>Component</NxTable.Cell>
              <NxTable.Cell>Policy</NxTable.Cell>
              <NxTable.Cell>Recommendation</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
            <NxTable.Row className="nx-table-row--filter-header">
              <NxTable.Cell />
              <NxTable.Cell>
                <NxFilterInput
                  id="priorities-component-name-filter"
                  placeholder="component name"
                  onChange={filterByComponentName}
                  value={optionalComponentNameFilter}
                />
              </NxTable.Cell>
              <NxTable.Cell colSpan={3} />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            isLoading={loadingTableData}
            retryHandler={doLoad}
            error={loadErrorTableData}
            emptyMessage={
              !optionalComponentNameFilter
                ? 'All clear! No violations were found during this evaluation.'
                : 'No Results'
            }
          >
            <>
              {!hasZeroFindings && isFirstPage && (
                <>
                  {!optionalComponentNameFilter && (
                    <NxTable.Row>
                      <NxTable.Cell className="iq-priorities-page-priority-findings-toggle" colSpan={5}>
                        <NxAccordion open={showPriorityFindings} onToggle={toggleShowPriorityFindings}>
                          <NxAccordion.Header>
                            <NxAccordion.Title>Top Priorities</NxAccordion.Title>
                          </NxAccordion.Header>
                        </NxAccordion>
                      </NxTable.Cell>
                    </NxTable.Row>
                  )}
                  {showPriorityFindings && <DataRows dataset={topPrioritiesData} page={page} indexOffset={0} />}
                </>
              )}
              {!hasZeroFindings && !optionalComponentNameFilter && (
                <NxTable.Row>
                  <NxTable.Cell className="iq-priorities-page-all-findings" colSpan={5}>
                    Remaining Priorities
                  </NxTable.Cell>
                </NxTable.Row>
              )}
              <DataRows dataset={additionalPrioritiesData} page={page} indexOffset={topPrioritiesData?.length} />
            </>
          </NxTable.Body>
        </NxTable>
        {!hasZeroFindings && additionalPrioritiesData && (
          <div className="nx-table-container__footer">
            <NxPagination
              aria-controls="pagination-table"
              pageCount={pageCount}
              currentPage={currentPage}
              onChange={setPage}
            />
          </div>
        )}
      </div>
    </>
  );
}

function DataRows({ dataset, page, indexOffset }) {
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

  return dataset.map((component, idx) => {
    const { componentHash } = component;

    const onRowClick = () => {
      setSelectedComponent(idx);
      dispatchComponentDetailsPage(componentHash);
    };

    const index = idx + (page - 1) * TABLE_PAGE_SIZE + 1 + indexOffset;

    return <PrioritiesPageRow key={componentHash} component={component} onClick={onRowClick} index={index} />;
  });
}
