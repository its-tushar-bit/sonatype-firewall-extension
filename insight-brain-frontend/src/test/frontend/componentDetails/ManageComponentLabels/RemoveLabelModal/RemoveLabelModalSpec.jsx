/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import RemoveLabelModal from 'MainRoot/componentDetails/ManageComponentLabels/RemoveLabelModal/RemoveLabelModal';
import { NxButton, NxLoadError } from '@sonatype/react-shared-components';

describe('RemoveLabelModal', () => {
  let minimalProps, getShallow;

  beforeEach(function () {
    minimalProps = {
      removeLabel: jasmine.createSpy('removeLabel'),
      selectedLabelDetails: {},
      toggleShowRemoveLabelModal: jasmine.createSpy('toggleShowRemoveLabelModal'),
      showRemoveLabelModal: true,
      removeLabelError: null,
    };

    getShallow = enzymeUtils.getShallowComponent(RemoveLabelModal, minimalProps);
  });

  it('renders a nx-modal-header', () => {
    const component = getShallow();
    const content = component.find('.nx-modal-header').find('.nx-h2').find('span');

    expect(content).toHaveText('Remove Label');
  });

  it('renders a nx-modal-content', () => {
    const component = getShallow();
    const content = component.find('.nx-modal-content').find('.nx-p');

    expect(content).toHaveText('Are you sure you want to remove this label?');
  });

  it('calls removeLabel', () => {
    const component = getShallow().find(NxButton).at(1);
    component.simulate('click');

    expect(minimalProps.removeLabel).toHaveBeenCalled();
  });

  it('calls toggleShowRemoveLabelModal when user clicks cancel', () => {
    const component = getShallow().find(NxButton).at(0);
    component.simulate('click');

    expect(minimalProps.toggleShowRemoveLabelModal).toHaveBeenCalled();
  });

  it('displays an error message if theres any error removing the label', () => {
    const component = getShallow({ removeLabelError: 'some err' }),
      error = component.find(NxLoadError);

    expect(error).toHaveProp('error', 'some err');
    expect(error).toHaveProp('titleMessage', 'An error occurred removing label.');
    expect(error).toHaveProp('retryHandler');
  });
});
