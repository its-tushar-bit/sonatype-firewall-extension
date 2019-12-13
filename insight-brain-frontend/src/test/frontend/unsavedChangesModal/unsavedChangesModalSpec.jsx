/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import UnsavedChangesModal from '../../../main/frontend/unsavedChangesModal/UnsavedChangesModal';

describe('UnsavedChangesModal', function() {
  let getShallowComponent,
      mockOnContinue = jasmine.createSpy(),
      mockOnClose = jasmine.createSpy();

  const propsObj = {
    onContinue: mockOnContinue,
    onClose: mockOnClose
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(UnsavedChangesModal, propsObj);
  });

  it('renders an NxModal component with unsaved-modal id', function() {
    const shallowComponent = getShallowComponent();
    expect(shallowComponent).toMatchSelector('NxModal#unsaved-modal');
  });

  it('calls onContinue when continue button is clicked', function() {
    const shallowComponent = getShallowComponent();
    const button = shallowComponent.find('#unsaved-changes-modal-continue-button');
    button.simulate('click');
    expect(mockOnContinue).toHaveBeenCalled();
  });

  it('calls onClose when cancel button is clicked', function() {
    const shallowComponent = getShallowComponent();
    const button = shallowComponent.find('#unsaved-changes-modal-cancel-button');
    button.simulate('click');
    expect(mockOnClose).toHaveBeenCalled();
  });
});
