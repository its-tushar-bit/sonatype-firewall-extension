/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import DashboardComponents from './DashboardComponents';
import { loadResults, sortResults } from '../dashboardResultsActions';
import { stateGo } from '../../../reduxUiRouter/routerActions';

function mapStateToProps({ dashboard, dashboardFilter }) {
  const { loading, needsAcknowledgement, filtersAreDirty } = dashboardFilter;

  return {
    results: dashboard,
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty
  };
}

const mapDispatchToProps = {
  loadResults,
  sortResults,
  stateGo
};

const DashboardComponentsContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardComponents);
export default DashboardComponentsContainer;
