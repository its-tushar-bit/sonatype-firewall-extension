/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxTextInput, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { getShallowComponent } from '../../../frontend/enzymeUtils';
import LdapServerNameForm from '../../../../main/frontend/configuration/ldap/LdapServerNameForm';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('LdapServerNameForm', () => {
  let getShallow;
  const serServerNameMock = jasmine.createSpy('setServerName');
  const minimalProps = {
    inputFields: {
      serverName: initUserInput(''),
    },
    setServerName: serServerNameMock,
  };

  beforeEach(() => {
    getShallow = getShallowComponent(LdapServerNameForm, minimalProps);
  });

  describe('serverName text input onChange handler', () => {
    it('calls setServerName action', () => {
      const component = getShallow();
      const serverNameInput = component.find('#serverName');

      serverNameInput.simulate('change', 'server');
      expect(serServerNameMock).toHaveBeenCalledWith('server');
    });
  });

  describe('autoFocus', () => {
    it('sets autofocus prop to true', () => {
      const shallowComponent = getShallow({ autoFocus: true });
      const textInput = shallowComponent.find(NxTextInput);
      expect(textInput).toHaveProp('autoFocus', true);
    });

    it('sets autofocus prop to false', () => {
      const shallowComponent = getShallow({ autoFocus: false });
      const textInput = shallowComponent.find(NxTextInput);
      expect(textInput).toHaveProp('autoFocus', false);
    });
  });
});
