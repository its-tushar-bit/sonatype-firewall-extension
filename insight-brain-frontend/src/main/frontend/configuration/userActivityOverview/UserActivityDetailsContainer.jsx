/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback, useState } from 'react';
import * as PropTypes from 'prop-types';
import { useSelector, useDispatch } from 'react-redux';
import { selectAppliedAge, selectIsUserActivityTrackingEnabled } from './userActivitySelectors';
import { authErrorMessage } from '../../util/authorizationUtil';

import {
  loadUserActivityDetail,
  loadFilterOptions,
  applyDetailsFilters,
  toggleDetailsFilterDrawer,
  setSelectedActivityTypes,
  setSelectedDomains,
  setSelectedErrorTypes,
  revertDetailsFilters,
  clearDetailsData,
  setDetailsCurrentUser,
  exportUserActivityData,
  clearErrors,
} from './userActivitySlice';

import UserActivityDetails from './UserActivityDetails';

export default function UserActivityDetailsContainer({ isAuthorized }) {
  const dispatch = useDispatch();
  const [hasInitiallyLoaded, setHasInitiallyLoaded] = useState(false);

  // Get router from Redux state and extract username from route params
  const router = useSelector((state) => state.router);
  const username = router?.currentParams?.username;
  const appliedAge = useSelector(selectAppliedAge);

  // Check if User Activity Tracking feature is enabled
  const isUserActivityTrackingEnabled = useSelector(selectIsUserActivityTrackingEnabled);

  // Get user activity state (always call this hook to maintain consistent hook order)
  const {
    detailsCurrentUser,
    detailsActivities,
    detailsTotalActivities,
    detailsLoading,
    detailsLoadError: detailsLoadErrorProp,
    detailsFilterDrawerOpen,
    detailsFiltersAreDirty,
    detailsSelectedFilters,
    detailsPagination,
    filterOptions,
    detailsExporting,
    detailsExportError,
  } = useSelector((state) => state.userActivity);

  // Use authorization error message if not authorized, otherwise use the actual load error
  const detailsLoadError = isAuthorized ? detailsLoadErrorProp : authErrorMessage;

  // Initialize current user when username prop changes
  useEffect(() => {
    if (isUserActivityTrackingEnabled && username && username !== detailsCurrentUser) {
      dispatch(setDetailsCurrentUser(username));
    }
  }, [isUserActivityTrackingEnabled, username, detailsCurrentUser, dispatch]);

  // Load filter options on mount
  useEffect(() => {
    if (isUserActivityTrackingEnabled) {
      dispatch(loadFilterOptions());
    }
  }, [isUserActivityTrackingEnabled, dispatch]);

  // Load initial data when username changes (but not if there's already an error)
  useEffect(() => {
    if (isUserActivityTrackingEnabled && username && !detailsLoadErrorProp && !hasInitiallyLoaded) {
      dispatch(applyDetailsFilters({ username }));
      setHasInitiallyLoaded(true);
    }
  }, [isUserActivityTrackingEnabled, username, detailsLoadErrorProp, hasInitiallyLoaded, dispatch]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (isUserActivityTrackingEnabled) {
        dispatch(clearDetailsData());
      }
    };
  }, [isUserActivityTrackingEnabled, dispatch]);

  const handleLoadUserActivityDetail = useCallback(
    (params) => {
      dispatch(loadUserActivityDetail(params));
    },
    [dispatch]
  );

  const handleLoadFilterOptions = useCallback(() => {
    dispatch(loadFilterOptions());
  }, [dispatch]);

  const handleApplyFilters = useCallback(
    (params) => {
      dispatch(applyDetailsFilters({ username, ...params }));
    },
    [dispatch, username]
  );

  const handleToggleFilterDrawer = useCallback(
    (isOpen) => {
      dispatch(toggleDetailsFilterDrawer(isOpen));
    },
    [dispatch]
  );

  const handleSetSelectedActivityTypes = useCallback(
    (activityTypes) => {
      dispatch(setSelectedActivityTypes(activityTypes));
    },
    [dispatch]
  );

  const handleSetSelectedDomains = useCallback(
    (domains) => {
      dispatch(setSelectedDomains(domains));
    },
    [dispatch]
  );

  const handleSetSelectedErrorTypes = useCallback(
    (errorTypes) => {
      dispatch(setSelectedErrorTypes(errorTypes));
    },
    [dispatch]
  );

  const handleRevertFilters = useCallback(() => {
    dispatch(revertDetailsFilters());
  }, [dispatch]);

  const handleExportUserActivityDetailData = useCallback(
    (params) => {
      dispatch(exportUserActivityData({ username, ...params }));
    },
    [dispatch, username]
  );

  const handleClearErrors = useCallback(() => {
    dispatch(clearErrors());
  }, [dispatch]);

  // If feature is disabled, don't render the component
  if (!isUserActivityTrackingEnabled) {
    return null;
  }

  return (
    <UserActivityDetails
      username={username || ''}
      appliedAge={appliedAge}
      activities={detailsActivities}
      loading={detailsLoading}
      loadError={detailsLoadError}
      totalActivities={detailsTotalActivities}
      pagination={detailsPagination}
      filterDrawerOpen={detailsFilterDrawerOpen}
      filtersAreDirty={detailsFiltersAreDirty}
      selectedActivityTypes={detailsSelectedFilters.selectedActivityTypes}
      selectedDomains={detailsSelectedFilters.selectedDomains}
      selectedErrorTypes={detailsSelectedFilters.selectedErrorTypes}
      filterOptions={filterOptions}
      loadUserActivityDetail={handleLoadUserActivityDetail}
      loadFilterOptions={handleLoadFilterOptions}
      applyFilters={handleApplyFilters}
      toggleFilterDrawer={handleToggleFilterDrawer}
      setSelectedActivityTypes={handleSetSelectedActivityTypes}
      setSelectedDomains={handleSetSelectedDomains}
      setSelectedErrorTypes={handleSetSelectedErrorTypes}
      revertFilters={handleRevertFilters}
      exportUserActivityData={handleExportUserActivityDetailData}
      exporting={detailsExporting}
      exportError={detailsExportError}
      clearErrors={handleClearErrors}
    />
  );
}

UserActivityDetailsContainer.propTypes = {
  isAuthorized: PropTypes.bool.isRequired,
};
