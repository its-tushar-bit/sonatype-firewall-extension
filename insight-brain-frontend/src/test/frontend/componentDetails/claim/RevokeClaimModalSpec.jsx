/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxForm, NxModal, NxWarningAlert } from '@sonatype/react-shared-components';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import RevokeClaimModal from 'MainRoot/componentDetails/claim/RevokeClaimModal';

describe('RevokeClaimModal', () => {
  let getShallow, containerModal;

  const revokeMock = jasmine.createSpy('removeServer');
  const closeModalMock = jasmine.createSpy('closeModal');

  const minimalProps = {
    revoke: revokeMock,
    closeModal: closeModalMock,
    revokeError: null,
  };

  beforeEach(() => {
    getShallow = enzymeUtils.getShallowComponent(RevokeClaimModal, minimalProps);
    containerModal = document.createElement('div');
    document.body.appendChild(containerModal);
  });

  afterEach(() => {
    if (containerModal) {
      document.body.removeChild(containerModal);
      containerModal = null;
    }
  });

  it('renders a component with NxModal', () => {
    expect(getShallow().find(NxModal)).toExist();
  });

  it('calls revoke when submitted', () => {
    const modal = getShallow().find(NxForm);

    modal.simulate('submit');

    expect(revokeMock).toHaveBeenCalledTimes(1);
  });

  it('calls closeModal on cancel', () => {
    const modal = getShallow().find(NxForm);
    modal.simulate('cancel');

    expect(closeModalMock).toHaveBeenCalled();
  });

  it('renders remove alert message', () => {
    const alert = getShallow().find(NxWarningAlert);

    expect(alert).toExist();
    expect(alert).toHaveText(
      'Are you sure you want to revoke the claim on this component? This change will not be reflected until a new policy evaluation is triggered.'
    );
  });

  it('renders error alert on unsuccessful submit', () => {
    const modal = getShallow({ revokeError: 'error' }).find(NxForm);

    expect(modal).toHaveProp('submitError', 'error');
  });
});
