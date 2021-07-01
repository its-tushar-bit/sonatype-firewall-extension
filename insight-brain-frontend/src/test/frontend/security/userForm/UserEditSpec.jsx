/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxForm, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';

import UserEdit from '../../../../main/frontend/security/userForm/UserEdit';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('UserEdit', () => {
  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const getSpy = jasmine.createSpy('get').and.returnValue({ data: { title: 'some title' } });
  const resetFormMock = jasmine.createSpy('resetForm');
  const loadUserByIdMock = jasmine.createSpy('loadUserById');
  const setFirstNameMock = jasmine.createSpy('setFirstName');
  const setLastNameMock = jasmine.createSpy('setLastName');
  const setEmailMock = jasmine.createSpy('setEmail');
  const updateMock = jasmine.createSpy('update');

  const minimalProps = {
    loading: false,
    inputFields: {
      firstName: initUserInput(''),
      lastName: initUserInput(''),
      email: initUserInput(''),
    },
    stateGo: stateGoSpy,
    setFirstName: setFirstNameMock,
    setLastName: setLastNameMock,
    setEmail: setEmailMock,
    update: updateMock,
    loadUserById: loadUserByIdMock,
    resetForm: resetFormMock,
    router: {
      currentParams: {
        userId: '325sdf',
      },
    },
  };

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: () => {},
      get: getSpy,
    });
    getShallowComponent = enzymeUtils.getShallowComponent(UserEdit, minimalProps);
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(UserEdit, minimalProps);
      const component = getMountedComponent();

      expect(loadUserByIdMock).toHaveBeenCalled();
      component.unmount();
    });

    it('resets the form before unmount component', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(UserEdit, minimalProps);
      const component = getMountedComponent();
      component.unmount();

      expect(resetFormMock).toHaveBeenCalled();
    });
  });

  describe('on render', () => {
    describe('validationErrors', () => {
      it('is null if form was changed and changes are valid', function () {
        const shallowComponent = getShallowComponent({
          isDirty: true,
          validationError: null,
        });
        const form = shallowComponent.find(NxForm);

        expect(form).toHaveProp('validationErrors', null);
      });

      it('has "There are no changes to update" error if form was not changed', function () {
        const shallowComponent = getShallowComponent({ isDirty: false });
        const form = shallowComponent.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'There are no changes to update');
      });

      it('has "Unable to save" error if form was not changed', function () {
        const shallowComponent = getShallowComponent({
          isDirty: true,
          validationError: 'Unable to save: fields with invalid or missing data',
        });
        const form = shallowComponent.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'Unable to save: fields with invalid or missing data');
      });
    });
  });

  describe('firstName text input onChange handler', () => {
    it('calls setFirstName action', () => {
      const component = getShallowComponent({
        inputFields: {
          firstName: initUserInput('Jane'),
        },
      });
      const firstNameInput = component.find('#firstName');

      firstNameInput.simulate('change', 'John');
      expect(setFirstNameMock).toHaveBeenCalledWith('John');
    });
  });

  describe('lastName text input onChange handler', () => {
    it('calls setLastName action', () => {
      const component = getShallowComponent({
        inputFields: {
          lastName: initUserInput('Up'),
        },
      });
      const lastNameInput = component.find('#lastName');

      lastNameInput.simulate('change', 'Doe');
      expect(setLastNameMock).toHaveBeenCalledWith('Doe');
    });
  });

  describe('email text input onChange handler', () => {
    it('calls setEmail action', () => {
      const component = getShallowComponent({
        inputFields: {
          email: initUserInput('jane@up.com'),
        },
      });
      const emailInput = component.find('#email');

      emailInput.simulate('change', 'john@doe.com');
      expect(setEmailMock).toHaveBeenCalledWith('john@doe.com');
    });
  });

  describe('on form submit', () => {
    it('calls update when the form is submitted if it"s dirty', function () {
      const shallowComponent = getShallowComponent({
        isDirty: true,
        validationError: null,
      });
      const form = shallowComponent.find(NxForm);

      form.simulate('submit');

      expect(updateMock).toHaveBeenCalled();
    });
  });

  describe('on form cancel', () => {
    it('navigates to users page', () => {
      const shallowComponent = getShallowComponent({
        isDirty: false,
        validationError: null,
      });

      const form = shallowComponent.find(NxForm);

      form.simulate('cancel');

      expect(stateGoSpy).toHaveBeenCalledWith('users');
    });
  });
});
