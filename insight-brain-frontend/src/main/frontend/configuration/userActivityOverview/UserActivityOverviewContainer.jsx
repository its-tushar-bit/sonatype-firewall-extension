/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useCallback } from 'react';
import * as PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import { authErrorMessage } from '../../util/authorizationUtil';
import UserActivityOverview from './UserActivityOverview';
import {
  selectUserActivityData,
  selectUserActivityLoading,
  selectUserActivityError,
  selectUserActivityExporting,
  selectUserActivityExportError,
  selectTotalUsers,
  selectFilterDrawerOpen,
  selectSelectedAge,
  selectAppliedAge,
  selectFiltersAreDirty,
  selectSearchFilter,
} from './userActivitySelectors';
import {
  loadUserActivity,
  loadUserActivityPage,
  exportUserActivityData,
  applyFilters,
  toggleFilterDrawer,
  setSelectedAge,
  revertFilters,
  searchUsers,
  clearErrors,
} from './userActivitySlice';

export default function UserActivityOverviewContainer(props) {
  const { onUserClick, isAuthorized, isCheckingPermissions = false, ...otherProps } = props;
  const dispatch = useDispatch();

  // Redux state selectors
  const users = useSelector(selectUserActivityData);
  const loading = useSelector(selectUserActivityLoading);
  const loadErrorProp = useSelector(selectUserActivityError);

  // Use authorization error message if not authorized, otherwise use the actual load error
  // Show loading state during permission check to prevent flickering
  const loadError = isCheckingPermissions ? null : isAuthorized ? loadErrorProp : authErrorMessage;
  const exporting = useSelector(selectUserActivityExporting);
  const exportError = useSelector(selectUserActivityExportError);
  const totalUsers = useSelector(selectTotalUsers);
  const filterDrawerOpen = useSelector(selectFilterDrawerOpen);
  const selectedAge = useSelector(selectSelectedAge);
  const appliedAge = useSelector(selectAppliedAge);
  const filtersAreDirty = useSelector(selectFiltersAreDirty);
  const searchFilter = useSelector(selectSearchFilter);

  // Action creators
  const handleLoadUserActivity = (params) => {
    dispatch(loadUserActivity(params));
  };

  const handleLoadUserActivityPage = (params) => {
    dispatch(loadUserActivityPage(params));
  };

  const handleExportUserActivityData = (params) => {
    dispatch(exportUserActivityData(params));
  };

  const handleApplyFilters = (params) => {
    dispatch(applyFilters(params));
  };

  const handleToggleFilterDrawer = (isOpen) => {
    dispatch(toggleFilterDrawer(isOpen));
  };

  const handleSetSelectedAge = (age) => {
    dispatch(setSelectedAge(age));
  };

  const handleRevertFilters = () => {
    dispatch(revertFilters());
  };

  const handleSearchUsers = (searchValue) => {
    dispatch(searchUsers(searchValue));
  };

  const handleClearErrors = useCallback(() => {
    dispatch(clearErrors());
  }, [dispatch]);

  return (
    <UserActivityOverview
      {...otherProps}
      users={users}
      loading={loading || isCheckingPermissions}
      loadError={loadError}
      exporting={exporting}
      exportError={exportError}
      totalUsers={totalUsers}
      filterDrawerOpen={filterDrawerOpen}
      selectedAge={selectedAge}
      appliedAge={appliedAge}
      filtersAreDirty={filtersAreDirty}
      searchFilter={searchFilter}
      loadUserActivity={handleLoadUserActivity}
      loadUserActivityPage={handleLoadUserActivityPage}
      exportUserActivityData={handleExportUserActivityData}
      applyFilters={handleApplyFilters}
      toggleFilterDrawer={handleToggleFilterDrawer}
      setSelectedAge={handleSetSelectedAge}
      revertFilters={handleRevertFilters}
      searchUsers={handleSearchUsers}
      clearErrors={handleClearErrors}
      onUserClick={onUserClick}
    />
  );
}

UserActivityOverviewContainer.propTypes = {
  onUserClick: PropTypes.func,
  isAuthorized: PropTypes.bool,
  isCheckingPermissions: PropTypes.bool,
};
