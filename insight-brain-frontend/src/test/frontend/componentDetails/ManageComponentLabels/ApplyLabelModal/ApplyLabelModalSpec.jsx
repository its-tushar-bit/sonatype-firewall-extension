/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxModal, NxStatefulForm, NxRadio, NxSubmitMask } from '@sonatype/react-shared-components';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import ApplyLabelModal from 'MainRoot/componentDetails/ManageComponentLabels/ApplyLabelModal/ApplyLabelModal';

describe('ApplyLabelModal', () => {
  let getShallow;
  let getMounted;
  let containerModal;
  let mountedModal;

  const loadApplicableLabelScopesMock = jasmine.createSpy('loadApplicableLabelScopes');
  const saveApplyLabelScopeMock = jasmine.createSpy('saveApplyLabelScope');
  const cancelApplyLabelModalMock = jasmine.createSpy('cancelApplyLabelModal');
  const setLabelScopeToSaveMock = jasmine.createSpy('setLabelScopeToSave');

  const minimalProps = {
    applicableLabelScopes: [],
    loading: false,
    loadError: null,
    componentName: 'test component',
    labelScopeToSave: {},
    loadApplicableLabelScopes: loadApplicableLabelScopesMock,
    cancelApplyLabelModal: cancelApplyLabelModalMock,
    saveApplyLabelScope: saveApplyLabelScopeMock,
    setLabelScopeToSave: setLabelScopeToSaveMock,
    selectedLabelDetails: {
      id: 'test id',
      color: 'pink',
      description: 'test description',
      label: 'test label text',
      ownerId: 'application',
    },
    showApplyLabelModal: true,
  };

  beforeEach(() => {
    containerModal = document.createElement('div');
    document.body.appendChild(containerModal);

    getShallow = enzymeUtils.getShallowComponent(ApplyLabelModal, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(ApplyLabelModal, minimalProps, { attachTo: containerModal });
  });

  afterEach(() => {
    if (containerModal) {
      document.body.removeChild(containerModal);
      containerModal = null;
      mountedModal?.unmount();
    }
  });

  it('renders nothing if showApplyLabelModal is false', () => {
    const modal = getShallow({ showApplyLabelModal: false });
    expect(modal).toBeEmptyRender();
  });
  it('renders a component with NxModal', () => {
    expect(getShallow().find(NxModal)).toExist();
  });

  it('calls loadApplicableLabelScopes on mount', () => {
    const form = getShallow().find(NxStatefulForm);
    form.invoke('doLoad')();

    expect(loadApplicableLabelScopesMock).toHaveBeenCalledTimes(1);
  });

  it('calls saveApplyLabelScope when submitted', () => {
    const form = getShallow().find(NxStatefulForm);
    form.simulate('submit');

    expect(saveApplyLabelScopeMock).toHaveBeenCalledTimes(1);
  });

  it('calls cancelApplyLabelModal on cancel', () => {
    const form = getShallow().find(NxStatefulForm);
    form.simulate('cancel');

    expect(cancelApplyLabelModalMock).toHaveBeenCalled();
  });

  it('renders NxRadio buttons with the correct scope props', () => {
    const radio = getShallow({
      applicableLabelScopes: [
        {
          children: null,
          id: 'testScopeId',
          name: 'testScopeName',
          type: 'application',
        },
      ],
      labelScopeToSave: { labelScopeType: 'application', labelScopeId: 'testScopeId' },
    }).find(NxRadio);

    expect(radio).toHaveProp('value', 'testScopeName');
    expect(radio).toHaveProp('isChecked', true);
    expect(radio.text()).toBe('application - testScopeName');
  });

  it('sets labelScopeToSave upon clicking NxRadio button', () => {
    const radio = getShallow({
      applicableLabelScopes: [
        {
          children: null,
          id: 'testScopeId',
          name: 'testScopeName',
          type: 'application',
        },
      ],
    }).find(NxRadio);
    radio.simulate('change');
    expect(setLabelScopeToSaveMock).toHaveBeenCalledTimes(1);
  });

  it('sets a validation error if labelScopeToSave has not been set', () => {
    const form = getShallow({
      labelScopeToSave: {},
    }).find(NxStatefulForm);
    expect(form).toHaveProp('validationErrors', 'Select a scope to apply');
  });

  it('sets the saveLabelError prop to the NxStatefulForm submitError prop', () => {
    let form = getShallow({
      saveLabelError: null,
    }).find(NxStatefulForm);
    expect(form).toHaveProp('submitError', null);

    form = getShallow({
      saveLabelError: 'save-err',
    }).find(NxStatefulForm);
    expect(form).toHaveProp('submitError', 'save-err');
  });

  it('does not render NxSubmitMask when submitMaskState is null', function () {
    const modal = getShallow({ submitMaskState: null });
    const submitMask = modal.find(NxSubmitMask);
    expect(submitMask).not.toExist();
  });

  it('renders NxSubmitMask with loading message when submitMaskState is false', () => {
    mountedModal = getMounted({ submitMaskState: false });
    const submitMask = mountedModal.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Applying label…');
  });

  it('renders NxSubmitMask with success message when submitMaskState is true', function () {
    mountedModal = getMounted({ submitMaskState: true });
    const submitMask = mountedModal.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Success!');
  });
});
