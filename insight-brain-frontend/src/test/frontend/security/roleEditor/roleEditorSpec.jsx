/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxStatefulForm, NxModal } from '@sonatype/react-shared-components';
import RoleEditor from 'MainRoot/security/roleEditor/RoleEditor';
import * as enzymeUtils from '../../enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

describe('RoleEditor', () => {
  let getShallowComponent,
    minimalProps,
    mockLoad,
    mockLoadRoles,
    mockSubmit,
    stateMock,
    stateGetSpy,
    mockDelete,
    stateGoSpy;

  beforeEach(() => {
    stateGoSpy = jasmine.createSpy('stateGo');
    mockLoad = jasmine.createSpy('load');
    mockLoadRoles = jasmine.createSpy('loadRoles');
    mockSubmit = jasmine.createSpy('save');
    stateGetSpy = jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some title' } });
    mockDelete = jasmine.createSpy('deleteRole');
    stateMock = {
      get: stateGetSpy,
      href: () => {},
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(stateMock);
    minimalProps = {
      load: mockLoad,
      deleteRole: mockDelete,
      roles: [],
      formState: {
        name: {
          trimmedValue: '',
        },
        description: {},
        permissionCategories: [],
      },
      loadRoles: mockLoadRoles,
      save: mockSubmit,
      router: {
        currentState: { name: 'addRole' },
        currentParams: { roleId: null },
      },
      deleteSaving: false,
      deleteSuccess: false,
      stateGo: stateGoSpy,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(RoleEditor, minimalProps);
  });

  it('renders a component with the nx-page-main class', function () {
    expect(getShallowComponent().find('.nx-page-main')).toExist();
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallowComponent();
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'rolesList');
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const props = {
        ...minimalProps,
        load: mockLoad,
        roles: [{ name: '' }],
        creationMode: true,
      };
      const getMountedComponent = enzymeUtils.getMountedComponent(RoleEditor, props);
      const mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalledWith(null);
      mountedComponent.unmount();
    });

    it('calls load with a role id', () => {
      const roleId = 'ROLE-ID';
      const props = {
        ...minimalProps,
        roleId,
        router: {
          currentState: { name: 'editRole' },
          currentParams: { roleId: 'ROLE-ID' },
        },
      };
      const getMountedComponent = enzymeUtils.getMountedComponent(RoleEditor, props);
      const mountedComponent = getMountedComponent();
      expect(mockLoad).toHaveBeenCalledWith(roleId);
      mountedComponent.unmount();
    });

    it('calls loadRoles when roles array is empty', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(RoleEditor, minimalProps);
      const mountedComponent = getMountedComponent();

      expect(mockLoadRoles).toHaveBeenCalled();
      mountedComponent.unmount();
    });
  });

  describe('cancel button', () => {
    it('redirect to roles list', () => {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxStatefulForm);
      form.simulate('cancel');
      expect(stateGoSpy).toHaveBeenCalledWith('rolesList');
    });
  });

  describe('on form submit', () => {
    it('calls update when form is submitted and it is dirty', () => {
      const shallowComponent = getShallowComponent({ isDirty: true });
      const form = shallowComponent.find(NxStatefulForm);
      form.simulate('submit');
      expect(mockSubmit).toHaveBeenCalledTimes(1);
    });

    it('has a validation error when the form is not dirty', () => {
      const props = {
        isDirty: false,
      };
      const shallowComponent = getShallowComponent(props);
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'There are no changes to update.');
    });

    it('has a validation error when the role name already exists', () => {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        roles: [{ name: 'role' }],
        formState: {
          name: { trimmedValue: 'role', validationErrors: ['Role name already exits'] },
          description: { trimmedValue: 'role description' },
          permissionCategories: [],
        },
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'Unable to submit: fields with invalid or missing data.');
    });

    it('has a validation error when the role name is empty', () => {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        formState: {
          name: { trimmedValue: '', validationErrors: ['Must be non-empty'] },
          description: { trimmedValue: 'role description' },
          permissionCategories: [],
        },
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'Unable to submit: fields with invalid or missing data.');
    });

    it('has a validation error when the role description is empty', () => {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        formState: {
          name: { trimmedValue: 'rol' },
          description: { trimmedValue: '', validationErrors: 'Must be non-empty' },
          permissionCategories: [],
        },
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'Unable to submit: fields with invalid or missing data.');
    });

    it('does not have validation error', () => {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        formState: {
          name: { trimmedValue: 'role' },
          description: { trimmedValue: 'role description' },
          permissionCategories: [],
        },
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', null);
    });

    it('has a validation error when the user has insufficient permissions', () => {
      const shallowComponent = getShallowComponent({
        readonly: true,
        builtIn: false,
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'You have insufficient permissions to edit this role.');
    });

    it('has a validation error when the role is built in', () => {
      const shallowComponent = getShallowComponent({
        readonly: true,
        builtIn: true,
      });
      const form = shallowComponent.find(NxStatefulForm);
      expect(form).toHaveProp('validationErrors', 'This role cannot be edited.');
    });
  });

  describe('delete dialog', () => {
    let shallowComponent, router;

    beforeEach(() => {
      router = {
        currentParams: {
          roleId: 'ROLE-ID',
        },
        currentState: {
          name: 'editRole',
        },
      };
      shallowComponent = getShallowComponent({ router });
      const deleteBtn = shallowComponent.find('#delete-role');
      deleteBtn.simulate('click');
    });

    it('sets showModal to true', () => {
      const modal = shallowComponent.find(NxModal);
      expect(modal).toExist();
    });

    it('has deleteError prop', () => {
      const deleteError = 'some error happened';
      const shallowComponent = getShallowComponent({ router, deleteError });
      const deleteBtn = shallowComponent.find('#delete-role');
      deleteBtn.simulate('click');
      const dialog = shallowComponent.find(NxModal);
      const formInDialog = dialog.find(NxStatefulForm);
      expect(formInDialog).toHaveProp('submitError', deleteError);
    });

    it('calls delete on form submit', () => {
      const modal = shallowComponent.find(NxModal);
      const form = modal.find(NxStatefulForm);
      form.simulate('submit');
      expect(mockDelete).toHaveBeenCalledTimes(1);
    });

    it('has submitMaskState prop in modal form as true', () => {
      shallowComponent = getShallowComponent({ deleteMaskState: true, router });
      const deleteBtn = shallowComponent.find('#delete-role');
      deleteBtn.simulate('click');
      const modal = shallowComponent.find(NxModal);
      const form = modal.find(NxStatefulForm);
      expect(form).toHaveProp('submitMaskState', true);
    });

    it('has submitMaskState prop in modal from as false', () => {
      shallowComponent = getShallowComponent({ router, deleteMaskState: false });
      const deleteBtn = shallowComponent.find('#delete-role');
      deleteBtn.simulate('click');
      const modal = shallowComponent.find(NxModal);
      const form = modal.find(NxStatefulForm);
      expect(form).toHaveProp('submitMaskState', false);
    });
  });
});
