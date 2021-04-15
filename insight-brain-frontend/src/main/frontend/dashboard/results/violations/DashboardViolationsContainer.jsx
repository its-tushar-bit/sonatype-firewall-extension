/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import DashboardViolations from './DashboardViolations';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { loadResults, sortResults } from '../dashboardResultsActions';

function mapStateToProps({ dashboard, dashboardFilter }) {
  const {
    loading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter,
  } = dashboardFilter;

  return {
    results: dashboard,
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter,
  };
}

const mapDispatchToProps = {
  stateGo,
  loadResults,
  sortResults,
};

const DashboardViolationsContainer = connect(
  mapStateToProps,
  mapDispatchToProps
)(DashboardViolations);
export default DashboardViolationsContainer;
