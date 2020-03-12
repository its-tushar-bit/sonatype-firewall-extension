/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxErrorAlert } from '@sonatype/react-shared-components';

export default function DashboardFilterHeader(props) {
  const {
    appliedFilterName,
    showDirtyAsterisk,
    loadErrorFilterName
  } = props;

  return (
    <div className="dashboard-filter-header">
      <div id="manage-filters-dropdown">
        { /* Placeholder for manage filter dropdown */ }
      </div>

      <h1 className="dashboard-filter-header-title">
        Filter
      </h1>
      <div className="dashboard-filter-name-container">
        {
          appliedFilterName &&
          <div className="dashboard-filter-name">
            { appliedFilterName }
            { showDirtyAsterisk && <span className="dashboard-filter-dirty-asterisk">*</span> }
          </div>
        }
      </div>
      {
        loadErrorFilterName &&
        <NxErrorAlert>Failed to load {loadErrorFilterName}</NxErrorAlert>
      }
    </div>
  );
}

DashboardFilterHeader.propTypes = {
  appliedFilterName: PropTypes.string,
  showDirtyAsterisk: PropTypes.bool,
  loadErrorFilterName: PropTypes.string
};
