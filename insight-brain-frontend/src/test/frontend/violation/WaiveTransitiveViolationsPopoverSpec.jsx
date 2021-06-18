/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import WaiveTransitiveViolationsPopover from '../../../main/frontend/violation/WaiveTransitiveViolationsPopover';
import { mount } from 'enzyme';
import React from 'react';
import { NxErrorAlert, NxFontAwesomeIcon, NxStatefulTextInput, NxSubmitMask } from '@sonatype/react-shared-components';
import { waiverExpirations } from '../../../main/frontend/util/waiverUtils';
import { faSync } from '@fortawesome/pro-solid-svg-icons';

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
    const toggle = wrapper.find('#waive-transitive-violations-popover-toggle');
    toggle.simulate('click');
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('calls toggleWaiveTransitiveViolations when the popover is closed', function () {
    const wrapper = getShallowComponent();
    const toggle = wrapper.find('#waive-transitive-violations-popover');
    toggle.simulate('close');
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('displays the correct sublabel', function () {
    let wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: {
        ...minimalProps.componentTransitivePolicyViolations,
        threatCountsTotal: 3,
        componentCount: 2,
      },
    });
    let countsGroup = wrapper.find('#waive-transitive-violations-counts-group');
    expect(countsGroup.props().sublabel).toBe(3 + ' total violations brought in by ' + 2 + ' components');
    wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: {
        ...minimalProps.componentTransitivePolicyViolations,
        threatCountsTotal: 0,
        componentCount: 0,
      },
    });
    countsGroup = wrapper.find('#waive-transitive-violations-counts-group');
    expect(countsGroup.props().sublabel).toBe(0 + ' total violations brought in by ' + 0 + ' components');
    wrapper = getShallowComponent({
      ...minimalProps,
      componentTransitivePolicyViolations: {
        ...minimalProps.componentTransitivePolicyViolations,
        threatCountsTotal: 1,
        componentCount: 1,
      },
    });
    countsGroup = wrapper.find('#waive-transitive-violations-counts-group');
    expect(countsGroup.props().sublabel).toBe(1 + ' total violation brought in by ' + 1 + ' component');
  });

  it('displays the correct transitive violation counts', function () {
    const wrapper = getShallowComponent();
    const countsContainer = wrapper.find('#waive-transitive-violations-counts').at(0);
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-critical')).toBeFalsy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-severe')).toBeFalsy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-moderate')).toBeFalsy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-low')).toBeFalsy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-none')).toBeFalsy();
    expect(countsContainer.find('.nx-threat-counter--critical dt')).toHaveText('Critical');
    expect(countsContainer.find('.nx-threat-counter--critical dd')).toHaveText('5');
    expect(countsContainer.find('.nx-threat-counter--severe dt')).toHaveText('Severe');
    expect(countsContainer.find('.nx-threat-counter--severe dd')).toHaveText('4');
    expect(countsContainer.find('.nx-threat-counter--moderate dt')).toHaveText('Moderate');
    expect(countsContainer.find('.nx-threat-counter--moderate dd')).toHaveText('3');
    expect(countsContainer.find('.nx-threat-counter--low dt')).toHaveText('Low');
    expect(countsContainer.find('.nx-threat-counter--low dd')).toHaveText('2');
    expect(countsContainer.find('.nx-threat-counter--none dt')).toHaveText('None');
    expect(countsContainer.find('.nx-threat-counter--none dd')).toHaveText('1');
  });

  it('hides zero counts', function () {
    const wrapper = mount(
      <WaiveTransitiveViolationsPopover
        {...{
          ...minimalProps,
          componentTransitivePolicyViolations: {
            threatCounts: {
              critical: 0,
              severe: 0,
              moderate: 0,
              low: 0,
              none: 0,
            },
            threatCountsTotal: 0,
            componentCount: 0,
          },
        }}
      />
    );
    getShallowComponent();
    const countsContainer = wrapper.find('#waive-transitive-violations-counts').at(0);
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-critical')).toBeTruthy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-severe')).toBeTruthy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-moderate')).toBeTruthy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-low')).toBeTruthy();
    expect(countsContainer.hasClass('nx-threat-counter-container--hide-zero-none')).toBeTruthy();
  });

  it('calls cancel and toggleWaiveTransitiveViolations when the cancel button is clicked', function () {
    const wrapper = mount(<WaiveTransitiveViolationsPopover {...minimalProps} />);
    const cancel = wrapper.find('#waive-transitive-violations-popover-cancel').at(0);
    cancel.simulate('click');
    expect(spyCancel).toHaveBeenCalled();
    expect(spyToggleWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('calls save when the save button is clicked', function () {
    const wrapper = getShallowComponent();
    const save = wrapper.find('#waive-transitive-violations-popover-save').at(0);
    save.simulate('click');
    expect(spySave).toHaveBeenCalled();
  });

  it('shows the scope', function () {
    const wrapper = getShallowComponent();
    const scope = wrapper.find('#waive-transitive-violations-scopes');
    expect(scope).toHaveText('Application - app');
  });

  it('shows the waiver expiration options as a select with the correct initial value', function () {
    const wrapper = getShallowComponent();
    const select = wrapper.find('select');
    expect(select).toHaveProp('value', 'never');
    const options = wrapper.find('option');
    expect(options.length).toBe(waiverExpirations.length);
    options.forEach((option, index) => {
      expect(option).toHaveProp('value', waiverExpirations[index].value);
      expect(option).toHaveText(waiverExpirations[index].name);
    });
  });

  it('updates the expiration on change', function () {
    const wrapper = getShallowComponent();
    const select = wrapper.find('select');
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
    let alert = wrapper.find(NxErrorAlert);
    expect(alert).not.toExist();
    let saveButton = wrapper.find('#waive-transitive-violations-popover-save');
    expect(saveButton).toHaveProp('variant', 'primary');
    expect(saveButton).toHaveText('Save');
    let retryIcon = saveButton.find(NxFontAwesomeIcon);
    expect(retryIcon).not.toExist();

    wrapper = getShallowComponent({ ...minimalProps, saveError: 'someSaveError' });
    alert = wrapper.find(NxErrorAlert);
    expect(alert).toExist();
    const saveErrorMessage = alert.find('span');
    expect(saveErrorMessage).toHaveText('someSaveError');
    saveButton = wrapper.find('#waive-transitive-violations-popover-save');
    expect(saveButton).toHaveProp('variant', 'error');
    expect(saveButton.text()).toContain('Retry');
    retryIcon = saveButton.find(NxFontAwesomeIcon);
    expect(retryIcon).toExist();
    expect(retryIcon.props().icon).toEqual(faSync);
  });
});
