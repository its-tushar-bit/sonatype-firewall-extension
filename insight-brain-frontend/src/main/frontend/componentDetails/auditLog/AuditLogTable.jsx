/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxTable, NxTableBody, NxTableHead, NxTableRow, NxTableCell } from '@sonatype/react-shared-components';
import { equals } from 'ramda';

import { formatDate } from '../../util/dateUtils';
import { extractSortFieldName } from '../../util/sortUtils';

const NO_LOG_MESSAGE = 'No changes were found for this component.';
export const AUDIT_DATE_FORMAT = 'MMM DD, YYYY HH:mm:ss a';

const SORT_FIELDS = ['-time', 'user', 'action', 'detail', 'comment'];

export default function AuditLogTable({
  auditRecords,
  isLoading,
  error,
  loadAuditLogForComponent,
  appliedSort,
  sortAuditLog,
}) {
  useEffect(() => {
    loadAuditLogForComponent();
  }, []);

  const sortedColumn = extractSortFieldName(appliedSort),
    isSortReversed = appliedSort && appliedSort.includes('-');

  const formatAuditDate = (date) => {
    return formatDate(date, AUDIT_DATE_FORMAT);
  };

  const getColumnDirection = (index) => {
    if (!auditRecords || !auditRecords.length || error) {
      return null;
    }

    const columnField = SORT_FIELDS[index],
      columnName = extractSortFieldName(columnField),
      isCurrentColumnSorted = columnName === sortedColumn,
      isUp = isCurrentColumnSorted && !isSortReversed,
      isDown = isCurrentColumnSorted && isSortReversed;

    return isUp ? 'asc' : isDown ? 'desc' : null;
  };

  const doSort = (index) => {
    if (!auditRecords || !auditRecords.length) {
      return;
    }
    const chosenColumn = SORT_FIELDS[index];
    if (equals(chosenColumn, appliedSort)) {
      if (isSortReversed) {
        sortAuditLog(chosenColumn.substring(1));
      } else {
        sortAuditLog(`-${chosenColumn}`);
      }
    } else {
      sortAuditLog(chosenColumn);
    }
  };

  return (
    <NxTable id="audit-log-table" className="iq-audit-log-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell onClick={() => doSort(0)} sortDir={getColumnDirection(0)} isSortable>
            Date
          </NxTableCell>
          <NxTableCell onClick={() => doSort(1)} sortDir={getColumnDirection(1)} isSortable>
            User
          </NxTableCell>
          <NxTableCell onClick={() => doSort(2)} sortDir={getColumnDirection(2)} isSortable>
            Action
          </NxTableCell>
          <NxTableCell onClick={() => doSort(3)} sortDir={getColumnDirection(3)} isSortable>
            Detail
          </NxTableCell>
          <NxTableCell onClick={() => doSort(4)} sortDir={getColumnDirection(4)} isSortable>
            Comment
          </NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody
        emptyMessage={NO_LOG_MESSAGE}
        error={error}
        isLoading={isLoading}
        retryHandler={loadAuditLogForComponent}
      >
        {auditRecords &&
          auditRecords.map((record, index) => (
            <NxTableRow key={index}>
              <NxTableCell>{formatAuditDate(record.time)}</NxTableCell>
              <NxTableCell>{record.user}</NxTableCell>
              <NxTableCell>{record.action}</NxTableCell>
              <NxTableCell>{record.detail}</NxTableCell>
              <NxTableCell className="iq-audit-log-table__comment">{record.comment}</NxTableCell>
            </NxTableRow>
          ))}
      </NxTableBody>
    </NxTable>
  );
}

export const auditRecordsPropTypes = PropTypes.shape({
  time: PropTypes.number.isRequired,
  user: PropTypes.string.isRequired,
  action: PropTypes.string.isRequired,
  detail: PropTypes.string,
  comment: PropTypes.string,
});

AuditLogTable.propTypes = {
  auditRecords: PropTypes.arrayOf(auditRecordsPropTypes),
  isLoading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  appliedSort: PropTypes.string,
  loadAuditLogForComponent: PropTypes.func.isRequired,
  sortAuditLog: PropTypes.func.isRequired,
};
