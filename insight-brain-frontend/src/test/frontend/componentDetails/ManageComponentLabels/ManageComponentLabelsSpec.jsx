/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import { NxLoadWrapper, NxSubmitMask } from '@sonatype/react-shared-components';
import * as RemoveLabelModal from 'MainRoot/componentDetails/ManageComponentLabels/RemoveLabelModal/RemoveLabelModalContainer';
import ManageComponentLabels from 'MainRoot/componentDetails/ManageComponentLabels/ManageComponentLabels';
import * as ApplyLabelModalContainer from 'MainRoot/componentDetails/ManageComponentLabels/ApplyLabelModal/ApplyLabelModalContainer';
import TransferList from 'MainRoot/componentDetails/TransferList/TransferList';

describe('ManageComponentLabels', () => {
  let minimalProps, getShallow, loadApplicableLabelsMock, getMounted, handleAddLabelTagMock;

  beforeEach(function () {
    spyOn(RemoveLabelModal, 'default').and.returnValue(<div>Modal</div>);
    loadApplicableLabelsMock = jasmine.createSpy('loadApplicableLabels').and.returnValue([]);
    handleAddLabelTagMock = jasmine.createSpy('handleAddLabelTag');
    spyOn(ApplyLabelModalContainer, 'default').and.returnValue(<div>Modal</div>);

    minimalProps = {
      applicableLabels: [],
      selectedLabels: [],
      showApplyLabelModal: false,
      handleAddLabelTag: handleAddLabelTagMock,
      loadApplicableLabels: loadApplicableLabelsMock,
      loading: false,
      loadError: null,
      handleRemoveLabelTag: () => {},
    };

    getShallow = enzymeUtils.getShallowComponent(ManageComponentLabels, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(ManageComponentLabels, minimalProps);
  });

  it('renders a component', () => {
    const component = getMounted();
    expect(component).toExist();
    component.unmount();
  });

  it('renders an nx-h2 title', () => {
    const component = getShallow();
    const content = component.find(NxLoadWrapper).dive().find('.nx-h2');

    expect(content).toHaveText('Manage Labels');
  });

  it('calls loadApplicable action', () => {
    const component = getMounted();
    expect(loadApplicableLabelsMock).toHaveBeenCalledTimes(1);
    component.unmount();
  });

  it('renders a TransferList', () => {
    const component = getShallow();

    expect(component.find(NxLoadWrapper).dive().find(TransferList)).toExist();
  });

  it('renders a TransferList with correct props', () => {
    const component = getShallow({
      applicableLabels: [{ id: 1 }, { id: 2 }, { id: 3 }],
      selectedLabels: [{ id: 3 }],
    })
      .find(NxLoadWrapper)
      .dive();

    expect(component.find(TransferList)).toExist();
    expect(component.find(TransferList)).toHaveProp('available', [{ id: 1 }, { id: 2 }]);
    expect(component.find(TransferList)).toHaveProp('selected', [{ id: 3 }]);
    expect(component.find(TransferList)).toHaveProp('onAddItem', handleAddLabelTagMock);
  });

  it('does not render NxSubmitMask when applyLabelMaskState is null', function () {
    const component = getShallow({ applyLabelMaskState: null });
    const submitMask = component.find(NxSubmitMask);
    expect(submitMask).not.toExist();
  });

  it('renders NxSubmitMask with expected props when applyLabelMaskState is false', () => {
    const component = getShallow({ applyLabelMaskState: false });
    const submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveProp('message', 'Applying label…');
    expect(submitMask).toHaveProp('successMessage', 'Success!');
  });

  it('renders NxSubmitMask when applyLabelMaskState is true', function () {
    const component = getShallow({ applyLabelMaskState: true });
    const submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
  });
});
