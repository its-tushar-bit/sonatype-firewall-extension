/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import CreateLdap from 'MainRoot/configuration/ldap/CreateLdap';
import LdapServerNameForm from 'MainRoot/configuration/ldap/LdapServerNameForm';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('CreateLdap', () => {
  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const getSpy = jasmine.createSpy('get').and.returnValue({ data: { title: 'some title' } });
  const loadMock = jasmine.createSpy('loadAddPage');
  const saveMock = jasmine.createSpy('saveServerName');
  const resetFormMock = jasmine.createSpy('resetForm');

  const minimalProps = {
    loading: false,
    inputFields: {
      serverName: initUserInput(''),
    },
    stateGo: stateGoSpy,
    loadAddPage: loadMock,
    saveServerName: saveMock,
    resetForm: resetFormMock,
  };

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: () => {},
      get: getSpy,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(CreateLdap, minimalProps);
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallowComponent();
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'ldap-list');
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(CreateLdap, minimalProps);
      const component = getMountedComponent();

      expect(loadMock).toHaveBeenCalled();
      component.unmount();
    });

    it('resets the form before unmount component', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(CreateLdap, minimalProps);
      const component = getMountedComponent();
      component.unmount();

      expect(resetFormMock).toHaveBeenCalled();
    });

    describe('on load error', () => {
      it('form has doLoad prop', () => {
        const component = getShallowComponent({ loadError: 'error' });
        const form = component.find(NxStatefulForm);

        expect(form).toExist();
        expect(form).toHaveProp('doLoad', loadMock);
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

    describe('on form submit', () => {
      it('calls save when the form is submitted if it"s dirty', function () {
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

        expect(stateGoSpy).toHaveBeenCalledWith('ldap-list');
      });
    });

    it('sends true to autoFocus prop in LdapServerNameForm', () => {
      const shallowComponent = getShallowComponent();
      const ldapServerNameForm = shallowComponent.find(LdapServerNameForm);
      expect(ldapServerNameForm).toHaveProp('autoFocus', true);
    });
  });
});
