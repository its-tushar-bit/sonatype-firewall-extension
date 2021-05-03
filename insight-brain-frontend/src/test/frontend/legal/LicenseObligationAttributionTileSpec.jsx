/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationAttributionTile from '../../../main/frontend/legal/LicenseObligationAttributionTile';
import { NxTextInput } from '@sonatype/react-shared-components';

describe('LicenseObligationAttributionTile component', function () {
  let getShallowComponent,
    setAttributionTextSpy,
    setAttributionScopeSpy,
    saveAttributionSpy,
    setShowAttributionModalSpy;

  beforeEach(function () {
    setAttributionTextSpy = jasmine.createSpy('setAttributionTextSpy');
    setAttributionScopeSpy = jasmine.createSpy('setAttributionScope');
    saveAttributionSpy = jasmine.createSpy('saveAttributionSpy');
    setShowAttributionModalSpy = jasmine.createSpy('setShowAttributionModalSpy');
    const minimalProps = {
      setAttributionText: setAttributionTextSpy,
      setAttributionScope: setAttributionScopeSpy,
      saveAttribution: saveAttributionSpy,
      setShowAttributionModal: setShowAttributionModalSpy,
      id: null,
      name: 'Must State Changes',
      originalAttributionText: '',
      attributionText: '',
      availableScopes: {
        values: [
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      originalScope: 'ROOT_ORGANIZATION_ID',
      scope: 'ROOT_ORGANIZATION_ID',
      error: null,
      saveAttributionSubmitMask: null,
      showAttributionModal: false,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationAttributionTile, minimalProps);
  });

  it('renders a header with a label matching the license obligation name', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-h2')).toHaveText('Stated Changes');
  });

  it('renders Additional Attributions header if there is not a name', function () {
    const wrapper = getShallowComponent({ name: null });
    expect(wrapper.find('.nx-h2')).toHaveText('Additional Attributions');
  });

  it('renders an Add button if there is no attribution', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Add');
  });

  it('renders an Edit button if there is an attribution', function () {
    const wrapper = getShallowComponent({ id: 'id' });
    expect(wrapper.find('.nx-tile__actions span')).toHaveText('Edit');
  });

  it('renders an Add modal title if there is no attribution', function () {
    const wrapper = getShallowComponent({ showAttributionModal: true });
    expect(wrapper.find('.nx-modal-header')).toHaveText('Add Stated Changes');
  });

  it('renders an Edit modal title if there is an attribution', function () {
    const wrapper = getShallowComponent({
      id: 'id',
      showAttributionModal: true,
    });
    expect(wrapper.find('.nx-modal-header')).toHaveText('Edit Stated Changes');
  });

  it('renders appropriate text if there is no attribution', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('.nx-tile-content')).toHaveText('No attribution for stated changes added');
  });

  it('renders `None added` as content if there is no attribution and no name', function () {
    const wrapper = getShallowComponent({ name: null });
    expect(wrapper.find('.nx-tile-content')).toHaveText('None added');
  });

  it('renders the original attribution text as content if there is an attribution', function () {
    const attribution = {
      id: 'id',
      originalAttributionText: 'Some attribution text.',
    };
    const wrapper = getShallowComponent(attribution);
    expect(wrapper.find('.nx-tile-content')).toHaveText(attribution.originalAttributionText);
  });

  it('renders the modal with root org data', function () {
    const wrapper = getShallowComponent({ showAttributionModal: true });
    expect(wrapper.find('.nx-modal-header')).toHaveText('Add Stated Changes');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', '');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(1);
    expect(options.at(0)).toHaveText('Organization - Root Organization');
  });

  it('renders the modal with org data', function () {
    let orgData = {
      attributionText: 'Some obligation attribution text.',
      availableScopes: {
        values: [
          { id: 'orgId', name: 'org', label: 'Organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      showAttributionModal: true,
    };
    const wrapper = getShallowComponent(orgData);
    expect(wrapper.find('.nx-modal-header')).toHaveText('Add Stated Changes');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', 'Some obligation attribution text.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(2);
    expect(options.at(0)).toHaveText('Organization - org');
    expect(options.at(1)).toHaveText('Organization - Root Organization');
  });

  it('renders the modal with app data', function () {
    let appData = {
      attributionText: 'Some obligation attribution text.',
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', label: 'Application' },
          { id: 'orgId', name: 'org', label: 'Organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      showAttributionModal: true,
    };
    const wrapper = getShallowComponent(appData);
    expect(wrapper.find('.nx-modal-header')).toHaveText('Add Stated Changes');
    expect(wrapper.find(NxTextInput)).toHaveProp('value', 'Some obligation attribution text.');
    let select = wrapper.find('select');
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    let options = wrapper.find('option');
    expect(options.length).toBe(3);
    expect(options.at(0)).toHaveText('Application - app');
    expect(options.at(1)).toHaveText('Organization - org');
    expect(options.at(2)).toHaveText('Organization - Root Organization');
  });
});
