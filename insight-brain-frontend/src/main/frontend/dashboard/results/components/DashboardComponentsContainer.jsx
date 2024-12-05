/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import DashboardComponents from './DashboardComponents';
import {
  loadComponentResults,
  setNextComponentsPage,
  setPreviousComponentsPage,
  sortComponentResults,
} from '../dashboardResultsActions';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { selectComponentResults } from 'MainRoot/dashboard/dashboardSelectors';

function mapStateToProps(state) {
  const { dashboardFilter } = state;
  const { loading, needsAcknowledgement, filtersAreDirty } = dashboardFilter;

  return {
    componentResults: selectComponentResults(state),
    filterLoading: loading,
    needsAcknowledgement,
    filtersAreDirty,
  };
}

const mapDispatchToProps = {
  sortComponents: sortComponentResults,
  loadComponentResults,
  stateGo,
  setNextComponentsPage,
  setPreviousComponentsPage,
};

const DashboardComponentsContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardComponents);
export default DashboardComponentsContainer;
