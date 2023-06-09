/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import * as routerContext from '../../../../../main/frontend/react/RouterStateContext';
import UserListItem from '../../../../../main/frontend/security/users/userList/UserListItem';
import DeleteModal from 'MainRoot/security/users/modals/DeleteModal';
import { NxButton, NxList } from '@sonatype/react-shared-components';

describe('UserListItem', () => {
  let getShallowComponent, getMountedComponent, hrefMock;

  const user = {
    id: '201',
    username: 'JohnnyDoe',
    firstName: 'John',
    lastName: 'Doe',
  };
  const currentUsername = 'admin';
  const minimalProps = {
    user,
    currentUsername,
    editable: true,
  };

  beforeEach(() => {
    hrefMock = jasmine.createSpy('href').and.returnValue('#/userEdit');
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefMock,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(UserListItem, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(UserListItem, minimalProps);
  });

  describe('when editable', function () {
    it('renders an NxList.LinkItem that links to the edit page', function () {
      const component = getShallowComponent();
      expect(component).toMatchSelector(NxList.LinkItem);
      expect(component).toHaveProp('href', '#/userEdit');

      expect(hrefMock).toHaveBeenCalledWith('editUser', { userId: '201' });
    });

    it('renders the username followed by the first and last name in parentheses', function () {
      expect(getMountedComponent()).toHaveText('JohnnyDoe (John Doe)');
    });

    it('shows "Current User" label on the current user', function () {
      const component = getMountedComponent({ currentUsername: 'JohnnyDoe' });

      expect(component).toIncludeText('Current User');
    });

    it('does not render a delete action button', function () {
      const component = getShallowComponent(),
        button = component.find(NxButton);

      expect(button).not.toExist();
    });
  });

  describe('when not editable', function () {
    let getShallowComponent, getMountedComponent;

    const nonEditableMinimalProps = {
      ...minimalProps,
      editable: false,
    };

    beforeEach(function () {
      getShallowComponent = enzymeUtils.getShallowComponent(UserListItem, nonEditableMinimalProps);
      getMountedComponent = enzymeUtils.getMountedComponent(UserListItem, nonEditableMinimalProps);
    });

    it('renders an NxList.Item', function () {
      const component = getShallowComponent();
      expect(component).toMatchSelector(NxList.Item);
      expect(hrefMock).not.toHaveBeenCalled();
    });

    it('renders the username followed by the first and last name in parentheses', function () {
      expect(getMountedComponent()).toHaveText('JohnnyDoe (John Doe)');
    });

    it('renders a delete action button', function () {
      const component = getShallowComponent(),
        button = component.find(NxButton);

      expect(button).toHaveProp('title', 'Delete user');
      expect(button).not.toHaveClassName('disabled');
    });

    it('renders a DeleteModal when the delete button is clicked', function () {
      const deleteUser = jasmine.createSpy('deleteUser'),
        component = getShallowComponent({
          deleteUser,
          deleteError: 'asdf',
          deleteMaskState: true,
        }),
        button = component.find(NxButton);

      expect(component.find(DeleteModal)).not.toExist();

      button.simulate('click');

      const modal = component.find(DeleteModal);
      expect(modal).toExist();
      expect(modal).toHaveProp('userId', '201');
      expect(modal).toHaveProp('username', 'JohnnyDoe');
      expect(modal).toHaveProp('deleteUser', deleteUser);
      expect(modal).toHaveProp('deleteError', 'asdf');
      expect(modal).toHaveProp('deleteMaskState', true);
    });

    it("removes the DeleteModal when the modal's onCancel fires", function () {
      const deleteUser = jasmine.createSpy('deleteUser'),
        component = getShallowComponent({
          deleteUser,
          deleteError: 'asdf',
          deleteMaskState: true,
        }),
        button = component.find(NxButton);

      expect(component.find(DeleteModal)).not.toExist();

      button.simulate('click');

      const modal = component.find(DeleteModal);

      modal.simulate('cancel');

      expect(component.find(DeleteModal)).not.toExist();
    });

    it('shows "Current User" label on the current user', function () {
      const component = getMountedComponent({ currentUsername: 'JohnnyDoe' });

      expect(component).toIncludeText('Current User');
    });

    it('disables the delete button (by classname) on the current user and adds a different tooltip', function () {
      const component = getMountedComponent({ currentUsername: 'JohnnyDoe' }),
        button = component.find(NxButton);

      expect(button).toHaveClassName('disabled');
      expect(button).toHaveProp('title', 'Current user cannot be deleted');

      button.simulate('click');

      expect(component.find(DeleteModal)).not.toExist();
    });
  });
});
