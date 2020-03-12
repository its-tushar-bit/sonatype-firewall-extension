/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { connect } from 'react-redux';
import { pick } from 'ramda';
import * as PropTypes from 'prop-types';
import DashboardFilter from './DashboardFilter';

export default function DashboardFilterContainer({ manageFiltersActions, dashboardFilterActions }) {
  const mapDispatchToProps = { ...manageFiltersActions, ...dashboardFilterActions };

  const mapStateToProps = ({ manageFilters, dashboardFilter}) => {
    return {
      ...dashboardFilter,
      ...pick(['appliedFilterName', 'showDirtyAsterisk'], manageFilters)
    };
  };

  const ConnectedComponent = connect(mapStateToProps, mapDispatchToProps)(DashboardFilter);

  return <ConnectedComponent />;
}

DashboardFilterContainer.propTypes = {
  manageFiltersActions: PropTypes.object.isRequired,
  dashboardFilterActions: PropTypes.object.isRequired
};
