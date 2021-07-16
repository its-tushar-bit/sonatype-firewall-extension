/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../enzymeUtils';
import ResetPasswordModal from '../../../../../../main/frontend/security/users/userConfiguration/modals/ResetPasswordModal';

describe('User ResetPasswordModal', () => {
  let getShallowComponent, containerModal;
  const resetPasswordMock = jasmine.createSpy('resetPassword');
  const setModeMock = jasmine.createSpy('setMode');

  const minimalProps = {
    username: 'Aragorn',
    userId: '325sdf',
    resetPassword: resetPasswordMock,
    setMode: setModeMock,
    resetError: null,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(ResetPasswordModal, minimalProps);
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

  it('calls resetPassword when submitted', () => {
    const modal = getShallowComponent().find(NxForm);

    modal.simulate('submit');

    expect(resetPasswordMock).toHaveBeenCalledWith(minimalProps.userId, minimalProps.username);
  });

  it('calls setMode with DEFAULT mode when canceled', () => {
    const modal = getShallowComponent().find(NxForm);
    modal.simulate('cancel');

    expect(setModeMock).toHaveBeenCalledWith('');
  });

  it('renders reset alert message', () => {
    const alert = getShallowComponent().find(NxWarningAlert);

    expect(alert).toExist();
    expect(alert).toHaveText(
      `Are you sure you want to reset the password for ${minimalProps.username}? This action cannot be undone.`
    );
  });

  it('renders error alert on unsuccessful submit', () => {
    const modal = getShallowComponent({ resetError: 'error' }).find(NxForm);

    expect(modal).toHaveProp('submitError', 'error');
  });
});
