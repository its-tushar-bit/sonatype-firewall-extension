/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../frontend/enzymeUtils';
import LdapRemoveServerModal from '../../../../main/frontend/configuration/ldap/LdapRemoveServerModal';

describe('LdapRemoveServerModal', () => {
  let getShallowComponent, containerModal;

  const removeServerMock = jasmine.createSpy('removeServer');
  const closeModalMock = jasmine.createSpy('closeModal');

  const minimalProps = {
    ldapId: '325sdf',
    removeServer: removeServerMock,
    closeModal: closeModalMock,
    removeError: null,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(LdapRemoveServerModal, minimalProps);
    containerModal = document.createElement('div');
    document.body.appendChild(containerModal);
  });

  afterEach(function () {
    if (containerModal) {
      document.body.removeChild(containerModal);
      containerModal = null;
    }
  });

  it('renders a component with NxModal', () => {
    expect(getShallowComponent().find(NxModal)).toExist();
  });

  it('calls removeServer when submitted', () => {
    const modal = getShallowComponent().find(NxStatefulForm);

    modal.simulate('submit');

    expect(removeServerMock).toHaveBeenCalledWith(minimalProps.ldapId);
  });

  it('calls closeModal on cancel', () => {
    const modal = getShallowComponent().find(NxStatefulForm);
    modal.simulate('cancel');

    expect(closeModalMock).toHaveBeenCalled();
  });

  it('renders remove alert message', () => {
    const alert = getShallowComponent().find(NxWarningAlert);

    expect(alert).toExist();
    expect(alert).toHaveText(
      "Clicking 'delete' will permanently remove this server and all data associated with it, including all data associated with the LDAP users in this configuration. This action cannot be undone. Are you sure you want to delete this server?"
    );
  });

  it('renders error alert on unsuccessful submit', () => {
    const modal = getShallowComponent({ removeError: 'error' }).find(NxStatefulForm);

    expect(modal).toHaveProp('submitError', 'error');
  });
});
