/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxButton, NxModal } from '@sonatype/react-shared-components';
import EulaModal from '../../../../../main/frontend/configuration/license/EulaModal';
import * as enzymeUtils from '../../../enzymeUtils';

describe('EULAModal', () => {
  let minimalProps, mockCloseModal, mockUpdateLicense, getShallowComponent, modalContainer;

  beforeEach(() => {
    mockCloseModal = jasmine.createSpy('closeModal');
    mockUpdateLicense = jasmine.createSpy('updateLicense');

    minimalProps = {
      closeModal: mockCloseModal,
      updateLicense: mockUpdateLicense,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(EulaModal, minimalProps);

    modalContainer = document.createElement('div');
    document.body.appendChild(modalContainer);
  });

  afterEach(() => {
    if (modalContainer) {
      document.body.removeChild(modalContainer);
      modalContainer = null;
    }
  });

  it('renders a component with a NxModal', () => {
    const shallowComponent = getShallowComponent();
    const modal = shallowComponent.find(NxModal);
    expect(modal).toExist();
  });

  it('calls closeModal when the user click on I Decline button', () => {
    const shallowComponent = getShallowComponent();
    const modal = shallowComponent.find(NxModal);
    const declineBtn = modal.find(NxButton).at(0);
    declineBtn.simulate('click');
    expect(mockCloseModal).toHaveBeenCalledTimes(1);
  });

  it('calls updateLicense when the I Accept button is clicked', () => {
    const shallowComponent = getShallowComponent();
    const modal = shallowComponent.find(NxModal);
    const acceptBtn = modal.find(NxButton).at(1);
    acceptBtn.simulate('click');
    expect(mockUpdateLicense).toHaveBeenCalledTimes(1);
  });
});
