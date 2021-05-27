/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import RoleList from '../../../../main/frontend/security/roleList/RoleList';
import RoleListItem from '../../../../main/frontend/security/roleList/RoleListItem';
import LoadWrapper from '../../../../main/frontend/react/LoadWrapper';

describe('RoleList', () => {
  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const loadMock = jasmine.createSpy('load');

  const minimalProps = {
    stateGo: stateGoSpy,
    load: loadMock,
    roles: [
      {
        id: 'roleIdOne',
        name: 'Role Name One',
        description: 'Role Description One',
        builtIn: false,
      },
      {
        id: 'roleIdTwo',
        name: 'Role Name Two',
        description: 'Role Description Two',
        builtIn: true,
      },
    ],
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(RoleList, minimalProps);
  });

  describe('on render', () => {
    it('renders a component with the "role-management" id', () => {
      expect(getShallowComponent().find('#role-management')).toExist();
    });

    it('renders passed roles', () => {
      const component = getShallowComponent();
      const roleItems = component.find(RoleListItem);

      expect(roleItems.length).toBe(2);
    });

    it('renders default message if no custom roles', () => {
      const roles = [
        {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: true,
        },
      ];
      const component = getShallowComponent({ roles });
      const emptyCustomRole = component.find('#custom-roles .nx-list__item');
      expect(emptyCustomRole.text()).toBe(
        'No custom roles defined. Click "Create Role" in the upper right to add one.'
      );
    });
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(RoleList, minimalProps);
      const component = getMountedComponent();

      expect(loadMock).toHaveBeenCalled();
      component.unmount();
    });

    describe('on load error', () => {
      it('triggers load method on retry handler', () => {
        const component = getShallowComponent({ loadError: 'error' });
        const loadWrapper = component.find(LoadWrapper);

        expect(loadWrapper).toExist();
        expect(loadWrapper).toHaveProp('retryHandler', loadMock);
      });
    });
  });

  describe('create role button', () => {
    it('disabled if user has readOnly access', () => {
      const component = getShallowComponent({ readOnly: true });
      const createRole = component.find('#create-role');

      expect(createRole).toHaveProp('disabled', true);
    });

    it('enabled if user has full access', () => {
      const component = getShallowComponent({ readOnly: false });
      const createRole = component.find('#create-role');

      expect(createRole).toHaveProp('disabled', false);
    });
  });

  describe('on create role click', () => {
    it('links to the roles.editor page', function () {
      const component = getShallowComponent();
      const createRole = component.find('#create-role');

      createRole.simulate('click');

      expect(stateGoSpy).toHaveBeenCalledWith('roles.editor', {
        roleId: '_new_',
      });
    });
  });
});
