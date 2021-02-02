/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationAttributionTile from '../../../main/frontend/legal/LicenseObligationAttributionTile';
import { mount } from 'enzyme';
import React from 'react';
import { NxButton, NxTextInput, NxCheckbox } from '@sonatype/react-shared-components';

describe('LicenseObligationAttributionTile component', function() {
  let getShallowComponent,
      setAttributionTextSpy = jasmine.createSpy('setAttributionTextSpy'),
      setObligationFulfilledSpy = jasmine.createSpy('setObligationFulfilledSpy'),
      setScopeSpy = jasmine.createSpy('setScopeSpy');

  const minimalProps = {
    setAttributionText: setAttributionTextSpy,
    setObligationFulfilled: setObligationFulfilledSpy,
    setScope: setScopeSpy,
    name: 'License Obligation Name',
    attributionText: '',
    obligationFulfilled: false,
    availableScopes: { values: [{ id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }] }
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationAttributionTile, minimalProps);
  });

  it('sets the initial scope', function() {
    const component = mount(<LicenseObligationAttributionTile { ...minimalProps }/>);
    expect(setScopeSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: 'ROOT_ORGANIZATION_ID' });
    component.unmount();
  });

  it('renders a header with a label matching the license obligation name', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-h2')).toHaveText('Attribution for "License Obligation Name"');
  });

  it('renders an Add Attribution button if there is no attribution', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Add Attribution');
  });

  it('renders an Edit Attribution button if there is an attribution', function() {
    const wrapper = getShallowComponent({ attributionText: 'Some attribution text.' });
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Edit Attribution');
  });

  it('renders `None added` as content if there is no attribution', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile-content')).toHaveText('None added');
  });

  it('renders the attribution text as content if there is an attribution', function() {
    const attribution = { attributionText: 'Some attribution text.' };
    const wrapper = getShallowComponent(attribution);
    expect(wrapper.find('.nx-tile-content')).toHaveText(attribution.attributionText);
  });

  it('renders the modal with root org data', function() {
    const wrapper = mount(<LicenseObligationAttributionTile {...{ ...minimalProps, scope: 'ROOT_ORGANIZATION_ID' }}/>);
    expect(setAttributionTextSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: '' });
    expect(setObligationFulfilledSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: false });
    expect(setScopeSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: 'ROOT_ORGANIZATION_ID' });
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveText('');
    let checkbox = wrapper.find(NxCheckbox);
    expect(checkbox).toHaveProp('isChecked', false);
    expect(checkbox.text()).toContain('Mark "License Obligation Name" as fulfilled.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(1);
    expect(options.at(0)).toHaveText('Organization - Root Organization');
  });

  it('renders the modal with org data', function() {
    let orgData = {
      attributionText: 'Some obligation text.',
      obligationFulfilled: true,
      availableScopes: {
        values: [
          { id: 'orgId', name: 'org', label: 'Organization' },
          { id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }
        ]
      }
    };
    const wrapper = mount(<LicenseObligationAttributionTile { ...{ ...minimalProps, ...orgData, scope: 'orgId' } }/>);
    expect(setAttributionTextSpy).toHaveBeenCalledWith(
        { name: 'License Obligation Name', value: 'Some obligation text.' });
    expect(setObligationFulfilledSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: true });
    expect(setScopeSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: 'ROOT_ORGANIZATION_ID' });
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveText('Some obligation text.');
    let checkbox = wrapper.find(NxCheckbox);
    expect(checkbox).toHaveProp('isChecked', true);
    expect(checkbox.text()).toContain('Mark "License Obligation Name" as fulfilled.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'orgId');
    let options = wrapper.find('option');
    expect(options.length).toBe(2);
    expect(options.at(0)).toHaveText('Organization - org');
    expect(options.at(1)).toHaveText('Organization - Root Organization');
  });

  it('renders the modal with app data', function() {
    let appData = {
      attributionText: 'Some obligation text.',
      obligationFulfilled: true,
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', label: 'Application' },
          { id: 'orgId', name: 'org', label: 'Organization' },
          { id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }
        ]
      }
    };
    const wrapper = mount(<LicenseObligationAttributionTile {...{ ...minimalProps, ...appData, scope: 'appId' }}/>);
    expect(setAttributionTextSpy).toHaveBeenCalledWith(
        { name: 'License Obligation Name', value: 'Some obligation text.' });
    expect(setObligationFulfilledSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: true });
    expect(setScopeSpy).toHaveBeenCalledWith({ name: 'License Obligation Name', value: 'ROOT_ORGANIZATION_ID' });
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveText('Some obligation text.');
    let checkbox = wrapper.find(NxCheckbox);
    expect(checkbox).toHaveProp('isChecked', true);
    expect(checkbox.text()).toContain('Mark "License Obligation Name" as fulfilled.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'appId');
    let options = wrapper.find('option');
    expect(options.length).toBe(3);
    expect(options.at(0)).toHaveText('Application - app');
    expect(options.at(1)).toHaveText('Organization - org');
    expect(options.at(2)).toHaveText('Organization - Root Organization');
  });
});
