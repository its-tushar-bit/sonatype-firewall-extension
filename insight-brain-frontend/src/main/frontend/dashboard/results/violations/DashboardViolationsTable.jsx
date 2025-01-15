/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxIndeterminatePagination,
  NxTableContainer,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import { equals } from 'ramda';

import DashboardViolationsTableRow, { violationPropTypes } from './DashboardViolationsTableRow';
import { extractSortFieldName } from '../../../util/sortUtils';
import { Messages } from '../../../utilAngular/CommonServices';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';
import { isNilOrEmpty } from '../../../util/jsUtil';

const DEFAULT_SORT_FIELDS = [
  ['-threatLevel', '-firstOccurrenceTime'],
  ['policyName', '-firstOccurrenceTime'],
  ['applicationName', '-threatLevel'],
  ['-firstOccurrenceTime', '-threatLevel'],
];

export default function DashboardViolationsTable(props) {
  const {
      reload,
      sortViolations,
      stateGo,
      maxDaysOld,
      needsAcknowledgement,
      violations: { results, hasNextPage, sortFields, error, hasMultiplePages, page },
      setNextViolationsPage,
      setPreviousViolationsPage,
    } = props,
    currentPage = hasMultiplePages ? page : null,
    isLoading = !error && !results && !needsAcknowledgement,
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

    if (equals(columnSortFields, sortFields)) {
      const column = extractSortFieldName(columnSortFields[0]);
      if (sortFields[0] !== column) {
        sortViolations([column, sortFields[1]]);
      } else {
        sortViolations([`-${column}`, sortFields[1]]);
      }
    } else {
      sortViolations(columnSortFields);
    }
  };

  const bodyFragment = () => {
    if (!isNilOrEmpty(results)) {
      return (
        <Fragment>
          {results.map((violation) => (
            <DashboardViolationsTableRow {...{ stateGo, violation, page }} key={violation.policyViolationId} />
          ))}
        </Fragment>
      );
    }
    return null;
  };

  return (
    <div className="nx-table-container">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow className="iq-dashboard-violation-headers">
            <NxTableCell
              className="iq-size-controlled-cell"
              onClick={() => doSort(0)}
              sortDir={getColumnDirection(0)}
              isSortable
            >
              Threat
            </NxTableCell>
            <NxTableCell onClick={() => doSort(1)} sortDir={getColumnDirection(1)} isSortable>
              Policy
            </NxTableCell>
            <NxTableCell onClick={() => doSort(2)} sortDir={getColumnDirection(2)} isSortable>
              Application
            </NxTableCell>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell
              className="iq-size-controlled-cell"
              onClick={() => doSort(3)}
              sortDir={getColumnDirection(3, true)}
              isSortable
            >
              Age
            </NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          className="iq-dashboard-violation-entries"
          isLoading={isLoading}
          emptyMessage={emptyMessage}
          error={Messages.getHttpErrorMessage(error)}
          retryHandler={reload}
        >
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : bodyFragment()}
        </NxTableBody>
      </NxTable>

      {!isLoading &&
        (currentPage === null || (currentPage === 0 && !hasNextPage) ? null : (
          <NxTableContainer.Footer>
            <NxIndeterminatePagination
              onPrevPageSelect={setPreviousViolationsPage}
              onNextPageSelect={setNextViolationsPage}
              isFirstPage={currentPage === 0}
              isLastPage={!hasNextPage}
            />
          </NxTableContainer.Footer>
        ))}
    </div>
  );
}

DashboardViolationsTable.propTypes = {
  reload: PropTypes.func.isRequired,
  sortViolations: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  maxDaysOld: PropTypes.number,
  needsAcknowledgement: PropTypes.bool.isRequired,
  violations: PropTypes.shape({
    results: PropTypes.arrayOf(violationPropTypes),
    hasNextPage: PropTypes.bool,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
    hasMultiplePages: PropTypes.bool,
    page: PropTypes.number,
  }),
  setNextViolationsPage: PropTypes.func.isRequired,
  setPreviousViolationsPage: PropTypes.func.isRequired,
};
