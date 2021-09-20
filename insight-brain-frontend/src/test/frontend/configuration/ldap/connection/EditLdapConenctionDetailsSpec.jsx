/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import EditLdapConnectionDetails, {
  protocols,
} from '../../../../../main/frontend/configuration/ldap/connection/EditLdapConnectionDetails';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('EditLdapConnectionDetails', () => {
  let getShallowComponent;

  const setProtocolMock = jasmine.createSpy('setProtocol');
  const setHostnameMock = jasmine.createSpy('setHostname');
  const setPortMock = jasmine.createSpy('setPort');
  const setSearchBaseMock = jasmine.createSpy('setSearchBase');
  const setReferralIgnoredMock = jasmine.createSpy('setReferralIgnored');
  const handleNumberInputMock = jasmine.createSpy('handleNumberInput');

  const minimalProps = {
    protocol: 'LDAP',
    hostname: initUserInput(''),
    port: initUserInput(''),
    searchBase: initUserInput(''),
    referralIgnored: false,
    setProtocol: setProtocolMock,
    setHostname: setHostnameMock,
    setPort: setPortMock,
    setSearchBase: setSearchBaseMock,
    setReferralIgnored: setReferralIgnoredMock,
    handleNumberInput: handleNumberInputMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapConnectionDetails, minimalProps);
  });

  describe('on render', () => {
    describe('hostname text input onChange handler', () => {
      it('calls hostname action', () => {
        const component = getShallowComponent();
        const hostnameInput = component.find('#hostname');

        hostnameInput.simulate('change', 'hostname.com');
        expect(setHostnameMock).toHaveBeenCalledWith('hostname.com');
      });
    });

    describe('port text input onChange handler', () => {
      it('calls port action', () => {
        const component = getShallowComponent();
        const portInput = component.find('#port');

        portInput.simulate('change', '222');
        expect(handleNumberInputMock).toHaveBeenCalledWith('222', setPortMock);
      });
    });

    describe('on toggle change', () => {
      it('calls setReferralIgnored when toggle value is changed', () => {
        const component = getShallowComponent();
        const toggle = component.find('#ignore-referrals-toggle');

        toggle.simulate('change');

        expect(setReferralIgnoredMock).toHaveBeenCalled();
      });

      it('calls setReferralIgnored when toggle value is changed', () => {
        let component = getShallowComponent();

        expect(component.find('#ignore-referrals-toggle')).toHaveProp('isChecked', false);

        component = getShallowComponent({ referralIgnored: true });

        expect(component.find('#ignore-referrals-toggle')).toHaveProp('isChecked', true);
      });
    });

    describe('protocol', () => {
      it('renders select element with correct protocol value', () => {
        const component = getShallowComponent({ protocol: 'LDAPS' });
        const select = component.find('#protocol-selector');

        expect(select).toHaveProp('value', protocols[1]);
      });

      it('renders all protocol options', () => {
        const component = getShallowComponent();
        const options = component.find('#protocol-selector > option');

        expect(options.length).toBe(2);

        expect(options.at(0).text()).toBe(protocols[0]);
        expect(options.at(1).text()).toBe(protocols[1]);
      });

      it('calls setProtocol when option changes', function () {
        const protocol = 'LDAPS';
        const component = getShallowComponent();
        const select = component.find('#protocol-selector');

        select.simulate('change', { target: { value: protocol } });
        expect(setProtocolMock).toHaveBeenCalledWith(protocol);
      });
    });
  });
});
