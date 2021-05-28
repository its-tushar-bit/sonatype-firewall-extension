/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTable, NxTableBody, NxTableHead, NxTableRow, NxTableCell } from '@sonatype/react-shared-components';

import { formatDate } from '../../util/dateUtils';

const NO_LOG_MESSAGE = 'No changes were found for this component.';
export const AUDIT_DATE_FORMAT = 'MMM DD, YYYY HH:mm:ss a';

export default function AuditLogTable({ auditRecords }) {
  const formatAuditDate = (date) => {
    return formatDate(date, AUDIT_DATE_FORMAT);
  };

  return (
    <NxTable id="audit-log-table" className="iq-audit-log-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>Date</NxTableCell>
          <NxTableCell>User</NxTableCell>
          <NxTableCell>Action</NxTableCell>
          <NxTableCell>Detail</NxTableCell>
          <NxTableCell>Comment</NxTableCell>
        </NxTableRow>
      </NxTableHead>
      <NxTableBody emptyMessage={NO_LOG_MESSAGE}>
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
};
