/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import WaiveTransitiveViolationsPopover from '../../../main/frontend/violation/WaiveTransitiveViolationsPopover';
import { mount } from 'enzyme';
import React from 'react';
import { NxLoadError, NxStatefulTextInput, NxSubmitMask } from '@sonatype/react-shared-components';
import { useWaiverExpirations } from '../../../main/frontend/util/waiverUtils';
import TransitiveViolationsSummary from '../../../main/frontend/violation/TransitiveViolationsSummary';
import { IqPopoverHeader } from '../../../main/frontend/react/IqPopover';

describe('WaiveTransitiveViolationsPopover', function () {
  let minimalProps,
    spyToggleWaiveTransitiveViolations,
    spySetScope,
    spySetExpiration,
    spySetComments,
    spyCancel,
    spySave,
    getShallowComponent;

  beforeEach(function () {
    spyToggleWaiveTransitiveViolations = jasmine.createSpy('spyToggleWaiveTransitiveViolations');
    spySetScope = jasmine.createSpy('spySetScope');
    spySetExpiration = jasmine.createSpy('spySetExpiration');
    spySetComments = jasmine.createSpy('spySetComments');
    spyCancel = jasmine.createSpy('spyCancel');
    spySave = jasmine.createSpy('spySave');
    minimalProps = {
      availableScopes: {
        data: [{ publicId: 'appPublicId', name: 'app', type: 'application' }],
      },
      componentTransitivePolicyViolations: {
        threatCounts: {
          critical: 5,
          severe: 4,
          moderate: 3,
          low: 2,
          none: 1,
        },
        threatCountsTotal: 15,
        componentCount: 1,
      },
      scope: 'appPublicId',
      expiration: 'never',
      comments: 'someComments',
      submitMaskState: null,
      saveError: null,
      toggleWaiveTransitiveViolations: spyToggleWaiveTransitiveViolations,
      setScope: spySetScope,
      setExpiration: spySetExpiration,
      setComments: spySetComments,
      cancel: spyCancel,
      save: spySave,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(WaiveTransitiveViolationsPopover, minimalProps);
  });

  it('sets the scope to the first available scope if it is null', function () {
    getShallowComponent({
      ...minimalProps,
      scope: null,
    });
    expect(spySetScope).toHaveBeenCalledWith('appPublicId');
  });

  it('calls toggleWaiveTransitiveViolations when the toggle is clicked', function () {
    const wrapper = getShallowComponent();
    const header = wrapper.find(IqPopoverHeader).dive();
    const toggle = header.find('#waive-transitive-violations-popover-toggle');
    toggle.simulate('click');
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('calls toggleWaiveTransitiveViolations when the popover is closed', function () {
    const wrapper = getShallowComponent();
    const toggle = wrapper.find('#waive-transitive-violations-popover');
    toggle.simulate('close');
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('creates a transitive violations summary with the correct props', function () {
    const wrapper = getShallowComponent();
    const transitiveViolationsSummary = wrapper.find(TransitiveViolationsSummary);
    expect(transitiveViolationsSummary).toHaveProp(
      'threatCounts',
      minimalProps.componentTransitivePolicyViolations.threatCounts
    );
    expect(transitiveViolationsSummary).toHaveProp(
      'threatCountsTotal',
      minimalProps.componentTransitivePolicyViolations.threatCountsTotal
    );
    expect(transitiveViolationsSummary).toHaveProp(
      'componentCount',
      minimalProps.componentTransitivePolicyViolations.componentCount
    );
  });

  it('calls cancel and toggleWaiveTransitiveViolations when the cancel button is clicked', function () {
    const wrapper = mount(<WaiveTransitiveViolationsPopover {...minimalProps} />);
    const cancel = wrapper.find('#waive-transitive-violations-popover-cancel').at(0);
    cancel.simulate('click');
    expect(spyCancel).toHaveBeenCalled();
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
    wrapper.unmount();
  });

  it('calls save when the save button is clicked', function () {
    const wrapper = getShallowComponent();
    const save = wrapper.find('#waive-transitive-violations-popover-save').at(0);
    save.simulate('click');
    expect(spySave).toHaveBeenCalled();
  });

  it('calls save when the retry button is clicked', function () {
    const wrapper = mount(
      <WaiveTransitiveViolationsPopover
        {...{
          ...minimalProps,
          saveError: 'someSaveError',
        }}
      />
    );
    const retry = wrapper.find(NxLoadError).find('.nx-btn');
    retry.simulate('click');
    expect(spySave).toHaveBeenCalled();
    wrapper.unmount();
  });

  it('shows the scope', function () {
    const wrapper = getShallowComponent();
    const scope = wrapper.find('#waive-transitive-violations-scopes .nx-read-only__data');
    expect(scope).toHaveText('Application - app');
  });

  it('shows the waiver expiration options as a select with the correct initial value', function () {
    const wrapper = getShallowComponent();
    const select = wrapper.find('#waive-transitive-violations-expirations');
    expect(select).toHaveProp('value', 'never');
    const options = wrapper.find('option');
    expect(options).toBeDefined();
    const waiverExpirations = useWaiverExpirations(false);
    expect(options.length).toBe(waiverExpirations.length);
    options.forEach((option, index) => {
      expect(option).toHaveProp('value', waiverExpirations[index].value);
      expect(option).toHaveText(waiverExpirations[index].name);
    });
  });

  it('updates the expiration on change', function () {
    const wrapper = getShallowComponent();
    const select = wrapper.find('#waive-transitive-violations-expirations');
    select.simulate('change', { currentTarget: { value: '7' } });
    expect(spySetExpiration).toHaveBeenCalledWith('7');
  });

  it('shows the comments in a text input with the correct initial value', function () {
    const wrapper = getShallowComponent();
    const textInput = wrapper.find(NxStatefulTextInput);
    expect(textInput).toHaveProp('defaultValue', 'someComments');
  });

  it('updates the comments on change', function () {
    const wrapper = getShallowComponent();
    const textInput = wrapper.find(NxStatefulTextInput);
    textInput.simulate('change', 'foo');
    expect(spySetComments).toHaveBeenCalledWith('foo');
  });

  it('shows the submit mask only if the submit mask state is not null', function () {
    let wrapper = getShallowComponent();
    let submitMask = wrapper.find(NxSubmitMask);
    expect(submitMask).not.toExist();
    wrapper = getShallowComponent({ ...minimalProps, submitMaskState: false });
    submitMask = wrapper.find(NxSubmitMask);
    expect(submitMask).toExist();
    wrapper = getShallowComponent({ ...minimalProps, submitMaskState: true });
    submitMask = wrapper.find(NxSubmitMask);
    expect(submitMask).toExist();
  });

  it('shows the save error if it exists', function () {
    let wrapper = getShallowComponent();
    let saveError = wrapper.find(NxLoadError);
    expect(saveError).not.toExist();
    let saveButton = wrapper.find('#waive-transitive-violations-popover-save');
    expect(saveButton).toHaveProp('variant', 'primary');
    expect(saveButton).toHaveText('Save');

    wrapper = getShallowComponent({ ...minimalProps, saveError: 'someSaveError' });
    saveError = wrapper.find(NxLoadError);
    expect(saveError).toHaveProp('error', 'someSaveError');
    expect(saveError).toHaveProp('titleMessage', 'An error occurred saving data.');
    saveButton = wrapper.find('#waive-transitive-violations-popover-save');
    expect(saveButton).not.toExist();
  });
});
