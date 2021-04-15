/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { loadResults, sortResults } from '../dashboardResultsActions';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import DashboardApplications, { APPLICATIONS_RESULTS_TYPE } from './DashboardApplications';

function mapStateToProps({ dashboard, dashboardFilter }) {
  const { loading, needsAcknowledgement, filtersAreDirty } = dashboardFilter;

  return {
    applicationResults: dashboard[APPLICATIONS_RESULTS_TYPE],
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty,
  };
}

const mapDispatchToProps = {
  loadResults,
  sortResults,
  stateGo,
};

const DashboardApplicationsContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardApplications);
export default DashboardApplicationsContainer;
