/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxInfoAlert,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import { equals, take } from 'ramda';

import DashboardViolationsTableRow, { violationPropTypes } from './DashboardViolationsTableRow';
import { MAX_RESULTS } from '../../services/dashboard.data.service';
import { extractSortFieldName } from '../../../util/sortUtils';
import { Messages } from '../../../util/CommonServices';

const DEFAULT_SORT_FIELDS = [
  ['-threatLevel', '-firstOccurrenceTime'],
  ['policyName', '-firstOccurrenceTime'],
  ['applicationName', '-threatLevel'],
  ['derivedComponentName', '-threatLevel'],
  ['-firstOccurrenceTime', '-threatLevel']
];

export default function DashboardViolationsTable(props) {
  const {
        reload,
        sortViolations,
        stateGo,
        maxDaysOld,
        needsAcknowledgement,
        violations: {
          results,
          numResults,
          sortFields,
          error
        }
      } = props,
      isLoading = !error && !results && !needsAcknowledgement,
      violationsToDisplay = results && take(MAX_RESULTS, results),
      sortedColumn = extractSortFieldName(sortFields[0]),
      isSortReversed = sortFields[0].includes('-'),
      emptyMessage = 'No data available ' + (maxDaysOld ? `in the last ${maxDaysOld} days ` : '') +
          'given the applied filters and permissions.';

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
      }
      else {
        sortViolations([`-${column}`, sortFields[1]]);
      }
    }
    else {
      sortViolations(columnSortFields);
    }
  };

  const maxResultsInfoRow = () => (
    <NxTableRow>
      <NxTableCell colSpan={6} metaInfo>
        <span id="max-results-shown">First { MAX_RESULTS } results shown</span>
      </NxTableCell>
    </NxTableRow>
  );

  const needsAcknowledgementInfoRow = () => (
    <NxTableRow>
      <NxTableCell colSpan={6} metaInfo>
        <NxInfoAlert id="needs-acknowledgement">
          {'Select your filter criteria on the left, and click \'apply\' to see results.'}
        </NxInfoAlert>
      </NxTableCell>
    </NxTableRow>
  );

  const bodyFragment = () => {
    if (violationsToDisplay && violationsToDisplay.length) {
      return (
        <Fragment>
          { violationsToDisplay.map(violation =>
            <DashboardViolationsTableRow { ...({ stateGo, violation }) } key={violation.policyViolationId} />
          )}
          { numResults > MAX_RESULTS && maxResultsInfoRow() }
        </Fragment>
      );
    }
    return null;
  };

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow className="iq-dashboard-violation-headers">
            <NxTableCell className="iq-size-controlled-cell"
                         onClick={ () => doSort(0) }
                         sortDir={ getColumnDirection(0) }
                         isSortable>Threat</NxTableCell>
            <NxTableCell onClick={ () => doSort(1) }
                         sortDir={ getColumnDirection(1) }
                         isSortable>Policy</NxTableCell>
            <NxTableCell onClick={ () => doSort(2) }
                         sortDir={ getColumnDirection(2) }
                         isSortable>Application</NxTableCell>
            <NxTableCell onClick={ () => doSort(3) }
                         sortDir={ getColumnDirection(3) }
                         isSortable>Component</NxTableCell>
            <NxTableCell className="iq-size-controlled-cell"
                         onClick={ () => doSort(4) }
                         sortDir={ getColumnDirection(4, true) }
                         isSortable>Age</NxTableCell>
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody className="iq-dashboard-violation-entries"
                     isLoading={ isLoading }
                     emptyMessage={ emptyMessage }
                     error={ Messages.getHttpErrorMessage(error) }
                     retryHandler = { reload }>
          {
            needsAcknowledgement ? needsAcknowledgementInfoRow() : bodyFragment()
          }
        </NxTableBody>
      </NxTable>
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
    numResults: PropTypes.number,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object])
  })
};
