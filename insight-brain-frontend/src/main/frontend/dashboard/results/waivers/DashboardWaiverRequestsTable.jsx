/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectDashboardFilter, selectWaiverRequests } from '../../dashboardSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectWaiverReasonsState } from 'MainRoot/waivers/requestWaiverSelectors';
import {
  loadWaiverRequestsResults,
  sortWaiverRequests,
  setNextRequestsPage,
  setPreviousRequestsPage,
} from '../dashboardResultsActions';
import { NxTable, NxIndeterminatePagination, NxTableContainer } from '@sonatype/react-shared-components';
import { equals } from 'ramda';

import { Messages } from 'MainRoot/util/CommonServices';
import DashboardMask from '../dashboardMask/DashboardMask';
import DashboardWaiverRequestsTableRow from './DashboardWaiverRequestsTableRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectHasWaiverRequestWorkflow } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';
import { extractSortFieldName } from 'MainRoot/util/sortUtils';
import { waiverRequestStatus } from 'MainRoot/util/waiverUtils';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';

const DEFAULT_SORT_FIELDS = [
  ['-threatLevel'],
  ['requestTime'],
  ['requesterName'],
  ['policyName'],
  ['scope'],
  ['status'],
];

export default function DashboardWaiverRequestsTable() {
  const dispatch = useDispatch();
  const loadWaiverRequests = () => dispatch(loadWaiverRequestsResults());
  const sortRequests = (sortFields) => {
    dispatch(sortWaiverRequests(sortFields));
  };
  const dispatchNextPage = () => dispatch(setNextRequestsPage());
  const dispatchPreviousPage = () => dispatch(setPreviousRequestsPage());
  const waiverRequests = useSelector(selectWaiverRequests);
  const waiverReasonsState = useSelector(selectWaiverReasonsState);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const hasWaiverRequestWorkflow = useSelector(selectHasWaiverRequestWorkflow);
  const {
    loading: filterLoading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter: { maxDaysOld },
  } = useSelector(selectDashboardFilter);
  const { results, hasNextPage, sortFields, error, hasMultiplePages, page } = waiverRequests;

  const isLoading = (!results && !error && !needsAcknowledgement) || waiverReasonsState.loading,
    currentPage = hasMultiplePages ? page : null,
    sortedColumn = extractSortFieldName(sortFields[0]),
    isSortReversed = sortFields[0].includes('-'),
    emptyMessage =
      'No data available ' +
      (maxDaysOld ? `in the last ${maxDaysOld} days ` : '') +
      'given the applied filters and permissions.',
    colSpan = 6;

  const getColumnDirection = (index, sortInverted = false) => {
    if (!results || !results.length || error) {
      return null;
    }

    const columnFields = DEFAULT_SORT_FIELDS[index],
      currentColumn = extractSortFieldName(columnFields[0]),
      isCurrentColumnSorted = sortedColumn === currentColumn,
      isUp = isCurrentColumnSorted && (sortInverted ? isSortReversed : !isSortReversed),
      isDown = isCurrentColumnSorted && (!sortInverted ? isSortReversed : !isSortReversed);

    return isUp ? 'asc' : isDown ? 'desc' : null;
  };

  const doSort = (columnIndex) => {
    const columnSortFields = DEFAULT_SORT_FIELDS[columnIndex];
    const column = extractSortFieldName(columnSortFields[0]);

    if (equals(columnSortFields, sortFields)) {
      if (sortFields[0] !== column) {
        sortRequests([column]);
      } else {
        sortRequests([`-${column}`]);
      }
    } else {
      sortRequests(columnSortFields);
    }
  };

  const bodyFragment = () => {
    if (!isNilOrEmpty(results)) {
      return (
        <>
          {results.map((waiverRequest) => (
            <DashboardWaiverRequestsTableRow {...{ stateGo, waiverRequest, page }} key={waiverRequest.id} />
          ))}
        </>
      );
    }
    return null;
  };

  useEffect(() => {
    if (isStandaloneFirewall || (!filterLoading && !needsAcknowledgement)) {
      loadWaiverRequests();
    }
  }, [filterLoading, needsAcknowledgement]);

  if (!hasWaiverRequestWorkflow) {
    return (
      <EnterpriseFullWidthBanner
        title="Waiver Requests"
        description="Enable your team to request waivers for policy violations with structured workflows and approval processes."
      />
    );
  }

  return (
    <>
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <div className="nx-table-container">
        <NxTable id="iq-dashboard-waiver-requests-table" className="nx-table--fixed-layout">
          <NxTable.Head>
            <NxTable.Row className="iq-dashboard-waiver-requests-headers">
              <NxTable.Cell
                className="iq-size-controlled-cell"
                onClick={() => doSort(0)}
                sortDir={getColumnDirection(0)}
                isSortable
              >
                Threat
              </NxTable.Cell>
              <NxTable.Cell
                className="iq-waiver-request-date-header"
                onClick={() => doSort(1)}
                sortDir={getColumnDirection(1)}
                isSortable
              >
                Date Requested
              </NxTable.Cell>
              <NxTable.Cell
                className="iq-waiver-requester-header"
                onClick={() => doSort(2)}
                sortDir={getColumnDirection(2)}
                isSortable
              >
                Requester
              </NxTable.Cell>
              <NxTable.Cell onClick={() => doSort(3)} sortDir={getColumnDirection(3)} isSortable>
                Policy
              </NxTable.Cell>
              <NxTable.Cell onClick={() => doSort(4)} sortDir={getColumnDirection(4)} isSortable>
                Scope
              </NxTable.Cell>
              <NxTable.Cell>Components</NxTable.Cell>
              <NxTable.Cell
                className="iq-waiver-request-status-header"
                onClick={() => doSort(5)}
                sortDir={getColumnDirection(5)}
                isSortable
              >
                Status
              </NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            className="iq-dashboard-waivers-entries"
            isLoading={isLoading}
            emptyMessage={emptyMessage}
            error={Messages.getHttpErrorMessage(error)}
            retryHandler={loadWaiverRequests}
          >
            {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : bodyFragment()}
          </NxTable.Body>
        </NxTable>

        {!isLoading &&
          (currentPage === null || (currentPage === 0 && !hasNextPage) ? null : (
            <NxTableContainer.Footer>
              <NxIndeterminatePagination
                onPrevPageSelect={dispatchPreviousPage}
                onNextPageSelect={dispatchNextPage}
                isFirstPage={currentPage === 0}
                isLastPage={!hasNextPage}
              />
            </NxTableContainer.Footer>
          ))}
      </div>
    </>
  );
}
