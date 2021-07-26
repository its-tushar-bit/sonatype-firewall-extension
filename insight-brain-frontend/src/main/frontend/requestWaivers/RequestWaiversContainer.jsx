/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import RequestWaivers from './RequestWaivers';
import { loadReport } from '../applicationReport/applicationReportActions';
import { selectPolicyViolation, selectIsLoading } from '../applicationReport/applicationReportSelectors';
import { selectPolicyViolationError } from './requestWaiversSelectors';

function mapStateToProps(state) {
  return {
    loadError: selectPolicyViolationError(state),
    isLoading: selectIsLoading(state),
    policyViolation: selectPolicyViolation(state),
  };
}

const mapDispatchToProps = {
  loadComponentDetails: () => loadReport(true),
};

export default connect(mapStateToProps, mapDispatchToProps)(RequestWaivers);
