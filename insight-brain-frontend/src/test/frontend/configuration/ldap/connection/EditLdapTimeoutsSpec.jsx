/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../frontend/enzymeUtils';
import EditLdapTimeouts from '../../../../../main/frontend/configuration/ldap/connection/EditLdapTimeouts';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('EditLdapConnectionDetails', () => {
  let getShallowComponent;

  const setConnectionMock = jasmine.createSpy('setConnection');
  const setRetryDelayMock = jasmine.createSpy('setRetryDelay');
  const handleNumberInputMock = jasmine.createSpy('handleNumberInput');

  const minimalProps = {
    connectionTimeout: initUserInput('30'),
    retryDelay: initUserInput('30'),
    setConnection: setConnectionMock,
    setRetryDelay: setRetryDelayMock,
    handleNumberInput: handleNumberInputMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapTimeouts, minimalProps);
  });

  describe('on render', () => {
    describe('connection text input onChange handler', () => {
      it('calls connection action', () => {
        const component = getShallowComponent();
        const connectionInput = component.find('#connection');

        connectionInput.simulate('change', '42');
        expect(handleNumberInputMock).toHaveBeenCalledWith('42', setConnectionMock);
      });
    });

    describe('retryDelay text input onChange handler', () => {
      it('calls retryDelay action', () => {
        const component = getShallowComponent();
        const retryDelayInput = component.find('#retryDelay');

        retryDelayInput.simulate('change', '41');
        expect(handleNumberInputMock).toHaveBeenCalledWith('41', setRetryDelayMock);
      });
    });
  });
});
