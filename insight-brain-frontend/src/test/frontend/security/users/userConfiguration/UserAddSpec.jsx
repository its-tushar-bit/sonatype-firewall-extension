/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import UserFormAdd from 'MainRoot/security/users/userConfiguration/UserAdd';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('UserAdd', () => {
  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const getSpy = jasmine.createSpy('get').and.returnValue({ data: { title: 'some title' } });
  const loadCreateUserPageMock = jasmine.createSpy('loadCreateUserPage');
  const resetFormMock = jasmine.createSpy('resetForm');
  const setFirstNameMock = jasmine.createSpy('setFirstName');
  const setLastNameMock = jasmine.createSpy('setLastName');
  const setEmailMock = jasmine.createSpy('setEmail');
  const setUserNameMock = jasmine.createSpy('setUserName');
  const setPasswordMock = jasmine.createSpy('setPassword');
  const setMatchPasswordMock = jasmine.createSpy('setMatchPassword');
  const saveMock = jasmine.createSpy('save');

  const minimalProps = {
    loading: false,
    inputFields: {
      firstName: initUserInput(''),
      lastName: initUserInput(''),
      email: initUserInput(''),
      username: initUserInput(''),
      password: initUserInput(''),
      matchPassword: initUserInput(''),
    },
    stateGo: stateGoSpy,
    loadCreateUserPage: loadCreateUserPageMock,
    resetForm: resetFormMock,
    setFirstName: setFirstNameMock,
    setLastName: setLastNameMock,
    setEmail: setEmailMock,
    setUserName: setUserNameMock,
    setPassword: setPasswordMock,
    setMatchPassword: setMatchPasswordMock,
    save: saveMock,
  };

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: () => {},
      get: getSpy,
    });
    getShallowComponent = enzymeUtils.getShallowComponent(UserFormAdd, minimalProps);
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallowComponent();
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'users');
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(UserFormAdd, minimalProps);
      const component = getMountedComponent();

      expect(loadCreateUserPageMock).toHaveBeenCalled();
      component.unmount();
    });

    it('resets the form before unmount component', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(UserFormAdd, minimalProps);
      const component = getMountedComponent();
      component.unmount();

      expect(resetFormMock).toHaveBeenCalled();
    });

    describe('on load error', () => {
      it('form has doLoad prop', () => {
        const component = getShallowComponent({ loadError: 'error' });
        const form = component.find(NxStatefulForm);

        expect(form).toExist();
        expect(form).toHaveProp('doLoad', loadCreateUserPageMock);
      });
    });
  });

  describe('on render', () => {
    describe('validationErrors', () => {
      it('is null if form was changed and changes are valid', function () {
        const shallowComponent = getShallowComponent({
          isDirty: true,
          validationError: null,
        });
        const form = shallowComponent.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', null);
      });

      it('has "There are no changes to save" error if form was not changed', function () {
        const shallowComponent = getShallowComponent({ isDirty: false });
        const form = shallowComponent.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'There are no changes to save');
      });

      it('has "Unable to save" error if form was not changed', function () {
        const shallowComponent = getShallowComponent({
          isDirty: true,
          validationError: 'Unable to save: fields with invalid or missing data',
        });
        const form = shallowComponent.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'Unable to save: fields with invalid or missing data');
      });
    });

    describe('firstName text input onChange handler', () => {
      it('calls setFirstName action', () => {
        const component = getShallowComponent();
        const firstNameInput = component.find('#firstName');

        firstNameInput.simulate('change', 'John');
        expect(setFirstNameMock).toHaveBeenCalledWith('John');
      });
    });

    describe('lastName text input onChange handler', () => {
      it('calls setLastName action', () => {
        const component = getShallowComponent();
        const lastNameInput = component.find('#lastName');

        lastNameInput.simulate('change', 'Doe');
        expect(setLastNameMock).toHaveBeenCalledWith('Doe');
      });
    });

    describe('email text input onChange handler', () => {
      it('calls setEmail action', () => {
        const component = getShallowComponent();
        const emailInput = component.find('#email');

        emailInput.simulate('change', 'john@doe.com');
        expect(setEmailMock).toHaveBeenCalledWith('john@doe.com');
      });
    });

    describe('username text input onChange handler', () => {
      it('calls setUserName action', () => {
        const component = getShallowComponent();
        const usernameInput = component.find('#username');

        usernameInput.simulate('change', 'john_doe');
        expect(setUserNameMock).toHaveBeenCalledWith('john_doe');
      });
    });

    describe('password text input onChange handler', () => {
      it('calls setPassword action', () => {
        const component = getShallowComponent();
        const passwordInput = component.find('#password');

        passwordInput.simulate('change', '1234');
        expect(setPasswordMock).toHaveBeenCalledWith('1234');
      });
    });

    describe('password match text input onChange handler', () => {
      it('calls setMatchPassword action', () => {
        const component = getShallowComponent();
        const passwordMatchInput = component.find('#passwordValidate');

        passwordMatchInput.simulate('change', '1234');
        expect(setMatchPasswordMock).toHaveBeenCalledWith('1234');
      });
    });

    describe('on form submit', () => {
      it('calls update when the form is submitted if it"s dirty', function () {
        const shallowComponent = getShallowComponent({
          isDirty: true,
          validationError: null,
        });
        const form = shallowComponent.find(NxStatefulForm);

        form.simulate('submit');

        expect(saveMock).toHaveBeenCalled();
      });
    });

    describe('on form cancel', () => {
      it('navigates to users page', () => {
        const shallowComponent = getShallowComponent({
          isDirty: false,
          validationError: null,
        });

        const form = shallowComponent.find(NxStatefulForm);

        form.simulate('cancel');

        expect(stateGoSpy).toHaveBeenCalledWith('users');
      });
    });
  });
});
