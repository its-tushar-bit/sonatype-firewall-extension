/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ConfirmationModal from '../../../../main/frontend/legal/application/ConfirmationModal';

describe('ConfirmationModal component', function () {
  let getMountedComponent, minimalProps;

  beforeEach(function () {
    minimalProps = {
      id: 'custom-id',
      titleContent: 'Custom title',
      confirmationMessage: 'Custom confirmation message',
      confirmationButtonText: 'Custom confirmation button text',
      cancelHandler: () => null,
      closeHandler: () => null,
      confirmationHandler: () => null,
    };

    getMountedComponent = enzymeUtils.getShallowComponent(ConfirmationModal, minimalProps);
  });

  it('renders a confirmation modal with custom texts', function () {
    const wrapper = getMountedComponent();
    expect(wrapper.find('.nx-h2').text()).toBe(minimalProps.titleContent);
    expect(wrapper.find('.nx-modal-content').text()).toBe(minimalProps.confirmationMessage);
  });
});
