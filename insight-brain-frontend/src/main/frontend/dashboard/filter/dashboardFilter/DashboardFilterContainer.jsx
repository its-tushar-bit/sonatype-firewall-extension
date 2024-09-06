/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { pick } from 'ramda';
import DashboardFilter from './DashboardFilter';
import * as manageFiltersActions from '../manageFiltersActions';
import * as dashboardFilterActions from '../dashboardFilterActions';

function mapStateToProps({
  manageFilters,
  dashboardFilter,
  orgsAndPolicies: { ownerSideNav },
  waivers: {
    waiverReasons: { data: waiverReasons, loading: waiverReasonsLoading, loadError: waiverReasonsLoadError },
  },
}) {
  return {
    ...dashboardFilter,
    ...pick(['appliedFilterName', 'showDirtyAsterisk', 'savedFilters'], manageFilters),
    ...pick(['ownersMap', 'topParentOrganizationId'], ownerSideNav),
    loading: dashboardFilter.loading || waiverReasonsLoading,
    loadError: dashboardFilter.loadError || waiverReasonsLoadError,
    waiverReasons,
  };
}

const mapDispatchToProps = {
  ...manageFiltersActions,
  ...dashboardFilterActions,
};

const DashboardFilterContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardFilter);
export default DashboardFilterContainer;
