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

function mapStateToProps({ manageFilters, dashboardFilter }) {
  return {
    ...dashboardFilter,
    ...pick(['appliedFilterName', 'showDirtyAsterisk', 'savedFilters'], manageFilters),
  };
}

const mapDispatchToProps = {
  ...manageFiltersActions,
  ...dashboardFilterActions,
};

const DashboardFilterContainer = connect(mapStateToProps, mapDispatchToProps)(DashboardFilter);
export default DashboardFilterContainer;
