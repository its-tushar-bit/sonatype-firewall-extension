/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';

import UserList from './userList/UserList';
import UserActivityOverviewContainer from '../../configuration/userActivityOverview/UserActivityOverviewContainer';
import { selectIsUserActivityTrackingEnabled } from '../../productFeatures/productFeaturesSelectors';

const tabs = ['users', 'activity'];
const tabNames = new Map([
  [tabs[0], 'Users'],
  [tabs[1], 'Activity'],
]);

export default function UserManagement(props) {
  const {
    activeTab = 'users',
    onTabChange,
    stateGo,
    isAuthorized,
    isCheckingPermissions = false,
    ...userListProps
  } = props;

  const isUserActivityTrackingEnabled = useSelector(selectIsUserActivityTrackingEnabled);

  // Only show activity tab if feature is enabled
  const availableTabs = isUserActivityTrackingEnabled ? tabs : [tabs[0]];

  const handleTabClick = (index) => {
    const selectedTab = availableTabs[index];
    if (onTabChange) {
      onTabChange(selectedTab);
    }
  };

  const handleUserClick = (username) => {
    stateGo('userActivityDetails', { username });
  };

  const renderTabContent = () => {
    switch (activeTab) {
      case 'activity':
        if (!isUserActivityTrackingEnabled) {
          return null;
        }
        return (
          <UserActivityOverviewContainer
            onUserClick={handleUserClick}
            isAuthorized={isAuthorized}
            isCheckingPermissions={isCheckingPermissions}
          />
        );
      case 'users':
      default:
        return <UserList {...userListProps} stateGo={stateGo} />;
    }
  };

  return (
    <main id="user-management" className="nx-page-main">
      <div className="nx-page-title">
        <h1 className="nx-h1">User Management</h1>
      </div>

      <NxTabs activeTab={availableTabs.indexOf(activeTab)} onTabSelect={handleTabClick}>
        <NxTabList>
          {availableTabs.map((tab) => (
            <NxTab key={tab}>{tabNames.get(tab)}</NxTab>
          ))}
        </NxTabList>
      </NxTabs>

      {renderTabContent()}
    </main>
  );
}

UserManagement.propTypes = {
  activeTab: PropTypes.oneOf(['users', 'activity']),
  onTabChange: PropTypes.func,
  isAuthorized: PropTypes.bool,
  isCheckingPermissions: PropTypes.bool,
  // UserList props
  stateGo: PropTypes.func.isRequired,
  loadListPage: PropTypes.func,
  deleteUser: PropTypes.func,
  users: PropTypes.array,
  loading: PropTypes.bool,
  loadError: PropTypes.string,
  deleteError: PropTypes.string,
  deleteMaskState: PropTypes.bool,
  currentUsername: PropTypes.string,
  tenantMode: PropTypes.string,
};
