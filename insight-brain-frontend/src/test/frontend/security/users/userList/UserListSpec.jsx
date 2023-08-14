/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import UserList from '../../../../../main/frontend/security/users/userList/UserList';
import UserListItem from '../../../../../main/frontend/security/users/userList/UserListItem';
import LoadWrapper from '../../../../../main/frontend/react/LoadWrapper';

describe('UserList', () => {
  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const loadListPageMock = jasmine.createSpy('loadListPage');

  const minimalProps = {
    stateGo: stateGoSpy,
    loadListPage: loadListPageMock,
    users: [
      {
        id: 'userIdOne',
        username: 'john_doe',
        firstName: 'John',
        lastName: 'Doe',
      },
      {
        id: 'userIdTwo',
        username: 'admin',
        firstName: 'Admin',
        lastName: 'BuiltIn',
      },
    ],
    currentUsername: 'admin',
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(UserList, minimalProps);
  });

  describe('on render', () => {
    it('renders a component with the "user-management" id', () => {
      expect(getShallowComponent().find('#user-management')).toExist();
    });

    it('renders passed users', () => {
      const component = getShallowComponent();
      const userItems = component.find(UserListItem);

      expect(userItems.length).toBe(2);
    });

    it('sets the UserListItems as editable if the tenantMode is not multi-tenant', function () {
      const defaultComponent = getShallowComponent(),
        defaultUserItems = defaultComponent.find(UserListItem);

      defaultUserItems.forEach((item) => {
        expect(item).toHaveProp('editable', true);
      });

      const singleTenantComponent = getShallowComponent({ tenantMode: 'single-tenant' }),
        singleTenantUserItems = singleTenantComponent.find(UserListItem);

      singleTenantUserItems.forEach((item) => {
        expect(item).toHaveProp('editable', true);
      });

      const multiTenantComponent = getShallowComponent({ tenantMode: 'multi-tenant' }),
        multiTenantUserItems = multiTenantComponent.find(UserListItem);

      multiTenantUserItems.forEach((item) => {
        expect(item).toHaveProp('editable', false);
      });
    });
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(UserList, minimalProps);
      const component = getMountedComponent();

      expect(loadListPageMock).toHaveBeenCalled();
      component.unmount();
    });

    describe('on load error', () => {
      it('triggers load method on retry handler', () => {
        const component = getShallowComponent({ loadError: 'error' });
        const loadWrapper = component.find(LoadWrapper);

        expect(loadWrapper).toExist();
        expect(loadWrapper).toHaveProp('retryHandler', loadListPageMock);
      });
    });
  });

  describe('on create user click', () => {
    it('links to the createUser page', function () {
      const component = getShallowComponent();
      const createUser = component.find('#create-user');

      createUser.simulate('click');

      expect(stateGoSpy).toHaveBeenCalledWith('createUser');
    });
  });

  it('renders an "Invite User" button when the multi-tenant feature flag is set', function () {
    const component = getShallowComponent({ tenantMode: 'multi-tenant' });
    const inviteBtn = component.find('#invite-user');
    expect(inviteBtn).toExist();
    expect(inviteBtn.text()).toBe('Invite User');
  });
});
