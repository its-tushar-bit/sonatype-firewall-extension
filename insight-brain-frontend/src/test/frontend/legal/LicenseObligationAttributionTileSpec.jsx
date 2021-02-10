/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationAttributionTile from '../../../main/frontend/legal/LicenseObligationAttributionTile';
import { NxButton, NxTextInput, NxCheckbox } from '@sonatype/react-shared-components';

describe('LicenseObligationAttributionTile component', function() {
  let getShallowComponent,
      setAttributionTextSpy = jasmine.createSpy('setAttributionTextSpy'),
      setObligationFulfilledSpy = jasmine.createSpy('setObligationFulfilledSpy'),
      setAttributionScopeSpy = jasmine.createSpy('setAttributionScope'),
      saveAttributionSpy = jasmine.createSpy('saveAttributionSpy'),
      setShowAttributionModalSpy = jasmine.createSpy('setShowAttributionModalSpy');

  const minimalProps = {
    setAttributionText: setAttributionTextSpy,
    setObligationFulfilled: setObligationFulfilledSpy,
    setAttributionScope: setAttributionScopeSpy,
    saveAttribution: saveAttributionSpy,
    setShowAttributionModal: setShowAttributionModalSpy,
    id: null,
    name: 'License Obligation Name',
    originalAttributionText: '',
    attributionText: '',
    originalObligationFulfilled: false,
    obligationFulfilled: false,
    availableScopes: { values: [{ id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }] },
    originalScope: 'ROOT_ORGANIZATION_ID',
    scope: 'ROOT_ORGANIZATION_ID',
    error: null,
    saveAttributionSubmitMask: null,
    showAttributionModal: false
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationAttributionTile, minimalProps);
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
    const wrapper = getShallowComponent({ id: 'id' });
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Edit Attribution');
  });

  it('renders `None added` as content if there is no attribution', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile-content')).toHaveText('None added');
  });

  it('renders the original attribution text as content if there is an attribution', function() {
    const attribution = { id: 'id', originalAttributionText: 'Some attribution text.' };
    const wrapper = getShallowComponent(attribution);
    expect(wrapper.find('.nx-tile-content')).toHaveText(attribution.originalAttributionText);
  });

  it('renders the modal with root org data', function() {
    const wrapper = getShallowComponent({ showAttributionModal: true });
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', '');
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
      attributionText: 'Some obligation attribution text.',
      obligationFulfilled: true,
      availableScopes: {
        values: [
          { id: 'orgId', name: 'org', label: 'Organization' },
          { id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }
        ]
      },
      showAttributionModal: true
    };
    const wrapper = getShallowComponent(orgData);
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', 'Some obligation attribution text.');
    let checkbox = wrapper.find(NxCheckbox);
    expect(checkbox).toHaveProp('isChecked', false);
    expect(checkbox.text()).toContain('Mark "License Obligation Name" as fulfilled.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(2);
    expect(options.at(0)).toHaveText('Organization - org');
    expect(options.at(1)).toHaveText('Organization - Root Organization');
  });

  it('renders the modal with app data', function() {
    let appData = {
      attributionText: 'Some obligation attribution text.',
      obligationFulfilled: true,
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', label: 'Application' },
          { id: 'orgId', name: 'org', label: 'Organization' },
          { id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization' }
        ]
      },
      showAttributionModal: true
    };
    const wrapper = getShallowComponent(appData);
    wrapper.find('.nx-tile__actions').find(NxButton).simulate('click');
    expect(wrapper.find('.nx-modal-header')).toHaveText('Attribution for "License Obligation Name"');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', 'Some obligation attribution text.');
    let checkbox = wrapper.find(NxCheckbox);
    expect(checkbox).toHaveProp('isChecked', false);
    expect(checkbox.text()).toContain('Mark "License Obligation Name" as fulfilled.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(3);
    expect(options.at(0)).toHaveText('Application - app');
    expect(options.at(1)).toHaveText('Organization - org');
    expect(options.at(2)).toHaveText('Organization - Root Organization');
  });
});
