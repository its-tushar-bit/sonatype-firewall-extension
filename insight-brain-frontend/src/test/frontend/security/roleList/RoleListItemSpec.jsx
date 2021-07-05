/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';
import RoleListItem from '../../../../main/frontend/security/roleList/RoleListItem';

describe('RoleListItem', () => {
  let getShallowComponent;
  const hrefSpy = jasmine.createSpy('href');

  const role = {
    id: 'roleIdOne',
    name: 'Role Name One',
    description: 'Role Description One',
    builtIn: true,
  };

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(RoleListItem, { role });
  });

  describe('on role item click', () => {
    it('links to the editRole page with role id', () => {
      const component = getShallowComponent();

      component.simulate('click');

      expect(hrefSpy).toHaveBeenCalledWith('editRole', {
        roleId: 'roleIdOne',
      });
    });
  });
});
