/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import {
  loadApplicationResults,
  sortApplicationResults,
  setNextApplicationsPage,
  setPreviousApplicationsPage,
} from '../dashboardResultsActions';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import DashboardApplications from './DashboardApplications';
import { selectApplicationResults } from 'MainRoot/dashboard/dashboardSelectors';

function mapStateToProps(state) {
  const { dashboardFilter } = state;
  const { loading, needsAcknowledgement, filtersAreDirty } = dashboardFilter;

  return {
    applicationResults: selectApplicationResults(state),
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty,
  };
}

const mapDispatchToProps = {
  sortApplications: sortApplicationResults,
  loadApplicationResults,
  stateGo,
  setNextApplicationsPage,
  setPreviousApplicationsPage,
};

const DashboardApplicationsContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardApplications);
export default DashboardApplicationsContainer;
