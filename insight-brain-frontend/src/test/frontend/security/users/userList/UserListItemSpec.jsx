/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import * as routerContext from '../../../../../main/frontend/react/RouterStateContext';
import UserListItem from '../../../../../main/frontend/security/users/userList/UserListItem';

describe('UserListItem', () => {
  let getShallowComponent;
  const hrefSpy = jasmine.createSpy('href');

  const user = {
    id: '201',
    username: 'JohnDoe',
    firstName: 'John',
    lastName: 'Doe',
  };
  const currentUsername = 'admin';

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(UserListItem, { user, currentUsername });
  });

  describe('on user item click', () => {
    it('links to the editUser page with user id', () => {
      const component = getShallowComponent();

      component.simulate('click');

      expect(hrefSpy).toHaveBeenCalledWith('editUser', {
        userId: '201',
      });
    });
  });

  describe('current user', () => {
    it('shows "Current User" label', function () {
      const component = getShallowComponent({ currentUsername: 'JohnDoe' });
      const label = component.find('.iq-user-list-item-current');

      expect(label).toHaveText('Current User');
    });
  });
});
