/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTable, NxIndeterminatePagination, NxTableContainer } from '@sonatype/react-shared-components';
import { equals } from 'ramda';

import { Messages } from 'MainRoot/utilAngular/CommonServices';
import DashboardWaiversTableRow, { waiverPropTypes } from './DashboardWaiversTableRow';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { extractSortFieldName } from 'MainRoot/util/sortUtils';

const DEFAULT_SORT_FIELDS = [['-threatLevel'], ['createTime'], ['expiryTime'], ['policyName'], ['scope']];

export default function DashboardWaiversTable(props) {
  const {
    waivers: { results, hasNextPage, sortFields, error, hasMultiplePages, page },
    sortWaivers,
    dispatchNexPage,
    dispatchPreviousPage,
    stateGo,
    maxDaysOld,
    needsAcknowledgement,
    reload,
  } = props;
  const currentPage = hasMultiplePages ? page : null;
  const isLoading = !error && !results && !needsAcknowledgement,
    sortedColumn = extractSortFieldName(sortFields[0]),
    isSortReversed = sortFields[0].includes('-'),
    emptyMessage =
      'No data available ' +
      (maxDaysOld ? `in the last ${maxDaysOld} days ` : '') +
      'given the applied filters and permissions.';
  const colSpan = 6;

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
  return (
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
          retryHandler={reload}
        >
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : bodyFragment()}
        </NxTable.Body>
      </NxTable>

      {!isLoading &&
        (currentPage === null || (currentPage === 0 && !hasNextPage) ? null : (
          <NxTableContainer.Footer>
            <NxIndeterminatePagination
              onPrevPageSelect={dispatchPreviousPage}
              onNextPageSelect={dispatchNexPage}
              isFirstPage={currentPage === 0}
              isLastPage={!hasNextPage}
            />
          </NxTableContainer.Footer>
        ))}
    </div>
  );
}

DashboardWaiversTable.propTypes = {
  reload: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  sortWaivers: PropTypes.func.isRequired,
  dispatchNexPage: PropTypes.func.isRequired,
  dispatchPreviousPage: PropTypes.func.isRequired,
  maxDaysOld: PropTypes.number,
  needsAcknowledgement: PropTypes.bool.isRequired,
  waivers: PropTypes.shape({
    results: PropTypes.arrayOf(waiverPropTypes),
    hasNextPage: PropTypes.bool,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
    hasMultiplePages: PropTypes.bool,
    page: PropTypes.number,
  }),
};
