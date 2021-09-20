/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxForm } from '@sonatype/react-shared-components';

import * as enzymeUtils from '../../../frontend/enzymeUtils';
import * as routerContext from '../../../../main/frontend/react/RouterStateContext';
import withLdapHeader from '../../../../main/frontend/configuration/ldap/withLdapHeader';
import LdapRemoveServerModal from '../../../../main/frontend/configuration/ldap/LdapRemoveServerModal';

describe('withLdapHeader', () => {
  function Wrapped() {
    return <div>some inner content</div>;
  }

  let getShallowComponent;

  const stateGoSpy = jasmine.createSpy('stateGo');
  const maybeLoadEditPageMock = jasmine.createSpy('maybeLoadEditPage');
  const saveConnectionMock = jasmine.createSpy('saveConnection');
  const resetAllNotificationsMock = jasmine.createSpy('resetAllNotifications');
  const resetFormMock = jasmine.createSpy('resetForm');
  const getSpy = jasmine.createSpy('get').and.returnValue({ data: { title: 'some title' } });

  const minimalWrapperProps = {
    WrappedComponent: Wrapped,
    data: { formId: 'form-id' },
  };
  const minimalProps = {
    stateGo: stateGoSpy,
    inputFields: {
      serverName: {},
    },
    maybeLoadEditPage: maybeLoadEditPageMock,
    saveConnection: saveConnectionMock,
    resetAllNotifications: resetAllNotificationsMock,
    resetForm: resetFormMock,
    router: {
      currentParams: { ldapId: '202' },
      currentState: { name: 'edit-ldap-connection' },
    },
  };

  beforeEach(() => {
    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: () => {},
      get: getSpy,
    });

    getShallowComponent = enzymeUtils.getShallowComponent(withLdapHeader(Wrapped, { formId: 'ldap-edit-connection' }), {
      ...minimalProps,
      ...minimalWrapperProps,
    });
  });

  describe('on initial load', () => {
    it('calls loadEditConnectionPage if current tab is edit-ldap-connection', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(
        withLdapHeader(Wrapped, { formId: 'ldap-edit-connection' }),
        { ...minimalProps, ...minimalWrapperProps }
      );
      const component = getMountedComponent();

      expect(maybeLoadEditPageMock).toHaveBeenCalledWith({ ldapId: '202', currentTab: 'edit-ldap-connection' });
      component.unmount();
    });

    it('calls loadEditUsermappingPage if current tab is edit-ldap-usermapping', () => {
      const router = {
        currentParams: { ldapId: '203' },
        currentState: { name: 'edit-ldap-usermapping' },
      };
      const getMountedComponent = enzymeUtils.getMountedComponent(
        withLdapHeader(Wrapped, { formId: 'edit-ldap-usermapping' }),
        { ...minimalProps, ...minimalWrapperProps, router }
      );
      const component = getMountedComponent();

      expect(maybeLoadEditPageMock).toHaveBeenCalledWith({ ldapId: '203', currentTab: 'edit-ldap-usermapping' });
      component.unmount();
    });
  });

  describe('on render', () => {
    it('shows all required components', () => {
      const component = getShallowComponent();
      const tabs = component.find('LdapTabs');
      const serverName = component.find('LdapServerNameForm');
      const wrapped = component.find('Wrapped');
      const removeBtn = component.find('#remove-server');

      expect(tabs).toExist();
      expect(serverName).toExist();
      expect(wrapped).toExist();
      expect(removeBtn).toExist();
    });

    describe('validationErrors', () => {
      it('is null if form was changed and changes are valid', () => {
        const component = getShallowComponent({
          isDirty: true,
          validationError: null,
        });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', null);
      });

      it('has "There are no changes to save" error if form was not changed', () => {
        const component = getShallowComponent({ isDirty: false });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'There are no changes to save');
      });

      it('has "Unable to save" error if form was not changed', () => {
        const component = getShallowComponent({
          isDirty: true,
          validationError: 'Unable to save: fields with invalid or missing data',
        });
        const form = component.find(NxForm);

        expect(form).toHaveProp('validationErrors', 'Unable to save: fields with invalid or missing data');
      });

      it('has "Password must be updated" error if form was not changed', () => {
        const component = getShallowComponent({
          isDirty: true,
          mustReenterPassword: true,
        });
        const form = component.find(NxForm);

        expect(form).toHaveProp(
          'validationErrors',
          'The password must be given when updating the hostname or port for a connection that uses authentication.'
        );
      });
    });

    describe('on form submit', () => {
      it('calls save when the form is submitted if it"s dirty', function () {
        const component = getShallowComponent({
          isDirty: true,
          validationError: null,
          mustReenterPassword: false,
        });
        const form = component.find(NxForm);

        form.simulate('submit');

        expect(saveConnectionMock).toHaveBeenCalled();
      });
    });

    describe('on form cancel', () => {
      it('navigates to users page', () => {
        const component = getShallowComponent({
          isDirty: false,
          validationError: null,
        });

        const form = component.find(NxForm);

        form.simulate('cancel');

        expect(stateGoSpy).toHaveBeenCalledWith('ldap-list');
        expect(resetFormMock).toHaveBeenCalledWith();
      });
    });

    describe('remove button', () => {
      it('shows remove modal when clicked', () => {
        const component = getShallowComponent();
        const removeButton = component.find('#remove-server');

        removeButton.simulate('click');

        expect(component.find(LdapRemoveServerModal)).toExist();
      });
    });
  });
});
