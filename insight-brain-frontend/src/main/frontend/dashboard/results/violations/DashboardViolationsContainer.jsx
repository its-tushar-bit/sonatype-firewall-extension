/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import DashboardViolations from './DashboardViolations';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  loadViolationResults,
  sortViolationResults,
  setNextViolationsPage,
  setPreviousViolationsPage,
} from '../dashboardResultsActions';
import { selectViolationResults } from 'MainRoot/dashboard/dashboardSelectors';

function mapStateToProps(state) {
  const { dashboardFilter } = state;
  const { loading, needsAcknowledgement, filtersAreDirty, appliedFilter } = dashboardFilter;
  const prevStateName = state.router.prevState.name;

  return {
    violations: selectViolationResults(state),
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty,
    appliedFilter,
    prevStateName,
  };
}

const mapDispatchToProps = {
  sortViolations: sortViolationResults,
  loadViolationResults,
  stateGo,
  setNextViolationsPage,
  setPreviousViolationsPage,
};

const DashboardViolationsContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardViolations);
export default DashboardViolationsContainer;
