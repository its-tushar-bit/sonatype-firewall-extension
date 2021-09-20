/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers, NxErrorAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import EditLdapAuth, { methods } from '../../../../../main/frontend/configuration/ldap/connection/EditLdapAuth';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('EditLdapAuth', () => {
  let getShallowComponent;

  const setMethodMock = jasmine.createSpy('setMethod');
  const setSaslRealmMock = jasmine.createSpy('setSaslRealm');
  const setUsernameMock = jasmine.createSpy('setUsername');
  const setPasswordMock = jasmine.createSpy('setPassword');

  const minimalProps = {
    mustReenterPassword: false,
    authenticationMethod: 'NONE',
    systemUsername: initUserInput(''),
    systemPassword: initUserInput(''),
    saslRealm: initUserInput(''),
    setMethod: setMethodMock,
    setSaslRealm: setSaslRealmMock,
    setUsername: setUsernameMock,
    setPassword: setPasswordMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapAuth, minimalProps);
  });

  describe('on render', () => {
    describe('mustReenterPassword', () => {
      it('renders NxErrorAlert if mustReenterPassword === true and auth is not default', () => {
        const component = getShallowComponent({ authenticationMethod: methods[1], mustReenterPassword: true });
        const alert = component.find(NxErrorAlert);

        expect(alert).toExist();
        expect(alert.text()).toBe(
          'The password must be given when updating the hostname or port for a connection that uses authentication.'
        );
      });

      it('does not render NxErrorAlert if mustReenterPassword !== true and auth is not default', () => {
        const component = getShallowComponent({ authenticationMethod: methods[1], mustReenterPassword: false });
        const alert = component.find(NxErrorAlert);

        expect(alert.length).toBe(0);
      });
    });

    describe('method', () => {
      it('renders select element with correct method value', () => {
        const component = getShallowComponent({ method: 'NONE' });
        const select = component.find('#method-selector');

        expect(select).toHaveProp('value', methods[0]);
      });

      it('renders all method options', () => {
        const component = getShallowComponent();
        const options = component.find('#method-selector > option');

        expect(options.length).toBe(4);

        expect(options.at(0).text()).toBe(methods[0]);
        expect(options.at(1).text()).toBe(methods[1]);
        expect(options.at(2).text()).toBe(methods[2]);
        expect(options.at(3).text()).toBe(methods[3]);
      });

      it('calls setMethod when option changes', function () {
        const method = 'SIMPLE';
        const component = getShallowComponent();
        const select = component.find('#method-selector');

        select.simulate('change', { target: { value: method } });
        expect(setMethodMock).toHaveBeenCalledWith(method);
      });

      describe('auth inputs', () => {
        const ids = ['#method-selector', '#saslRealm', '#username', '#password'];

        it('shows only method input if authenticationMethod === NONE', () => {
          const component = getShallowComponent();
          ids.forEach((id) => {
            if (id === ids[0]) {
              expect(component.find(id)).toExist();
            } else {
              expect(component.find(id).length).toBe(0);
            }
          });
        });

        it('shows full number of inputs if authenticationMethod !== NONE', () => {
          const component = getShallowComponent({ authenticationMethod: methods[1] });

          ids.forEach((id) => {
            expect(component.find(id)).toExist();
          });
        });
      });
    });

    describe('inputs', () => {
      describe('saslRealm text input onChange handler', () => {
        it('calls saslRealm action', () => {
          const component = getShallowComponent({ authenticationMethod: methods[1] });
          const saslRealmInput = component.find('#saslRealm');

          saslRealmInput.simulate('change', 'text');
          expect(setSaslRealmMock).toHaveBeenCalledWith('text');
        });
      });

      describe('username text input onChange handler', () => {
        it('calls setUsername action', () => {
          const component = getShallowComponent({ authenticationMethod: methods[1] });
          const usernameInput = component.find('#username');

          usernameInput.simulate('change', 'name');
          expect(setUsernameMock).toHaveBeenCalledWith('name');
        });
      });

      describe('password text input onChange handler', () => {
        it('calls setPassword action', () => {
          const component = getShallowComponent({ authenticationMethod: methods[1] });
          const passwordInput = component.find('#password');

          passwordInput.simulate('change', 'some pass');
          expect(setPasswordMock).toHaveBeenCalledWith('some pass');
        });
      });
    });
  });
});
