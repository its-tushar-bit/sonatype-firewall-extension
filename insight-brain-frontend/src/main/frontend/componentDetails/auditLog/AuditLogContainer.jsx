/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import AuditLog from './AuditLog';
import { loadAuditLogForComponent, sortAuditLog } from './auditLogActions';
import { selectComponentDetailsLoading, selectComponentDetailsLoadErrors } from '../componentDetailsSelectors';
import { actions as componentDetailsActions } from '../componentDetailsSlice';

function mapStateToProps(state) {
  const { auditLog } = state;
  const isLoadingComponentDetails = selectComponentDetailsLoading(state);
  const componentDetailsLoadError = selectComponentDetailsLoadErrors(state);
  return {
    isLoadingComponentDetails,
    componentDetailsLoadError,
    ...auditLog,
  };
}

const { loadComponentDetails } = componentDetailsActions;

const mapDispatchToProps = {
  loadAuditLogForComponent,
  sortAuditLog,
  loadComponentDetails,
};

const AuditLogContainer = connect(mapStateToProps, mapDispatchToProps)(AuditLog);
export default AuditLogContainer;
