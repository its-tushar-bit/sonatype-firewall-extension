/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect, useState, useMemo } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
  NxErrorAlert,
  NxIndeterminatePagination,
} from '@sonatype/react-shared-components';
import { faFilter, faDownload } from '@fortawesome/free-solid-svg-icons';

import LoadWrapper from '../../react/LoadWrapper';
import { formatDate, USER_ACTIVITY_DATE_FORMAT } from '../../util/dateUtils';
import { USER_ACTIVITY_PAGE_SIZE, calculateDateRange } from './userActivitySlice';
import UserActivityDetailsFilter from './UserActivityDetailsFilter';
import MenuBarBackButton from '../../mainHeader/MenuBar/MenuBarBackButton';
import UserActivityMask from 'MainRoot/configuration/userActivityOverview/UserActivityMask';

// Helper function to get time frame display text
const getTimeFrameText = (ageInDays) => {
  switch (ageInDays) {
    case 1:
      return 'Past 24 Hours';
    case 7:
      return 'Past 7 Days';
    case 30:
      return 'Past 30 Days';
    default:
      return 'Selected Period';
  }
};

export default function UserActivityDetails(props) {
  const {
    username,
    appliedAge = 30,
    activities = [],
    loading = false,
    loadError = null,
    pagination = { limit: USER_ACTIVITY_PAGE_SIZE, offset: 0, hasMore: false },
    filterDrawerOpen = false,
    filtersAreDirty = false,
    selectedActivityTypes = [],
    selectedDomains = [],
    selectedErrorTypes = [],
    filterOptions = { activityTypes: [], domains: [], errorTypes: [] },
    applyFilters,
    toggleFilterDrawer,
    setSelectedActivityTypes,
    setSelectedDomains,
    setSelectedErrorTypes,
    revertFilters,
    exportUserActivityData,
    exporting = false,
    exportError = null,
    clearErrors,
  } = props;

  // State for sorting and pagination
  const [sortConfig, setSortConfig] = useState({
    key: 'timestamp',
    direction: 'desc',
  });
  const [page, setPage] = useState(0);

  // Clear errors on component unmount to prevent persistence across navigation
  useEffect(() => {
    return () => clearErrors();
  }, [clearErrors]);

  // Load data when pagination changes (but not on initial mount - that's handled by Container)
  useEffect(() => {
    if (username && !loadError && page > 0) {
      applyFilters({
        username,
        limit: USER_ACTIVITY_PAGE_SIZE,
        offset: page * USER_ACTIVITY_PAGE_SIZE,
      });
    }
  }, [page, username, loadError, applyFilters]);

  // Sort activities
  const sortedActivities = useMemo(() => {
    if (!sortConfig.key) return activities;

    return [...activities].sort((a, b) => {
      let aVal = a[sortConfig.key];
      let bVal = b[sortConfig.key];

      // Handle different data types
      if (sortConfig.key === 'timestamp') {
        aVal = new Date(aVal || 0);
        bVal = new Date(bVal || 0);
      } else {
        aVal = (aVal || '').toString().toLowerCase();
        bVal = (bVal || '').toString().toLowerCase();
      }

      if (aVal < bVal) return sortConfig.direction === 'asc' ? -1 : 1;
      if (aVal > bVal) return sortConfig.direction === 'asc' ? 1 : -1;
      return 0;
    });
  }, [activities, sortConfig]);

  // Pagination - cursor-based using offset and hasMore
  const { limit = USER_ACTIVITY_PAGE_SIZE, offset = 0, hasMore = false } = pagination;
  const currentPage = Math.floor(offset / limit);
  const canGoNext = hasMore;
  const canGoPrevious = offset > 0;

  // Event handlers
  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  const handleFilterToggle = () => {
    toggleFilterDrawer();
  };

  const handleFilterApply = () => {
    setPage(0); // Reset to first page when applying filters
    applyFilters({
      username,
      limit: USER_ACTIVITY_PAGE_SIZE,
      offset: 0,
    });
  };

  const handleFilterReset = () => {
    revertFilters();
    setPage(0);
  };

  const handlePreviousPage = () => {
    if (canGoPrevious) {
      const newPage = currentPage - 1;
      setPage(newPage);
      applyFilters({
        username,
        limit: USER_ACTIVITY_PAGE_SIZE,
        offset: newPage * USER_ACTIVITY_PAGE_SIZE,
      });
    }
  };

  const handleNextPage = () => {
    if (canGoNext) {
      const newPage = currentPage + 1;
      setPage(newPage);
      applyFilters({
        username,
        limit: USER_ACTIVITY_PAGE_SIZE,
        offset: newPage * USER_ACTIVITY_PAGE_SIZE,
      });
    }
  };

  const getSortDirection = (key) => {
    if (sortConfig.key !== key) return null;
    return sortConfig.direction;
  };

  const handleExport = () => {
    if (exportUserActivityData) {
      // Calculate date range from appliedAge (similar to other API calls)
      const dateRange = calculateDateRange(appliedAge);

      exportUserActivityData({
        username,
        ...dateRange,
        activityTypes: selectedActivityTypes?.length > 0 ? selectedActivityTypes : null,
        domains: selectedDomains?.length > 0 ? selectedDomains : null,
        errorTypes: selectedErrorTypes?.length > 0 ? selectedErrorTypes : null,
      });
    }
  };

  const timeFrameText = getTimeFrameText(appliedAge);

  return (
    <main className="nx-page-main">
      <MenuBarBackButton stateName="users.activity" />
      <div className="nx-page-title">
        <h1 className="nx-h1">
          {username} Activity ({timeFrameText})
        </h1>
      </div>

      <LoadWrapper
        loading={loading}
        error={loadError}
        retryHandler={() =>
          applyFilters({
            username,
            limit: USER_ACTIVITY_PAGE_SIZE,
            offset: page * USER_ACTIVITY_PAGE_SIZE,
          })
        }
      >
        <section className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile__header-main">
              <h2 className="nx-h3">Activity Details</h2>
            </div>
            <div className="nx-tile__actions">
              <NxButton
                variant="tertiary"
                onClick={handleExport}
                disabled={loading || activities.length === 0 || exporting}
              >
                <NxFontAwesomeIcon icon={faDownload} />
                <span>{exporting ? 'Exporting...' : 'Export Activity'}</span>
              </NxButton>
              <NxButton variant="tertiary" onClick={handleFilterToggle}>
                <NxFontAwesomeIcon icon={faFilter} />
                <span>Filter</span>
              </NxButton>
            </div>
          </header>

          <div className="nx-tile-content">
            {/* Error Display */}
            {exportError && (
              <NxErrorAlert onClose={clearErrors}>
                Failed to export user activity detail data: {exportError}
              </NxErrorAlert>
            )}

            {/* Data Table */}
            <div className="nx-table-container user-activity-details-table">
              {filtersAreDirty && <UserActivityMask />}
              <NxTable id="user-activity-details-table">
                <NxTableHead>
                  <NxTableRow>
                    <NxTableCell
                      isSortable
                      sortDir={getSortDirection('timestamp')}
                      onClick={() => handleSort('timestamp')}
                    >
                      Timestamp
                    </NxTableCell>
                    <NxTableCell isSortable sortDir={getSortDirection('domain')} onClick={() => handleSort('domain')}>
                      Domain
                    </NxTableCell>
                    <NxTableCell isSortable sortDir={getSortDirection('type')} onClick={() => handleSort('type')}>
                      Type
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={getSortDirection('errorType')}
                      onClick={() => handleSort('errorType')}
                    >
                      Error
                    </NxTableCell>
                    <NxTableCell isSortable sortDir={getSortDirection('uri')} onClick={() => handleSort('uri')}>
                      Request URI
                    </NxTableCell>
                    <NxTableCell isSortable sortDir={getSortDirection('method')} onClick={() => handleSort('method')}>
                      Method
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={getSortDirection('ipAddress')}
                      onClick={() => handleSort('ipAddress')}
                    >
                      IP Address
                    </NxTableCell>
                    <NxTableCell
                      isSortable
                      sortDir={getSortDirection('userAgent')}
                      onClick={() => handleSort('userAgent')}
                    >
                      User Agent
                    </NxTableCell>
                  </NxTableRow>
                </NxTableHead>
                <NxTableBody emptyMessage="No activity found for the selected criteria.">
                  {sortedActivities.map((activity, index) => (
                    <UserActivityDetailRow key={`${activity.timestamp}-${index}`} activity={activity} />
                  ))}
                </NxTableBody>
              </NxTable>

              {/* Pagination */}
              {(canGoPrevious || canGoNext) && (
                <div className="nx-table-container__footer">
                  <NxIndeterminatePagination
                    onPrevPageSelect={handlePreviousPage}
                    onNextPageSelect={handleNextPage}
                    isFirstPage={!canGoPrevious}
                    isLastPage={!canGoNext}
                  />
                </div>
              )}
            </div>

            {/* Summary Information */}
            <div className="activity-summary">
              <p className="nx-p">Showing {activities.length} activities</p>
            </div>
          </div>
        </section>

        {/* Filter Drawer */}
        <UserActivityDetailsFilter
          isOpen={filterDrawerOpen}
          onClose={() => toggleFilterDrawer(false)}
          selectedActivityTypes={selectedActivityTypes}
          selectedDomains={selectedDomains}
          selectedErrorTypes={selectedErrorTypes}
          filterOptions={filterOptions}
          onActivityTypesChange={setSelectedActivityTypes}
          onDomainsChange={setSelectedDomains}
          onErrorTypesChange={setSelectedErrorTypes}
          onApply={handleFilterApply}
          onReset={handleFilterReset}
          filtersAreDirty={filtersAreDirty}
        />
      </LoadWrapper>
    </main>
  );
}

function UserActivityDetailRow({ activity }) {
  const {
    timestamp,
    domain = '',
    type = '',
    errorType = '',
    uri = '',
    method = '',
    ipAddress = '',
    userAgent = '',
  } = activity;

  return (
    <NxTableRow className="user-activity-detail-row">
      <NxTableCell className="timestamp-cell">{formatDate(timestamp, USER_ACTIVITY_DATE_FORMAT) || 'N/A'}</NxTableCell>
      <NxTableCell className="domain-cell">{domain}</NxTableCell>
      <NxTableCell className="type-cell">{type}</NxTableCell>
      <NxTableCell className="error-cell">{errorType || ''}</NxTableCell>
      <NxTableCell className="uri-cell" title={uri}>
        {uri}
      </NxTableCell>
      <NxTableCell className="method-cell">{method}</NxTableCell>
      <NxTableCell className="ip-address-cell">{ipAddress}</NxTableCell>
      <NxTableCell className="user-agent-cell" title={userAgent}>
        {userAgent}
      </NxTableCell>
    </NxTableRow>
  );
}

UserActivityDetails.propTypes = {
  username: PropTypes.string.isRequired,
  appliedAge: PropTypes.number,
  activities: PropTypes.arrayOf(
    PropTypes.shape({
      timestamp: PropTypes.string,
      domain: PropTypes.string,
      type: PropTypes.string,
      error: PropTypes.bool,
      uri: PropTypes.string,
      method: PropTypes.string,
      ipAddress: PropTypes.string,
      userAgent: PropTypes.string,
    })
  ),
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  totalActivities: PropTypes.number,
  pagination: PropTypes.shape({
    limit: PropTypes.number,
    offset: PropTypes.number,
    hasMore: PropTypes.bool,
  }),
  filterDrawerOpen: PropTypes.bool,
  filtersAreDirty: PropTypes.bool,
  selectedActivityTypes: PropTypes.arrayOf(PropTypes.string),
  selectedDomains: PropTypes.arrayOf(PropTypes.string),
  selectedErrorTypes: PropTypes.arrayOf(PropTypes.string),
  filterOptions: PropTypes.shape({
    activityTypes: PropTypes.arrayOf(PropTypes.string),
    domains: PropTypes.arrayOf(PropTypes.string),
    errorTypes: PropTypes.arrayOf(PropTypes.string),
  }),
  loadUserActivityDetail: PropTypes.func.isRequired,
  loadFilterOptions: PropTypes.func.isRequired,
  applyFilters: PropTypes.func.isRequired,
  toggleFilterDrawer: PropTypes.func.isRequired,
  setSelectedActivityTypes: PropTypes.func.isRequired,
  setSelectedDomains: PropTypes.func.isRequired,
  setSelectedErrorTypes: PropTypes.func.isRequired,
  revertFilters: PropTypes.func.isRequired,
};

UserActivityDetailRow.propTypes = {
  activity: PropTypes.shape({
    timestamp: PropTypes.string,
    domain: PropTypes.string,
    type: PropTypes.string,
    error: PropTypes.bool,
    uri: PropTypes.string,
    method: PropTypes.string,
    ipAddress: PropTypes.string,
    userAgent: PropTypes.string,
  }).isRequired,
};
