/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import AuditLog from './AuditLog';
import { loadAuditLogForComponent } from './auditLogActions';

function mapStateToProps({ auditLog }) {
  return { ...auditLog };
}

const mapDispatchToProps = { loadAuditLogForComponent };

const AuditLogContainer = connect(mapStateToProps, mapDispatchToProps)(AuditLog);
export default AuditLogContainer;
