/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxAccordion,
  NxFontAwesomeIcon,
  NxTable,
  NxTooltip,
  useToggle,
  NxPagination,
} from '@sonatype/react-shared-components';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { selectComponent } from 'MainRoot/applicationReport/applicationReportActions';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

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
    metadata: {
      application: { publicId: storedPublicId },
      scanId: storedScanId,
    },
  } = useSelector(selectPrioritiesPageSlice);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;
  const isFirstPage = page === 1;

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);

  const hasZeroFindings = isNilOrEmpty(topPrioritiesData) && isNilOrEmpty(additionalPrioritiesData);
  const setPage = (page) => dispatch(actions.setPage(page));

  const priorityTooltip = `Priority of actionable items based on this application's policy, component reachability status, recommendation availability, and threat score severity.`;

  useEffect(() => {
    //If page is viewed for a different applicationId and scanId, reset pagination
    if (publicAppId !== storedPublicId || scanId !== storedScanId) {
      setPage(0);
    }
    doLoad();
  }, [page]);

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
          </NxTable.Head>
          <NxTable.Body
            isLoading={loadingTableData}
            retryHandler={doLoad}
            error={loadErrorTableData}
            emptyMessage="All clear! No violations were found during this evaluation."
          >
            {!hasZeroFindings && isFirstPage && (
              <>
                <NxTable.Row>
                  <NxTable.Cell className="iq-priorities-page-priority-findings-toggle" colSpan={5}>
                    <NxAccordion open={showPriorityFindings} onToggle={toggleShowPriorityFindings}>
                      <NxAccordion.Header>
                        <NxAccordion.Title>Top Priorities</NxAccordion.Title>
                      </NxAccordion.Header>
                    </NxAccordion>
                  </NxTable.Cell>
                </NxTable.Row>
                {showPriorityFindings && <DataRows dataset={topPrioritiesData} />}
              </>
            )}
            {!hasZeroFindings && (
              <NxTable.Row>
                <NxTable.Cell className="iq-priorities-page-all-findings" colSpan={5}>
                  Remaining Priorities
                </NxTable.Cell>
              </NxTable.Row>
            )}
            <DataRows dataset={additionalPrioritiesData} />
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

function DataRows({ dataset }) {
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const setSelectedComponent = (idx) => dispatch(selectComponent(idx));
  const currentRouteName = useSelector(selectCurrentRouteName);

  const getCurrentPrioritiesContainer = () => {
    if (currentRouteName === 'prioritiesPageFromDashboard') {
      return 'appReportPageWithinPrioritiesPageContainerFromDashboard';
    } else if (currentRouteName === 'prioritiesPageFromReports') {
      return 'appReportPageWithinPrioritiesPageContainerFromReports';
    } else if (currentRouteName === 'prioritiesPageFromAppReport') {
      return 'appReportPageWithinPrioritiesPageContainerFromAppReport';
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

    return <PrioritiesPageRow key={component.displayName} component={component} onClick={onRowClick} />;
  });
}
