/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { selectDashboardFilter, selectWaiversResults } from '../../dashboardSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectWaiverReasonsState } from 'MainRoot/waivers/requestWaiverSelectors';
import {
  loadWaiverResults,
  sortWaiversResults,
  setNextWaiversPage,
  setPreviousWaiversPage,
} from '../dashboardResultsActions';
import { NxTable, NxIndeterminatePagination, NxTableContainer } from '@sonatype/react-shared-components';
import DashboardMask from '../dashboardMask/DashboardMask';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { equals } from 'ramda';

import { Messages } from 'MainRoot/util/CommonServices';
import DashboardWaiversTableRow from './DashboardWaiversTableRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { extractSortFieldName } from 'MainRoot/util/sortUtils';

const DEFAULT_SORT_FIELDS = [['-threatLevel'], ['createTime'], ['expiryTime'], ['policyName'], ['scope']];

export default function DashboardWaiversTable() {
  const dispatch = useDispatch();
  const loadWaivers = () => dispatch(loadWaiverResults());
  const sortWaivers = (sortFields) => {
    dispatch(sortWaiversResults(sortFields));
  };
  const dispatchNextPage = () => dispatch(setNextWaiversPage());
  const dispatchPreviousPage = () => dispatch(setPreviousWaiversPage());
  const waivers = useSelector(selectWaiversResults);
  const waiverReasonsState = useSelector(selectWaiverReasonsState);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const {
    loading: filterLoading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter: { maxDaysOld },
  } = useSelector(selectDashboardFilter);
  const { results, hasNextPage, sortFields, error, hasMultiplePages, page } = waivers;

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
        sortWaivers([column]);
      } else {
        sortWaivers([`-${column}`]);
      }
    } else {
      sortWaivers(columnSortFields);
    }
  };

  const bodyFragment = () => {
    if (!isNilOrEmpty(results)) {
      return (
        <>
          {results.map((waiver) => (
            <DashboardWaiversTableRow {...{ stateGo, waiver, page }} key={waiver.id} />
          ))}
        </>
      );
    }
    return null;
  };

  useEffect(() => {
    if (isStandaloneFirewall || (!filterLoading && !needsAcknowledgement)) {
      loadWaivers();
    }
  }, [filterLoading, needsAcknowledgement]);

  return (
    <>
      {filtersAreDirty && !needsAcknowledgement && !isLoading && <DashboardMask />}
      <div className="nx-table-container">
        <NxTable className="nx-table--fixed-layout">
          <NxTable.Head>
            <NxTable.Row className="iq-dashboard-waivers-headers">
              <NxTable.Cell
                className="iq-size-controlled-cell"
                onClick={() => doSort(0)}
                sortDir={getColumnDirection(0)}
                isSortable
              >
                Threat
              </NxTable.Cell>
              <NxTable.Cell
                className="iq-waiver-date-header"
                onClick={() => doSort(1)}
                sortDir={getColumnDirection(1)}
                isSortable
              >
                Date Created
              </NxTable.Cell>
              <NxTable.Cell
                className="iq-waiver-date-header"
                onClick={() => doSort(2)}
                sortDir={getColumnDirection(2)}
                isSortable
              >
                Expiration
              </NxTable.Cell>
              <NxTable.Cell onClick={() => doSort(3)} sortDir={getColumnDirection(3)} isSortable>
                Policy
              </NxTable.Cell>
              <NxTable.Cell onClick={() => doSort(4)} sortDir={getColumnDirection(4)} isSortable>
                Scope
              </NxTable.Cell>
              <NxTable.Cell>Components</NxTable.Cell>
              <NxTable.Cell className="iq-upgrade-header">Upgrade</NxTable.Cell>
              <NxTable.Cell chevron />
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body
            className="iq-dashboard-waivers-entries"
            isLoading={isLoading}
            emptyMessage={emptyMessage}
            error={Messages.getHttpErrorMessage(error)}
            retryHandler={loadWaivers}
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
