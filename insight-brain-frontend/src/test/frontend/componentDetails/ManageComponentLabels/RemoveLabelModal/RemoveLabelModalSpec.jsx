/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import RemoveLabelModal from 'MainRoot/componentDetails/ManageComponentLabels/RemoveLabelModal/RemoveLabelModal';
import { NxForm, NxWarningAlert } from '@sonatype/react-shared-components';

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

  it('renders delete alert message', () => {
    const component = getShallow();
    const content = component.find(NxWarningAlert);

    expect(content).toHaveText('Are you sure you want to remove this label?');
  });

  it('calls removeLabel', () => {
    const component = getShallow().find(NxForm);
    component.simulate('submit');

    expect(minimalProps.removeLabel).toHaveBeenCalled();
  });

  it('calls toggleShowRemoveLabelModal when user clicks cancel', () => {
    const component = getShallow().find(NxForm);
    component.simulate('cancel');

    expect(minimalProps.toggleShowRemoveLabelModal).toHaveBeenCalled();
  });

  it('displays an error message if theres any error removing the label', () => {
    const component = getShallow({ removeLabelError: 'some err' }).find(NxForm);

    expect(component).toHaveProp('submitError', 'some err');
  });
});
