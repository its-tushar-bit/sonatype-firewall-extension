/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';

import AuditLogTable from './AuditLogTable';

export default function AuditLog(props) {
  const { loadAuditLogForComponent } = props;

  useEffect(() => {
    loadAuditLogForComponent();
  }, []);

  return <AuditLogTable {...props} />;
}

AuditLog.propTypes = AuditLogTable.propTypes;
