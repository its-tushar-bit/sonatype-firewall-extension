/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import AuditLogTable, { auditRecordsPropTypes } from './AuditLogTable';
import LoadWrapper from '../../react/LoadWrapper';

export default function AuditLog({ auditRecords, isLoading, error, loadAuditLogForComponent }) {
  useEffect(() => {
    loadAuditLogForComponent();
  }, []);

  return (
    <LoadWrapper loading={isLoading} error={error} retryHandler={loadAuditLogForComponent}>
      {auditRecords && <AuditLogTable auditRecords={auditRecords} />}
    </LoadWrapper>
  );
}

AuditLog.propTypes = {
  auditRecords: PropTypes.arrayOf(auditRecordsPropTypes),
  isLoading: PropTypes.bool.isRequired,
  error: PropTypes.string,
  loadAuditLogForComponent: PropTypes.func.isRequired,
};
