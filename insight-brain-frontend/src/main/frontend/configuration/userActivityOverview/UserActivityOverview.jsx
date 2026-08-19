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
  NxPagination,
  NxFilterInput,
  NxErrorAlert,
} from '@sonatype/react-shared-components';
import { faDownload, faFilter, faChevronRight } from '@fortawesome/free-solid-svg-icons';
import moment from 'moment';

import LoadWrapper from '../../react/LoadWrapper';
import UserActivityFilter from './UserActivityFilter';
import UserActivityMask from './UserActivityMask';
import { USER_ACTIVITY_PAGE_SIZE, calculateOffset, calculateDateRange } from './userActivitySlice';

// Helper function to get time frame display text
const getTimeFrameText = (ageInDays) => {
  switch (ageInDays) {
    case 1:
      return 'past 24 hours';
    case 7:
      return 'past 7 days';
    case 30:
      return 'past 30 days';
    default:
      return 'selected period';
  }
};

export default function UserActivityOverview(props) {
  const {
    loadUserActivityPage,
    exportUserActivityData,
    applyFilters,
    users = [],
    loading = false,
    loadError = null,
    exporting = false,
    exportError = null,
    totalUsers = 0,
    filterDrawerOpen = false,
    selectedAge = 30,
    appliedAge = 30,
    filtersAreDirty = false,
    searchFilter = '',
    toggleFilterDrawer,
    setSelectedAge,
    revertFilters,
    searchUsers,
    clearErrors,
    onUserClick,
  } = props;

  // State for sorting and pagination
  const [sortConfig, setSortConfig] = useState({
    key: 'loginCount',
    direction: 'desc',
  });
  const [page, setPage] = useState(0);

  // Clear errors on component unmount to prevent persistence across navigation
  useEffect(() => clearErrors, [clearErrors]);

  // Load data on component mount and when pagination changes
  // (Search is handled by debounced searchUsers action)
  useEffect(() => {
    const params = {
      username: searchFilter || null,
      limit: USER_ACTIVITY_PAGE_SIZE,
      offset: calculateOffset(page),
    };

    // Use permission-checked action for initial load (page 0) to ensure consistent permission handling
    if (page === 0) {
      loadUserActivityPage(params);
    } else {
      // Use regular applyFilters for pagination after permission check passed
      applyFilters(params);
    }
  }, [page]); // Only pagination dependency - search handled separately!

  // Sort users (server handles search filtering)
  const sortedUsers = useMemo(() => {
    if (!sortConfig.key) return users;

    return [...users].sort((a, b) => {
      let aVal = a[sortConfig.key];
      let bVal = b[sortConfig.key];

      // Handle different data types
      if (sortConfig.key === 'loginCount') {
        aVal = parseInt(aVal) || 0;
        bVal = parseInt(bVal) || 0;
      } else if (sortConfig.key === 'lastActive') {
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
  }, [users, sortConfig]);

  // Pagination
  const pageCount = Math.ceil(totalUsers / USER_ACTIVITY_PAGE_SIZE);
  const currentPage = page;

  // Event handlers
  const handleSort = (key) => {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }));
  };

  const handleSearchChange = (value) => {
    searchUsers(value);
    setPage(0); // Reset to first page when searching
  };

  const handleFilterToggle = () => {
    toggleFilterDrawer();
  };

  const handleAgeChange = (age) => {
    setSelectedAge(age);
  };

  const handleFilterApply = () => {
    setPage(0); // Reset to first page when applying filters
    applyFilters({
      username: searchFilter || null,
      limit: USER_ACTIVITY_PAGE_SIZE,
      offset: 0,
    });
  };

  const handleFilterReset = () => {
    revertFilters();
    setPage(0);
  };

  const handleExport = () => {
    // Calculate current date range based on selected age
    const dateRange = calculateDateRange(selectedAge);
    exportUserActivityData({
      ...dateRange,
      username: searchFilter || null,
    });
  };

  const getSortDirection = (key) => {
    if (sortConfig.key !== key) return null;
    return sortConfig.direction;
  };

  return (
    <LoadWrapper
      loading={loading}
      error={loadError}
      retryHandler={() =>
        applyFilters({
          username: searchFilter || null,
          limit: USER_ACTIVITY_PAGE_SIZE,
          offset: calculateOffset(page),
        })
      }
    >
      <section className="nx-tile">
        <header className="nx-tile-header">
          <div className="nx-tile__header-main">
            <NxFilterInput
              id="user-search"
              placeholder="Search by user name"
              value={searchFilter}
              onChange={handleSearchChange}
              searchIcon
            />
          </div>
          <div className="nx-tile__actions">
            <NxButton variant="tertiary" onClick={handleExport} disabled={loading || users.length === 0 || exporting}>
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
            <NxErrorAlert onClose={clearErrors}>Failed to export user activity data: {exportError}</NxErrorAlert>
          )}

          {/* Data Table */}
          <div className="nx-table-container user-activity-table" style={{ position: 'relative' }}>
            {filtersAreDirty && <UserActivityMask />}
            <NxTable id="user-activity-table">
              <NxTableHead>
                <NxTableRow>
                  <NxTableCell isSortable sortDir={getSortDirection('username')} onClick={() => handleSort('username')}>
                    Username
                  </NxTableCell>
                  <NxTableCell
                    isSortable
                    sortDir={getSortDirection('loginCount')}
                    onClick={() => handleSort('loginCount')}
                  >
                    Login Count ({getTimeFrameText(appliedAge)})
                  </NxTableCell>
                  <NxTableCell
                    isSortable
                    sortDir={getSortDirection('lastActive')}
                    onClick={() => handleSort('lastActive')}
                  >
                    Last Active
                  </NxTableCell>
                  <NxTableCell className="nx-cell--meta-info"></NxTableCell>
                </NxTableRow>
              </NxTableHead>
              <NxTableBody emptyMessage="No user activity found for the selected criteria.">
                {sortedUsers.map((user) => (
                  <UserActivityRow key={user.username} user={user} onUserClick={onUserClick} />
                ))}
              </NxTableBody>
            </NxTable>

            {/* Pagination */}
            {pageCount > 1 && (
              <div className="nx-table-container__footer">
                <NxPagination onChange={setPage} pageCount={pageCount} currentPage={currentPage} />
              </div>
            )}
          </div>

          {/* Summary Information */}
          <div className="activity-summary">
            <p className="nx-p">
              Showing {users.length} of {totalUsers} users
            </p>
          </div>
        </div>
      </section>

      {/* Filter Drawer */}
      <UserActivityFilter
        isOpen={filterDrawerOpen}
        onClose={() => toggleFilterDrawer(false)}
        selectedAge={selectedAge}
        onAgeChange={handleAgeChange}
        onApply={handleFilterApply}
        onReset={handleFilterReset}
        filtersAreDirty={filtersAreDirty}
      />
    </LoadWrapper>
  );
}

function UserActivityRow({ user, onUserClick }) {
  const { username, loginCount = 0, lastActive } = user;

  const handleRowClick = () => {
    if (onUserClick) {
      onUserClick(username);
    }
  };

  return (
    <NxTableRow className="user-activity-row" isClickable onClick={handleRowClick}>
      <NxTableCell className="username-cell" title={username}>
        {username}
      </NxTableCell>
      <NxTableCell className="login-count-cell">{loginCount}</NxTableCell>
      <NxTableCell className="last-active-cell">
        {lastActive ? moment(lastActive).format('MMM DD, YYYY HH:mm') : 'Never'}
      </NxTableCell>
      <NxTableCell className="nx-cell--meta-info">
        <NxFontAwesomeIcon icon={faChevronRight} />
      </NxTableCell>
    </NxTableRow>
  );
}

UserActivityOverview.propTypes = {
  loadUserActivity: PropTypes.func.isRequired,
  loadUserActivityPage: PropTypes.func.isRequired,
  exportUserActivityData: PropTypes.func.isRequired,
  applyFilters: PropTypes.func.isRequired,
  toggleFilterDrawer: PropTypes.func.isRequired,
  setSelectedAge: PropTypes.func.isRequired,
  revertFilters: PropTypes.func.isRequired,
  searchUsers: PropTypes.func.isRequired,
  onUserClick: PropTypes.func,
  users: PropTypes.arrayOf(
    PropTypes.shape({
      username: PropTypes.string.isRequired,
      loginCount: PropTypes.number,
      lastActive: PropTypes.string,
    })
  ),
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  exporting: PropTypes.bool,
  exportError: PropTypes.string,
  totalUsers: PropTypes.number,
  tenantMode: PropTypes.string,
  filterDrawerOpen: PropTypes.bool,
  selectedAge: PropTypes.number,
  appliedAge: PropTypes.number,
  filtersAreDirty: PropTypes.bool,
  searchFilter: PropTypes.string,
};

UserActivityRow.propTypes = {
  user: PropTypes.shape({
    username: PropTypes.string.isRequired,
    loginCount: PropTypes.number,
    lastActive: PropTypes.string,
  }).isRequired,
  onUserClick: PropTypes.func,
};
