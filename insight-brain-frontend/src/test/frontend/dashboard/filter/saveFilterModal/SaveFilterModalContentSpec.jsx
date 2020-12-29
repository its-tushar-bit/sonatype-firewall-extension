/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {shallow} from 'enzyme';
import * as PropTypes from 'prop-types';
import {NxModal, NxSubmitMask, NxWarningAlert} from '@sonatype/react-shared-components';
import NxTextInput from '@sonatype/react-shared-components/components/NxTextInput/NxTextInput';

import * as enzymeUtils from '../../../enzymeUtils';
import SaveFilterModalContent
  from '../../../../../main/frontend/dashboard/filter/saveFilterModal/SaveFilterModalContent';

describe('SaveFilterModalContent component', function() {

  function MockMaximizedContainer({children}) {
    return <div>{children}</div>;
  }

  MockMaximizedContainer.propTypes = {children: PropTypes.node};

  let getShallowComponent,
      mountedComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(SaveFilterModalContent);
  });

  afterEach(function() {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('returns an NxModal component', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxModal)).toExist();
  });

  it('calls the cancelSaveFilter action if you hit the cancel button', function() {
    let cancelSaveFilterSpy = jasmine.createSpy('cancelSaveFilter');
    const wrapper = getShallowComponent({
      cancelSaveFilter: cancelSaveFilterSpy
    });
    const cancelButton = wrapper.find('#save-filter-modal-cancel-button');
    expect(cancelButton).toExist();
    cancelButton.simulate('click');
    expect(cancelSaveFilterSpy).toHaveBeenCalled();
  });

  it('returns an NxSubmitMask component iff saveFilterSuccess or saveFilterSaving is true', function() {
    let wrapper = shallow(<SaveFilterModalContent/>);
    expect(wrapper.find(NxSubmitMask)).not.toExist();

    wrapper = shallow(<SaveFilterModalContent saveFilterSuccess={true} />);
    expect(wrapper.find(NxSubmitMask)).toExist();

    wrapper = shallow(<SaveFilterModalContent saveFilterSaving={true} />);
    expect(wrapper.find(NxSubmitMask)).toExist();
  });

  it('shows the save filter form initially', () => {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-h2 span').text()).toBe('Save Filter');
    expect(wrapper.find('#dashboard-filter-overwrite')).toHaveProp('disabled', true);
    expect(wrapper.find('#dashboard-filter-save-as')).toExist();
    expect(wrapper.find('#save-filter-modal-continue-button')).toExist();
    expect(wrapper.find('#save-filter-modal-cancel-button')).toExist();
  });

  it('has the overwrite button disabled if there is not an appliedFilter passed in', () => {
    let wrapper = getShallowComponent();
    let overwriteRadioButton = wrapper.find('#dashboard-filter-overwrite');
    expect(overwriteRadioButton).toHaveProp('disabled', true);

    wrapper = getShallowComponent({
      appliedFilterName: 'filter'
    });
    overwriteRadioButton = wrapper.find('#dashboard-filter-overwrite');
    expect(overwriteRadioButton).toHaveProp('isChecked', true);
    expect(overwriteRadioButton).toHaveProp('disabled', false);
  });

  it('checks the overwrite radio button and shows the overwrite text if you have an active filter', () => {
    const wrapper = getShallowComponent({
      appliedFilterName: 'mario'
    });
    expect(wrapper.find('#dashboard-filter-overwrite')).toHaveProp('isChecked', true);
    expect(wrapper.find('#dashboard-filter-overwrite').text()).toBe('save (overwrite mario)');
  });

  it('checks the save as radio button and shows the save as text box if you dont have an active filter', () => {
    const wrapper = getShallowComponent();
    const overwriteRadioButton = wrapper.find('#dashboard-filter-overwrite');
    expect(overwriteRadioButton).toHaveProp('isChecked', false);
    expect(overwriteRadioButton).toHaveProp('disabled', true);

    expect(wrapper.find('#dashboard-filter-save-as')).toHaveProp('isChecked', true);
    expect(wrapper.find(NxTextInput)).toExist();
  });

  it('marks the filter name input as validatable', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxTextInput)).toHaveProp('validatable', true);
  });

  it('only shows the save as text box if you click the save as radio button', () => {
    const wrapper = getShallowComponent({
      appliedFilterName: 'mario'
    });
    const saveAsRadioButton = wrapper.find('#dashboard-filter-save-as');

    let saveAsTextBox = wrapper.find(NxTextInput);
    expect(saveAsRadioButton).toExist();
    expect(saveAsTextBox).not.toExist();

    saveAsRadioButton.simulate('change', 'saveAs');

    saveAsTextBox = wrapper.find(NxTextInput);
    expect(saveAsTextBox).toExist();
  });

  it('submits a filter to save (adding new filter)', () => {
    const saveFilterSpy = jasmine.createSpy('saveFilter');
    const wrapper = getShallowComponent({
      saveFilter: saveFilterSpy
    });
    const saveAsRadioButton = wrapper.find('#dashboard-filter-save-as');
    saveAsRadioButton.simulate('change', 'saveAs');
    const saveAsTextBox = wrapper.find(NxTextInput);
    saveAsTextBox.simulate('change', 'awesome new filter');

    expect(wrapper.find('#save-filter-modal-continue-button')).toHaveProp('disabled', false);

    const form = wrapper.find('form');
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const simulatedEvent = {
      preventDefault: preventDefaultSpy
    };
    form.simulate('submit', simulatedEvent);
    expect(preventDefaultSpy).toHaveBeenCalled();
    expect(saveFilterSpy).toHaveBeenCalledWith({ name: 'awesome new filter', isOverwriting: false });
  });

  it('submits a filter to save (overwriting existing filter)', () => {
    const saveFilterSpy = jasmine.createSpy('saveFilter');
    const wrapper = getShallowComponent({
      appliedFilterName: 'mario',
      saveFilter: saveFilterSpy
    });

    expect(wrapper.find('#dashboard-filter-overwrite')).toHaveProp('isChecked', true);
    const submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', false);

    let form = wrapper.find('form');
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const simulatedEvent = {
      preventDefault: preventDefaultSpy
    };
    form.simulate('submit', simulatedEvent);
    expect(preventDefaultSpy).toHaveBeenCalled();
    expect(saveFilterSpy).toHaveBeenCalledWith({ name: 'mario', isOverwriting: true });
  });

  it('saves the filter with trimmed value if filter name contains leading or trailing spaces', () => {
    const saveFilterSpy = jasmine.createSpy('saveFilter');
    const wrapper = getShallowComponent({
      saveFilter: saveFilterSpy
    });
    const saveAsRadioButton = wrapper.find('#dashboard-filter-save-as');
    saveAsRadioButton.simulate('change', 'saveAs');
    const saveAsTextBox = wrapper.find(NxTextInput);
    saveAsTextBox.simulate('change', ' mario   ');

    expect(wrapper.find('#save-filter-modal-continue-button')).toHaveProp('disabled', false);

    let warningAlert = wrapper.find(NxWarningAlert);
    expect(warningAlert).not.toExist();

    const form = wrapper.find('form');
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const simulatedEvent = {
      preventDefault: preventDefaultSpy
    };

    form.simulate('submit', simulatedEvent);
    expect(preventDefaultSpy).toHaveBeenCalled();
    expect(saveFilterSpy).toHaveBeenCalledWith({ name: 'mario', isOverwriting: false });
  });

  it('disables the save button if there is no value in the save as text box', () => {
    const wrapper = getShallowComponent();

    expect(wrapper.find('#dashboard-filter-save-as')).toHaveProp('isChecked', true);
    const saveAsTextBox = wrapper.find(NxTextInput);
    expect(saveAsTextBox).toExist();
    let submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', true);

    saveAsTextBox.simulate('change', 'a');
    submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', false);
  });

  it('disables the save button if there is a value in the save as text box more than 60 chars', () => {
    const wrapper = getShallowComponent();

    expect(wrapper.find('#dashboard-filter-save-as')).toHaveProp('isChecked', true);
    const saveAsTextBox = wrapper.find(NxTextInput);
    expect(saveAsTextBox).toExist();
    let submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', true);

    saveAsTextBox.simulate('change', 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefgh');
    submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', false);

    saveAsTextBox.simulate('change', 'abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghi');
    submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', true);
  });

  it('disables the save button if the value in the save as text box is "Default"', () => {
    const wrapper = getShallowComponent();

    expect(wrapper.find('#dashboard-filter-save-as')).toHaveProp('isChecked', true);
    const saveAsTextBox = wrapper.find(NxTextInput);
    expect(saveAsTextBox).toExist();
    let submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', true);

    saveAsTextBox.simulate('change', 'Default.');
    submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', false);

    saveAsTextBox.simulate('change', 'Default');
    submitButton = wrapper.find('#save-filter-modal-continue-button');
    expect(submitButton).toHaveProp('disabled', true);
  });
});
