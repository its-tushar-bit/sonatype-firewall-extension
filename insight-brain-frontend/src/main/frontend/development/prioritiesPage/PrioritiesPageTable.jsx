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
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
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
  } = useSelector(selectPrioritiesPageSlice);
  const currentPage = pageCount && pageCount > 0 ? page - 1 : null;
  const isFirstPage = page === 1;

  const getTopPrioritiesLabel = () => {
    if (isNilOrEmpty(topPrioritiesData)) {
      return 'Top Priorities';
    }

    if (topPrioritiesData.length === 1) {
      return 'Top Priority';
    }

    return `Top ${topPrioritiesData.length} Priorities`;
  };

  const setPage = (page) => dispatch(actions.setPage(page));

  useEffect(() => {
    doLoad();
  }, [page]);

  return (
    <>
      <div className="nx-table-container">
        <NxTable className="iq-priorities-page-table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell>
                <NxTooltip title="Some title">
                  <span>
                    Priority <NxFontAwesomeIcon className="iq-priorities-page-table-info-icon" icon={faInfoCircle} />
                  </span>
                </NxTooltip>
              </NxTable.Cell>
              <NxTable.Cell>Component</NxTable.Cell>
              <NxTable.Cell>Highest Policy Threat</NxTable.Cell>
              <NxTable.Cell>Recommendation</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body isLoading={loadingTableData} retryHandler={doLoad} error={loadErrorTableData}>
            {isFirstPage && (
              <>
                <NxTable.Row>
                  <NxTable.Cell className="iq-priorities-page-priority-findings-toggle" colSpan={5}>
                    <NxAccordion open={showPriorityFindings} onToggle={toggleShowPriorityFindings}>
                      <NxAccordion.Header>
                        <NxAccordion.Title>{getTopPrioritiesLabel()}</NxAccordion.Title>
                      </NxAccordion.Header>
                    </NxAccordion>
                  </NxTable.Cell>
                </NxTable.Row>
                {showPriorityFindings && <DataRows dataset={topPrioritiesData} />}
              </>
            )}
            <NxTable.Row>
              <NxTable.Cell className="iq-priorities-page-all-findings" colSpan={5}>
                Remaining Findings
              </NxTable.Cell>
            </NxTable.Row>
            <DataRows dataset={additionalPrioritiesData} />
          </NxTable.Body>
        </NxTable>
        {additionalPrioritiesData && (
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
  const dispatchComponentDetailsPage = (hash) =>
    dispatch(stateGo('prioritiesPageContainer.componentDetails.overview', { hash, publicId: publicAppId, scanId }));
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
