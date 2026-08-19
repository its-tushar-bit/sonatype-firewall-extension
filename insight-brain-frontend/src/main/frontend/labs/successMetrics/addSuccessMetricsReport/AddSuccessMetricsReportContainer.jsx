/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';
import { connect } from 'react-redux';
import AddSuccessMetricsReport from './AddSuccessMetricsReport';
import * as addSuccessMetricsReportActions from './addSuccessMetricsReportActions';

function mapStateProp(
  { successMetrics: { addSuccessMetricsReport }, orgsAndPolicies: { ownerSideNav } },
  { close, dismiss, reports }
) {
  return {
    ...addSuccessMetricsReport,
    ...pick(['ownersMap', 'topParentOrganizationId'], ownerSideNav),
    close,
    dismiss,
    reports,
  };
}

export default connect(mapStateProp, addSuccessMetricsReportActions)(AddSuccessMetricsReport);
