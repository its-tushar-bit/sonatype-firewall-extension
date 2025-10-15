/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { pick } from 'ramda';
import { connect } from 'react-redux';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { selectTenantMode } from '../../productFeatures/productFeaturesSelectors';
import { loadListPage, deleteUser } from './usersActions';
import { getPermissions } from '../../util/authorizationUtil';
import UserManagement from './UserManagement';

function UserManagementContainer(props) {
  const { router, stateGo } = props;

  // Determine initial active tab based on current router state
  const getInitialActiveTab = () => {
    if (router?.currentState?.name === 'users.activity') {
      return 'activity';
    }
    return 'users';
  };

  const [activeTab, setActiveTab] = useState(getInitialActiveTab);
  const [isAuthorized, setIsAuthorized] = useState(false); // Default to false, check on mount
  const [isCheckingPermissions, setIsCheckingPermissions] = useState(true); // Loading state

  // Check permissions and feature flags on mount and when active tab changes
  useEffect(() => {
    setIsCheckingPermissions(true);

    if (activeTab === 'activity') {
      // Activity tab requires both CONFIGURE_SYSTEM and ACCESS_AUDIT_LOG permissions
      getPermissions(['CONFIGURE_SYSTEM', 'ACCESS_AUDIT_LOG'])
        .then((permissions) => {
          setIsAuthorized(permissions.length === 2);
          setIsCheckingPermissions(false);
        })
        .catch(() => {
          setIsAuthorized(false);
          setIsCheckingPermissions(false);
        });
    } else {
      // Users tab only requires CONFIGURE_SYSTEM permission
      getPermissions(['CONFIGURE_SYSTEM'])
        .then((permissions) => {
          setIsAuthorized(permissions.length > 0);
          setIsCheckingPermissions(false);
        })
        .catch(() => {
          setIsAuthorized(false);
          setIsCheckingPermissions(false);
        });
    }
  }, [activeTab]);

  // Update active tab when router state changes
  useEffect(() => {
    const currentTab = getInitialActiveTab();
    if (currentTab !== activeTab) {
      setActiveTab(currentTab);
    }
  }, [router?.currentState?.name]);

  const handleTabChange = (tab) => {
    setActiveTab(tab);
    // Navigate to appropriate state based on tab
    if (tab === 'activity') {
      stateGo('users.activity');
    } else {
      stateGo('users');
    }
  };

  return (
    <UserManagement
      {...props}
      activeTab={activeTab}
      onTabChange={handleTabChange}
      isAuthorized={isAuthorized}
      isCheckingPermissions={isCheckingPermissions}
    />
  );
}

UserManagementContainer.propTypes = {
  router: PropTypes.shape({
    currentState: PropTypes.shape({
      name: PropTypes.string,
    }),
  }),
  stateGo: PropTypes.func.isRequired,
};

export default connect(
  (state) => {
    const { userConfiguration, router } = state;
    const tenantMode = selectTenantMode(state);

    return {
      ...pick(
        ['users', 'loading', 'loadError', 'currentUsername', 'deleteError', 'deleteMaskState'],
        userConfiguration
      ),
      tenantMode,
      router,
    };
  },
  {
    stateGo,
    loadListPage,
    deleteUser,
  }
)(UserManagementContainer);
