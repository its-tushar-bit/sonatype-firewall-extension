/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxModal,
  NxRadio,
  NxStatefulForm,
  NxSubmitMask,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import NxTextInput from '@sonatype/react-shared-components/components/NxTextInput/NxTextInput';

import * as enzymeUtils from 'TestRoot/enzymeUtils';
import SaveFilterModalContent from 'MainRoot/dashboard/filter/saveFilterModal/SaveFilterModalContent';

describe('SaveFilterModalContent component', function () {
  let getShallowComponent, mountPoint, getMountedComponentWithAutoClean, mountedComponent;

  const saveFilter = jasmine.createSpy('saveFilter'),
    cancelSaveFilter = jasmine.createSpy('cancelSaveFilter');

  const minimalProps = {
    saveFilter,
    cancelSaveFilter,
  };

  beforeEach(function () {
    mountPoint = document.createElement('div');
    document.body.appendChild(mountPoint);
    getShallowComponent = enzymeUtils.getShallowComponent(SaveFilterModalContent, minimalProps);

    const getMountedComponent = enzymeUtils.getMountedComponent(SaveFilterModalContent, minimalProps, {
      attachTo: mountPoint,
    });
    getMountedComponentWithAutoClean = (additionalProps) => {
      mountedComponent = getMountedComponent(additionalProps);
      return mountedComponent;
    };
  });

  afterEach(() => {
    if (mountPoint) {
      document.body.removeChild(mountPoint);
      mountPoint = null;
    }
    mountedComponent?.unmount();
    mountedComponent = null;
  });

  it('returns an NxModal component', function () {
    const wrapper = getShallowComponent();

    expect(wrapper.find(NxModal)).toExist();
  });

  it('calls the cancelSaveFilter action if you hit the cancel button', function () {
    const wrapper = getMountedComponentWithAutoClean(),
      cancelButton = wrapper.find(NxButton).first();

    expect(cancelButton).toExist();

    cancelButton.simulate('click');

    expect(cancelSaveFilter).toHaveBeenCalled();
  });

  describe('NxSubmitMask', function () {
    it('shows saving mask when saveFilterMaskState is false', function () {
      const wrapper = getMountedComponentWithAutoClean({ saveFilterMaskState: false }),
        mask = wrapper.find(NxSubmitMask);

      expect(mask).toHaveText('Saving…');
    });

    it('shows success mask when saveFilterMaskState is true', function () {
      const wrapper = getMountedComponentWithAutoClean({ saveFilterMaskState: true }),
        mask = wrapper.find(NxSubmitMask);

      expect(mask).toHaveText('Success!');
    });

    it('is not rendered when saveFilterMaskState is null', function () {
      const wrapper = getMountedComponentWithAutoClean({ saveFilterMaskState: null }),
        mask = wrapper.find(NxSubmitMask);

      expect(mask).not.toExist();
    });
  });

  it('shows the save filter modal form initially', () => {
    const wrapper = getMountedComponentWithAutoClean(),
      header = wrapper.find('.nx-h2 span'),
      overwriteRadioButton = wrapper.find(NxRadio).first(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput),
      submitButton = wrapper.find(NxButton).first(),
      cancelButton = wrapper.find(NxButton).last();

    expect(header.text()).toBe('Save Filter');
    expect(overwriteRadioButton).toHaveProp('disabled', true);
    expect(saveAsRadioButton).toExist();
    expect(saveAsTextBox).toExist();
    expect(submitButton).toExist();
    expect(cancelButton).toExist();
  });

  it('has the overwrite button disabled if there is not an appliedFilter passed in', () => {
    let wrapper = getShallowComponent(),
      overwriteRadioButton = wrapper.find(NxRadio).first();

    expect(overwriteRadioButton).toHaveProp('disabled', true);

    wrapper = getShallowComponent({ appliedFilterName: 'filter' });

    overwriteRadioButton = wrapper.find(NxRadio).first();

    expect(overwriteRadioButton).toHaveProp('isChecked', true);
    expect(overwriteRadioButton).toHaveProp('disabled', false);
  });

  it('checks the overwrite radio button and shows the overwrite text if you have an active filter', () => {
    const wrapper = getShallowComponent({ appliedFilterName: 'mario' }),
      overwriteRadioButton = wrapper.find(NxRadio).first();

    expect(overwriteRadioButton).toHaveProp('isChecked', true);
    expect(overwriteRadioButton.text()).toBe('save (overwrite mario)');
  });

  it('checks the save as radio button and shows the save as text box if you dont have an active filter', () => {
    const wrapper = getShallowComponent(),
      overwriteRadioButton = wrapper.find(NxRadio).first(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput);

    expect(overwriteRadioButton).toHaveProp('isChecked', false);
    expect(overwriteRadioButton).toHaveProp('disabled', true);

    expect(saveAsRadioButton).toHaveProp('isChecked', true);
    expect(saveAsTextBox).toExist();
  });

  it('marks the filter name input as validatable', function () {
    const wrapper = getShallowComponent(),
      saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsTextBox).toHaveProp('validatable', true);
  });

  it('only shows the save as text box if you click the save as radio button', () => {
    const wrapper = getShallowComponent({ appliedFilterName: 'mario' }),
      saveAsRadioButton = wrapper.find(NxRadio).last();

    let saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsRadioButton).toExist();
    expect(saveAsTextBox).not.toExist();

    saveAsRadioButton.simulate('change', 'saveAs');

    saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsTextBox).toExist();
  });

  it('submits a filter to save (adding new filter)', () => {
    const wrapper = getShallowComponent(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput);

    saveAsRadioButton.simulate('change', 'saveAs');
    saveAsTextBox.simulate('change', 'awesome new filter');

    const form = wrapper.find(NxStatefulForm),
      preventDefault = jasmine.createSpy('preventDefault');

    form.simulate('submit', { preventDefault });

    expect(preventDefault).toHaveBeenCalled();
    expect(saveFilter).toHaveBeenCalledWith({
      name: 'awesome new filter',
      isOverwriting: false,
    });
  });

  it('submits a filter to save (overwriting existing filter)', () => {
    const wrapper = getShallowComponent({ appliedFilterName: 'mario' }),
      overwriteRadioButton = wrapper.find(NxRadio).first();

    expect(overwriteRadioButton).toHaveProp('isChecked', true);

    const form = wrapper.find(NxStatefulForm),
      preventDefault = jasmine.createSpy('preventDefault');

    form.simulate('submit', { preventDefault });

    expect(preventDefault).toHaveBeenCalled();
    expect(saveFilter).toHaveBeenCalledWith({
      name: 'mario',
      isOverwriting: true,
    });
  });

  it('saves the filter with trimmed value if filter name contains leading or trailing spaces', () => {
    const wrapper = getShallowComponent(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput),
      warningAlert = wrapper.find(NxWarningAlert);

    saveAsRadioButton.simulate('change', 'saveAs');
    saveAsTextBox.simulate('change', ' mario   ');

    expect(warningAlert).not.toExist();

    const form = wrapper.find(NxStatefulForm),
      preventDefault = jasmine.createSpy('preventDefault');

    form.simulate('submit', { preventDefault });

    expect(preventDefault).toHaveBeenCalled();
    expect(saveFilter).toHaveBeenCalledWith({
      name: 'mario',
      isOverwriting: false,
    });
  });

  it('has validation errors if there is a value in the save as text box more than 60 chars', () => {
    const wrapper = getShallowComponent(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsRadioButton).toHaveProp('isChecked', true);
    expect(saveAsTextBox).toExist();

    saveAsTextBox.simulate('change', 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefgh');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual([]);

    saveAsTextBox.simulate('change', 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghi');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual(['Please enter less than 60 characters']);
  });

  it('has validation errors if the value in the save as text box is "Default"', () => {
    const wrapper = getShallowComponent(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsRadioButton).toHaveProp('isChecked', true);
    expect(saveAsTextBox).toExist();

    saveAsTextBox.simulate('change', 'Default.');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual([]);

    saveAsTextBox.simulate('change', 'Default');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual(['Can not overwrite Default filter']);
  });

  it('has validation errors if the save as text box has empty value', () => {
    const wrapper = getShallowComponent(),
      saveAsRadioButton = wrapper.find(NxRadio).last(),
      saveAsTextBox = wrapper.find(NxTextInput);

    expect(saveAsRadioButton).toHaveProp('isChecked', true);
    expect(saveAsTextBox).toExist();

    saveAsTextBox.simulate('change', 'asdf');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual([]);

    saveAsTextBox.simulate('change', '');

    expect(wrapper.find(NxStatefulForm).prop('validationErrors')).toEqual(['Must be non-empty']);
  });
});
