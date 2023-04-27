/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import ProxyConfig from '../../../../main/frontend/configuration/proxy/ProxyConfig';

describe('ProxyConfig', () => {
  let getShallowComponent, getMountedComponent;

  const loadMock = jasmine.createSpy('load');
  const loadLicencedMock = jasmine.createSpy('loadLicenced');
  const setHostnameMock = jasmine.createSpy('setHostname');
  const setPortMock = jasmine.createSpy('setPort');
  const setUsernameMock = jasmine.createSpy('setUsername');
  const setPasswordMock = jasmine.createSpy('setPassword');
  const setExcludeHostsMock = jasmine.createSpy('setExcludeHosts');
  const resetFormMock = jasmine.createSpy('resetForm');
  const stateGoSpy = jasmine.createSpy('stateGo');

  const minimalProps = {
    load: loadMock,
    loadLicenced: loadLicencedMock,
    setHostname: setHostnameMock,
    setPort: setPortMock,
    setUsername: setUsernameMock,
    setPassword: setPasswordMock,
    setExcludeHosts: setExcludeHostsMock,
    resetForm: resetFormMock,
    stateGo: stateGoSpy,
    licensed: true,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(ProxyConfig, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(ProxyConfig, minimalProps);
  });

  describe('on initial load', () => {
    it('calls load', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(ProxyConfig, minimalProps);
      const component = getMountedComponent();

      expect(loadMock).toHaveBeenCalled();
      expect(loadLicencedMock).toHaveBeenCalled();
      component.unmount();
    });
  });

  describe('on render', () => {
    describe('validationErrors', () => {
      it('is "There are no changes to update." if form was not changed', () => {
        const component = getShallowComponent({ isDirty: false });
        const form = component.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'There are no changes to update.');
      });

      it('is null if form has all required data and valid data', () => {
        const component = getShallowComponent({
          isDirty: true,
          hasAllRequiredData: true,
          isValid: true,
          mustReenterPassword: false,
        });
        const form = component.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', null);
      });

      it('is "Hostname and Port are required details." if form does not have all required data when adding proxy', () => {
        const component = getShallowComponent({
          isDirty: true,
          hasAllRequiredData: false,
          isValid: true,
        });
        const form = component.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'Hostname and Port are required details.');
      });

      it('is "Hostname and Port are required details." if form has all required data but it is not valid when adding proxy', () => {
        const component = getShallowComponent({
          isDirty: true,
          hasAllRequiredData: true,
          isValid: false,
        });
        const form = component.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'Hostname and Port are required details.');
      });

      it('is "Password must be provided when updating Hostname or Port." if form has all required data but it is not valid when editing proxy', () => {
        const component = getShallowComponent({
          isDirty: true,
          hasAllRequiredData: true,
          isValid: true,
          mustReenterPassword: true,
        });
        const form = component.find(NxStatefulForm);

        expect(form).toHaveProp('validationErrors', 'Password must be provided when updating Hostname or Port.');
      });
    });

    describe('hostName text input onChange handler', () => {
      it('calls hostName action', () => {
        const component = getShallowComponent();
        const hostNameInput = component.find('#proxy-config-hostname');

        hostNameInput.simulate('change', 'a.host');
        expect(setHostnameMock).toHaveBeenCalledWith('a.host');
      });
    });

    describe('port text input onChange handler', () => {
      it('calls port action', () => {
        const component = getShallowComponent();
        const portInput = component.find('#proxy-config-port');

        portInput.simulate('change', '8080');
        expect(setPortMock).toHaveBeenCalledWith('8080');
      });
    });

    describe('username text input onChange handler', () => {
      it('calls username action', () => {
        const component = getShallowComponent();
        const usernameInput = component.find('#proxy-config-username');

        usernameInput.simulate('change', 'John');
        expect(setUsernameMock).toHaveBeenCalledWith('John');
      });
    });

    describe('password text input behaviour', () => {
      it('calls password action', () => {
        const component = getShallowComponent();
        const passwordInput = component.find('#proxy-config-password');

        passwordInput.simulate('change', 'John');
        expect(setPasswordMock).toHaveBeenCalledWith('John');
      });

      it('shows label if there is a need to re-enter password', () => {
        const component = getMountedComponent({
          hasAllRequiredData: true,
          mustReenterPassword: true,
        });
        const subLabel = component.find('.nx-sub-label').at(0);
        expect(subLabel.text()).toBe('Must be re-entered when Hostname or Port is modified.');
      });
    });

    describe('exclude hosts text input onChange handler', () => {
      it('calls exclude hosts action', () => {
        const component = getShallowComponent();
        const excludedInput = component.find('#proxy-config-exclude-hosts');

        excludedInput.simulate('change', 'first,second');
        expect(setExcludeHostsMock).toHaveBeenCalledWith('first,second');
      });
    });
  });
});
