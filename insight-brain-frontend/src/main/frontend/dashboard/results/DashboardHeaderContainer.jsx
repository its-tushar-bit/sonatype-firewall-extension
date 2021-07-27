/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import DashboardHeader from './DashboardHeader';
import * as manageFiltersActions from '../filter/manageFiltersActions';
import * as dashboardFilterActions from '../filter/dashboardFilterActions';
import { selectExportTitle, selectExportRequestData, selectExportUrl } from '../dashboardSelectors';
import { stateGo } from '../../reduxUiRouter/routerActions';

const mapStateToProps = (state) => {
  const { manageFilters, dashboardFilter, dashboard } = state;
  return {
    dashboard,
    exportTitle: selectExportTitle(state),
    exportRequestData: selectExportRequestData(state),
    exportUrl: selectExportUrl(state),
    ...pick(['appliedFilterName', 'showDirtyAsterisk'], manageFilters),
    filterSidebarOpen: dashboardFilter.filterSidebarOpen,
    filters: dashboardFilter.appliedFilter,
    filterLoading: dashboardFilter.loading,
    loadFilterError: dashboardFilter.loadError,
  };
};

const mapDispatchToProps = {
  ...manageFiltersActions,
  ...dashboardFilterActions,
  stateGo,
};

export default connect(mapStateToProps, mapDispatchToProps)(DashboardHeader);
