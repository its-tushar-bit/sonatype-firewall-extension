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
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import {
  selectIsAutoWaiversEnabled,
  selectIsDashboardSupported,
  selectIsDashboardWaiversSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

const mapStateToProps = (state) => {
  const isAutoWaiversEnabled = selectIsAutoWaiversEnabled(state);
  const { manageFilters, dashboardFilter, dashboard, waivers } = state;
  const prevStateName = state.router.prevState.name;
  return {
    dashboard,
    exportTitle: selectExportTitle(state),
    exportRequestData: selectExportRequestData(state),
    exportUrl: selectExportUrl(state, isAutoWaiversEnabled),
    ...pick(['appliedFilterName', 'showDirtyAsterisk'], manageFilters),
    filterSidebarOpen: dashboardFilter.filterSidebarOpen,
    filters: dashboardFilter.appliedFilter,
    filterLoading: dashboardFilter.loading || waivers.waiverReasons.loading,
    loadFilterError: dashboardFilter.loadError || waivers.waiverReasons.loadError,
    isDashboardEnabled: selectIsDashboardSupported(state),
    isWaiversTabEnabled: selectIsDashboardWaiversSupported(state),
    prevStateName,
  };
};

const mapDispatchToProps = {
  ...manageFiltersActions,
  ...dashboardFilterActions,
  stateGo,
};

export default connect(mapStateToProps, mapDispatchToProps)(DashboardHeader);
