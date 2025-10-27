/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from 'TestRoot/SpecUtil';
import UserManagement from 'MainRoot/security/users/UserManagement';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

jest.mock('MainRoot/security/users/userList/UserList', () => {
  return function MockUserList() {
    return <div data-testid="user-list">User List Component</div>;
  };
});

jest.mock('MainRoot/configuration/userActivityOverview/UserActivityOverviewContainer', () => {
  return function MockUserActivityOverviewContainer() {
    return <div data-testid="user-activity-overview">User Activity Overview Component</div>;
  };
});

describe('UserManagement', () => {
  let selectIsUserActivityTrackingEnabledSpy, selectIsUserManagementPagesEnabledSpy;

  const defaultProps = {
    activeTab: 'users',
    onTabChange: jest.fn(),
    stateGo: jest.fn(),
    isAuthorized: true,
    isCheckingPermissions: false,
    loadListPage: jest.fn(),
    deleteUser: jest.fn(),
    users: [],
    loading: false,
    loadError: null,
    deleteError: null,
    deleteMaskState: false,
    currentUsername: 'admin',
    tenantMode: 'single-tenant',
  };

  beforeEach(() => {
    selectIsUserActivityTrackingEnabledSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsUserActivityTrackingEnabled')
      .mockReturnValue(false);
    selectIsUserManagementPagesEnabledSpy = jest
      .spyOn(productFeaturesSelectors, 'selectIsUserManagementPagesEnabled')
      .mockReturnValue(true);
  });

  describe('when feature flag is OFF (on-prem default)', () => {
    beforeEach(() => {
      selectIsUserActivityTrackingEnabledSpy.mockReturnValue(false);
      selectIsUserManagementPagesEnabledSpy.mockReturnValue(true);
    });

    it('should render page title as "Users"', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.getByRole('heading', { name: 'Users' })).toBeInTheDocument();
    });

    it('should not render tabs', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    });

    it('should render UserList component directly', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.getByTestId('user-list')).toBeInTheDocument();
      expect(screen.queryByTestId('user-activity-overview')).not.toBeInTheDocument();
    });
  });

  describe('when feature flag is ON and user management enabled (on-prem with flag)', () => {
    beforeEach(() => {
      selectIsUserActivityTrackingEnabledSpy.mockReturnValue(true);
      selectIsUserManagementPagesEnabledSpy.mockReturnValue(true);
    });

    it('should render page title as "User Management"', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.getByRole('heading', { name: 'User Management' })).toBeInTheDocument();
    });

    it('should render tabs', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.getByRole('tablist')).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Users/ })).toBeInTheDocument();
      expect(screen.getByRole('tab', { name: /Activity/ })).toBeInTheDocument();
    });

    it('should show Users tab content by default', () => {
      render(<UserManagement {...defaultProps} />);

      expect(screen.getByTestId('user-list')).toBeInTheDocument();
      expect(screen.queryByTestId('user-activity-overview')).not.toBeInTheDocument();
    });

    it('should show Activity tab content when activeTab is "activity"', () => {
      render(<UserManagement {...defaultProps} activeTab="activity" />);

      expect(screen.queryByTestId('user-list')).not.toBeInTheDocument();
      expect(screen.getByTestId('user-activity-overview')).toBeInTheDocument();
    });

    it('should call onTabChange when tab is clicked', async () => {
      const user = userEvent.setup();
      const onTabChange = jest.fn();

      render(<UserManagement {...defaultProps} onTabChange={onTabChange} />);

      await user.click(screen.getByRole('tab', { name: /Activity/ }));

      expect(onTabChange).toHaveBeenCalledWith('activity');
    });
  });

  describe('when feature flag is ON but user management disabled (SaaS mode)', () => {
    beforeEach(() => {
      selectIsUserActivityTrackingEnabledSpy.mockReturnValue(true);
      selectIsUserManagementPagesEnabledSpy.mockReturnValue(false);
    });

    it('should render page title as "User Activity"', () => {
      render(<UserManagement {...defaultProps} activeTab="activity" />);

      expect(screen.getByRole('heading', { name: 'User Activity' })).toBeInTheDocument();
    });

    it('should not render tabs', () => {
      render(<UserManagement {...defaultProps} activeTab="activity" />);

      expect(screen.queryByRole('tablist')).not.toBeInTheDocument();
    });

    it('should render UserActivityOverview component directly', () => {
      render(<UserManagement {...defaultProps} activeTab="activity" />);

      expect(screen.getByTestId('user-activity-overview')).toBeInTheDocument();
      expect(screen.queryByTestId('user-list')).not.toBeInTheDocument();
    });
  });

  describe('stateGo navigation', () => {
    it('should navigate to userActivityDetails when user is clicked in activity view', () => {
      selectIsUserActivityTrackingEnabledSpy.mockReturnValue(true);
      selectIsUserManagementPagesEnabledSpy.mockReturnValue(false);

      const stateGo = jest.fn();

      render(<UserManagement {...defaultProps} stateGo={stateGo} activeTab="activity" />);

      expect(stateGo).not.toHaveBeenCalled();
    });
  });
});
