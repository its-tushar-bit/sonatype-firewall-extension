/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxModal } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../enzymeUtils';
import InnerSourceProducerReportModal from '../../../../../main/frontend/applicationReport/results/cipModal/cipTabPanel/innerSourceProducerReportModal/InnerSourceProducerReportModal';

describe('InnerSourceProducerReportModal', function () {
  let minimalProps, getShallowComponent, onCloseMock, containerModal;

  beforeEach(function () {
    onCloseMock = jasmine.createSpy('onClose');
    minimalProps = {
      onClose: onCloseMock,
      reportUrl: 'someUrl',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(InnerSourceProducerReportModal, minimalProps);

    containerModal = document.createElement('div');
    document.body.appendChild(containerModal);
  });

  afterEach(function () {
    if (containerModal) {
      document.body.removeChild(containerModal);
      containerModal = null;
    }
  });

  it('renders a component with NxModal', function () {
    expect(getShallowComponent().find(NxModal)).toExist();
  });

  it('opens a new tab when clicking the "Continue to Report" button', function () {
    spyOn(window, 'open');
    const button = getShallowComponent().find('#innersource-producer-report-modal-continue-to-report');
    button.simulate('click');

    expect(onCloseMock).toHaveBeenCalled();
    expect(window.open).toHaveBeenCalledWith('someUrl', '_blank');
  });

  it('calls the onClose callback when using the Cancel button', function () {
    const button = getShallowComponent().find('#innersource-producer-report-modal-cancel');
    button.simulate('click');

    expect(onCloseMock).toHaveBeenCalled();
  });

  it('calls the onClose callback when dismissing the modal via keyboard', function () {
    enzymeUtils.getMountedComponent(InnerSourceProducerReportModal, minimalProps, { attachTo: containerModal })();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(onCloseMock).toHaveBeenCalled();
  });
});
