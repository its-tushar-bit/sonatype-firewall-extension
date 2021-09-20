/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxSuccessAlert, NxErrorAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import EditLdapConnection from '../../../../main/frontend/configuration/ldap/EditLdapConnection';

describe('EditLdapConnection', () => {
  let getShallowComponent;

  const resetAlertMessagesMock = jasmine.createSpy('resetAlertMessages');

  const minimalProps = {
    inputFields: {
      authenticationMethod: 'NONE',
    },
    resetAlertMessages: resetAlertMessagesMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(EditLdapConnection, minimalProps);
  });

  describe('on render', () => {
    it('shows success alert if successMessage prop exists', () => {
      const component = getShallowComponent({ successMessage: 'Success!' });
      const alert = component.find(NxSuccessAlert);

      expect(alert).toHaveText('Success!');
    });

    it('calls resetSuccessAlert on alert close action', () => {
      const component = getShallowComponent({ successMessage: 'Success!' });
      const alert = component.find(NxSuccessAlert);

      expect(alert).toHaveProp('onClose', resetAlertMessagesMock);
    });

    it('shows error alert if testConnectionErrorMessage prop exists', () => {
      const component = getShallowComponent({ testConnectionErrorMessage: 'We are doomed!' });
      const alert = component.find(NxErrorAlert);

      expect(alert).toHaveText('We are doomed!');
    });

    it('calls resetSuccessAlert on alert close action', () => {
      const component = getShallowComponent({ testConnectionErrorMessage: 'We are doomed!' });
      const alert = component.find(NxErrorAlert);

      expect(alert).toHaveProp('onClose', resetAlertMessagesMock);
    });

    it('shows all necessary parts of connection tab', () => {
      const component = getShallowComponent();

      const details = component.find('EditLdapConnectionDetails');
      const auth = component.find('EditLdapAuth');
      const timeouts = component.find('EditLdapTimeouts');

      expect(details).toExist();
      expect(auth).toExist();
      expect(timeouts).toExist();
    });
  });
});
