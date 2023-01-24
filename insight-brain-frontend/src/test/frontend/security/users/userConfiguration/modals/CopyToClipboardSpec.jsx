/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxStatefulForm, NxModal } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../../enzymeUtils';
import CopyToClipboard from '../../../../../../main/frontend/security/users/userConfiguration/modals/CopyToClipboard';

describe('User CopyToClipboard', () => {
  let getShallowComponent, containerModal;
  const resetInitialNewPasswordValueMock = jasmine.createSpy('resetInitialNewPasswordValue');
  const setModeMock = jasmine.createSpy('setMode');

  const minimalProps = {
    username: 'Aragorn',
    newPassword: 'weAreDoomed',
    resetInitialNewPasswordValue: resetInitialNewPasswordValueMock,
    setMode: setModeMock,
  };

  beforeEach(() => {
    getShallowComponent = enzymeUtils.getShallowComponent(CopyToClipboard, minimalProps);
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

  it('calls setMode with DEFAULT mode when submitted', () => {
    const modal = getShallowComponent().find(NxStatefulForm);

    modal.simulate('submit');

    expect(resetInitialNewPasswordValueMock).toHaveBeenCalled();
    expect(setModeMock).toHaveBeenCalledWith('');
  });

  it('calls reset new password and setMode with DEFAULT mode when canceled', () => {
    const modal = getShallowComponent().find(NxStatefulForm);
    modal.simulate('cancel');

    expect(resetInitialNewPasswordValueMock).toHaveBeenCalled();
    expect(setModeMock).toHaveBeenCalledWith('');
  });
});
