/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { cloneElement } from 'react';
import { mount } from 'enzyme';
import configureStore from 'redux-mock-store';
import { Provider } from 'react-redux';
import * as enzymeUtils from '../../../enzymeUtils';
import UserMenu from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserMenu';
import UserTokenModal from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/UserTokenModal';
import UserDetailsModal from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserDetailsModal';
import ChangePasswordModal from '../../../../../main/frontend/mainHeader/MenuBar/UserMenu/ChangePasswordModal';

describe('UserMenu', function () {
  let minProps;
  let getMountedComponent;

  beforeEach(() => {
    minProps = {
      user: { displayName: 'Example User' },
      isUserTokenModalVisible: false,
      loadUser: jasmine.createSpy(),
      onLogout: jasmine.createSpy(),
      canChangePassword: false,
      onChangePassword: jasmine.createSpy(),
      changePasswordStatus: 'idle',
      changePasswordErrorMessage: undefined,
      onManageUserToken: jasmine.createSpy(),
    };
    getMountedComponent = enzymeUtils.getMountedComponent(UserMenu, minProps);
  });

  it('calls the loadUser callback prop immediately on render', () => {
    const loadUser = jasmine.createSpy('loadUser');
    // mount needed for useEffect to be run sync
    const component = getMountedComponent({ loadUser });
    expect(loadUser).toHaveBeenCalled();

    component.unmount();
  });

  it('renders the displayName of the user', () => {
    const user = { displayName: 'My Display Name' };

    const component = getMountedComponent({ user });
    const button = component.find('button');
    button.simulate('click');
    const userName = component.find('#user-name');

    expect(userName).toHaveText(user.displayName);

    component.unmount();
  });

  it('calls onLogout callback prop when logout link is clicked', () => {
    const onLogout = jasmine.createSpy('logout');
    const component = getMountedComponent({ onLogout });

    const button = component.find('button');
    button.simulate('click');
    const logoutLink = component.find('#logout');
    logoutLink.simulate('click');

    expect(onLogout).toHaveBeenCalled();

    component.unmount();
  });

  it('calls onManageUserToken callback prop when manage user token link is clicked', () => {
    const onManageUserToken = jasmine.createSpy('onManageUserToken');
    const component = getMountedComponent({ onManageUserToken });

    const button = component.find('button');
    button.simulate('click');
    const logoutLink = component.find('#user-token-management');
    logoutLink.simulate('click');

    expect(onManageUserToken).toHaveBeenCalled();

    component.unmount();
  });

  it('opens the UserTokenModal component when isUserTokenModalVisible prop is true', () => {
    const store = configureStore([])({});
    const wrapper = mount(
      <Provider store={store}>
        <UserMenu {...minProps} />
      </Provider>
    );
    expect(wrapper.find(UserTokenModal)).not.toExist();

    // rerender UserMenu with new props
    wrapper.setProps({
      children: cloneElement(wrapper.props().children, { ...minProps, isUserTokenModalVisible: true }),
    });

    expect(wrapper.find(UserTokenModal)).toExist();

    wrapper.unmount();
  });

  it('opens the userdetails modal when the Details link is clicked', () => {
    const component = getMountedComponent();
    const button = component.find('button');
    button.simulate('click');
    const userDetails = component.find('#user-details');
    userDetails.simulate('click');
    expect(component.find(UserDetailsModal)).toExist();
    component.unmount();
  });

  describe('change password modal', () => {
    it('shows the change password link if canChangePassword prop is true', () => {
      const withoutComponent = getMountedComponent({ canChangePassword: false });
      const withoutButton = withoutComponent.find('button');
      withoutButton.simulate('click');
      expect(withoutComponent.find('#change-password')).not.toExist();

      const component = getMountedComponent({ canChangePassword: true });
      const button = component.find('button');
      button.simulate('click');
      expect(component.find('#change-password')).toExist();

      component.unmount();
    });

    it('opens the changePasswordModal when the Change Password link is clicked', () => {
      const component = getMountedComponent({ canChangePassword: true });
      const button = component.find('button');
      button.simulate('click');
      const userDetails = component.find('#change-password');
      userDetails.simulate('click');
      expect(component.find(ChangePasswordModal)).toExist();

      component.unmount();
    });

    it('closes the change password modal when the changePasswordState goes from success to idle', () => {
      const component = getMountedComponent({ canChangePassword: true });
      const button = component.find('button');
      button.simulate('click');
      const userDetails = component.find('#change-password');
      userDetails.simulate('click');
      expect(component.find(ChangePasswordModal)).toExist();

      // rerender UserMenu with new props
      component.setProps({ changePasswordStatus: 'success' });
      expect(component.find(ChangePasswordModal)).toExist();

      component.setProps({ changePasswordStatus: 'idle' });
      component.update();
      expect(component.find(ChangePasswordModal)).not.toExist();

      component.unmount();
    });
  });
});
