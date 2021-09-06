/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton, NxModal } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../enzymeUtils';
import InnerSourceProducerPermissionsModal from '../../../../../main/frontend/applicationReport/results/cipModal/cipTabPanel/innerSourceProducerPermissionsModal/InnerSourceProducerPermissionsModal';

describe('InnerSourceProducerPermissionsModal', function () {
  let minimalProps, getShallowComponent, onCloseMock;

  beforeEach(function () {
    onCloseMock = jasmine.createSpy('onClose');
    minimalProps = {
      onClose: onCloseMock,
      applicationName: 'someName',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(InnerSourceProducerPermissionsModal, minimalProps);
  });

  it('renders a component with NxModal', function () {
    expect(getShallowComponent().find(NxModal)).toExist();
  });

  it('renders a modal containing the application name', function () {
    expect(getShallowComponent().find('.nx-modal-content').text()).toContain('someName');
  });

  it('calls the onClose callback when using the Close button', function () {
    const button = getShallowComponent().find(NxButton);
    button.simulate('click');

    expect(onCloseMock).toHaveBeenCalled();
  });

  it('calls the onClose callback when modal close is fired', () => {
    const shallowComponent = getShallowComponent();
    const modal = shallowComponent.find(NxModal);
    modal.simulate('close');

    expect(onCloseMock).toHaveBeenCalled();
  });
});
