/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import LoadWrapper from '../../react/LoadWrapper';
import ExportButton, { exportButtonPropTypes } from './dashboardSummary/ExportButton';
import DashboardTabs, { dashboardTabsPropTypes } from './dashboardTabs/DashboardTabs';
import { DEFAULT_FILTER_NAME } from '../filter/defaultFilter';
import { NxButton, NxFontAwesomeIcon, NxInfoAlert } from '@sonatype/react-shared-components';
import { faFilter } from '@fortawesome/pro-solid-svg-icons';

export default function DashboardResults(props) {
  const {
    dashboard,
    filterSidebarOpen,
    toggleFilterSidebar,
    appliedFilterName,
    filterLoading,
    loadFilterError,
    exportTitle,
    exportRequestData,
    exportUrl,
    showDirtyAsterisk,
    loadFilter,
    stateGo,
    isDashboardEnabled,
    isWaiversTabEnabled,
    prevStateName,
  } = props;

  useEffect(() => {
    // if it comes back from violation details and there are results, don't reload
    if (
      !(
        dashboard.currentTab === 'violations' &&
        dashboard.violations.results &&
        prevStateName === 'sidebarView.violation'
      )
    ) {
      loadFilter();
    }
  }, []);

  return (
    <div className="dashboard-summary">
      <div className="nx-page-title">
        <h1 className="nx-h1">Results</h1>
        {isDashboardEnabled && (
          <div className="nx-btn-bar">
            <ExportButton exportTitle={exportTitle} exportRequestData={exportRequestData} exportUrl={exportUrl} />
            <NxButton variant="secondary" id="filter-toggle" onClick={() => toggleFilterSidebar(!filterSidebarOpen)}>
              <NxFontAwesomeIcon icon={faFilter} />
              Filter: {showDirtyAsterisk && <span id="filter-toggle-dirty-asterisk">*</span>}
              {appliedFilterName || DEFAULT_FILTER_NAME}
            </NxButton>
          </div>
        )}
      </div>
      {!isDashboardEnabled ? (
        <NxInfoAlert>The Dashboard feature has been disabled by your administrator.</NxInfoAlert>
      ) : (
        <LoadWrapper loading={filterLoading} error={loadFilterError} retryHandler={loadFilter}>
          <DashboardTabs
            stateGo={stateGo}
            {...dashboard}
            isDashboardEnabled={isDashboardEnabled}
            isWaiversTabEnabled={isWaiversTabEnabled}
          />
        </LoadWrapper>
      )}
    </div>
  );
}

DashboardResults.propTypes = {
  dashboard: PropTypes.shape({ ...dashboardTabsPropTypes }),
  stateGo: PropTypes.func.isRequired,
  ...exportButtonPropTypes,
  appliedFilterName: PropTypes.string,
  showDirtyAsterisk: PropTypes.bool.isRequired,
  filterSidebarOpen: PropTypes.bool.isRequired,
  filters: PropTypes.object,
  filterLoading: PropTypes.bool.isRequired,
  loadFilterError: PropTypes.string,
  isDashboardEnabled: PropTypes.bool.isRequired,
  isWaiversTabEnabled: PropTypes.bool.isRequired,
  prevStateName: PropTypes.string,
};
