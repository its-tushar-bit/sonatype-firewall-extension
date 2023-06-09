/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../enzymeUtils';
import DeleteModal from 'MainRoot/security/users/modals/DeleteModal';

describe('User DeleteModal', () => {
  let getShallowComponent, containerModal;
  const deleteUserMock = jasmine.createSpy('deleteUser');

  const minimalProps = {
    username: 'Aragorn',
    userId: '325sdf',
    deleteUser: deleteUserMock,
    onCancel: () => {},
    deleteError: null,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(DeleteModal, minimalProps);
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

  it('calls deleteUser when submitted', () => {
    const modal = getShallowComponent().find(NxStatefulForm);

    modal.simulate('submit');

    expect(deleteUserMock).toHaveBeenCalledWith(minimalProps.userId);
  });

  it('calls onCancel when canceled', () => {
    const onCancel = jasmine.createSpy('onCancel'),
      modal = getShallowComponent({ onCancel }).find(NxStatefulForm);

    expect(onCancel).not.toHaveBeenCalled();

    modal.simulate('cancel');

    expect(onCancel).toHaveBeenCalled();
  });

  it('renders delete alert message', () => {
    const alert = getShallowComponent().find(NxWarningAlert);

    expect(alert).toExist();
    expect(alert).toHaveText(
      `You are about to permanently remove ${minimalProps.username}. This action cannot be undone.`
    );
  });

  it('renders error alert on unsuccessful submit', () => {
    const modal = getShallowComponent({ deleteError: 'error' }).find(NxStatefulForm);

    expect(modal).toHaveProp('submitError', 'error');
  });
});
