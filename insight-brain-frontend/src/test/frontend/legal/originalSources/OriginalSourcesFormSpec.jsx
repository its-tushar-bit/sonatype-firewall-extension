/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../enzymeUtils';
import { NxForm, NxModal, NxToggle, NxTextInput } from '@sonatype/react-shared-components';
import { pathSet } from '../../../../main/frontend/util/jsUtil';
import ObligationStatusComponent from '../../../../main/frontend/legal/shared/ObligationStatusComponent';
import OriginalSourcesForm from 'MainRoot/legal/originalSources/OriginalSourcesForm';

describe('OriginalSourcesForm component', function () {
  let getShallowComponent, mountedComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        sourceLinks: [
          {
            id: '9ebd06ff0e5746d0abfec3d47e062881',
            content: 'source1',
            status: 'enabled',
          },
          {
            id: '9ebd06ff0e5746d0abfec3d47e062882',
            content: 'source2',
            status: 'disabled',
          },
        ],
      },
    },
    availableScopes: {
      values: [
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          label: 'Organization',
        },
        {
          id: 'someOrg',
          name: 'Some Other Organization',
          label: 'Organization',
        },
        {
          id: 'some-application-id',
          name: 'Some Application',
          label: 'Application',
        },
      ],
    },
    existingObligation: {
      id: 'd387da0b87a9428fbc352f437c8294cf',
      name: 'Required Disclosure of Original Source Code with Distribution',
      status: 'FLAGGED',
      originalStatus: 'FLAGGED',
      comment: 'Test comment',
      ownerId: 'ROOT_ORGANIZATION_ID',
    },
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(OriginalSourcesForm, minimalProps);
  });

  afterEach(function () {
    if (mountedComponent) {
      mountedComponent.unmount();
    }

    mountedComponent = null;
  });

  it('returns an NxModal component', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxModal)).toExist();
  });

  it('checks that sources are visible', function () {
    let wrapper = getShallowComponent();
    const sourceLinkTextInputs = wrapper.find(NxTextInput);
    expect(sourceLinkTextInputs.length).toBe(2);
    expect(sourceLinkTextInputs.at(0)).toHaveProp('value', 'source1');
    expect(sourceLinkTextInputs.at(0)).toHaveProp('isPristine', true);
    expect(sourceLinkTextInputs.at(0)).toHaveProp('disabled', false);
    expect(sourceLinkTextInputs.at(1)).toHaveProp('value', 'source2');
    expect(sourceLinkTextInputs.at(1)).toHaveProp('isPristine', true);
    expect(sourceLinkTextInputs.at(1)).toHaveProp('disabled', true);

    const sourceToggles = wrapper.find(NxToggle);
    expect(sourceToggles.length).toBe(2);

    expect(sourceToggles.at(0)).toHaveProp('isChecked', true);
    expect(sourceToggles.at(1)).toHaveProp('isChecked', false);
  });

  it('displays the obligation status if there is a matching obligation', function () {
    let wrapper = getShallowComponent();
    const statusDropdown = wrapper.find(ObligationStatusComponent);
    const propObligation = statusDropdown.prop('existingObligation');
    expect(propObligation.status).toBe('FLAGGED');
  });

  it('does not display the obligation status if there is not a matching obligation', function () {
    const props = pathSet(['existingObligation'], null, minimalProps);
    let wrapper = getShallowComponent(props);
    expect(wrapper.find(ObligationStatusComponent)).not.toExist();
  });

  it('modify toggle', function () {
    let wrapper = getShallowComponent();
    let sourceLinkToggles = wrapper.find(NxToggle);
    sourceLinkToggles.at(0).simulate('change', { target: { checked: false } });

    const sourceTextInputs = wrapper.find(NxTextInput);
    expect(sourceTextInputs.length).toBe(2);
    expect(sourceTextInputs.at(0)).toHaveProp('value', 'source1');
    expect(sourceTextInputs.at(0)).toHaveProp('disabled', true);
    expect(sourceTextInputs.at(1)).toHaveProp('value', 'source2');
    expect(sourceTextInputs.at(1)).toHaveProp('disabled', true);

    sourceLinkToggles = wrapper.find(NxToggle);
    expect(sourceLinkToggles.length).toBe(2);

    expect(sourceLinkToggles.at(0)).toHaveProp('isChecked', false);
    expect(sourceLinkToggles.at(1)).toHaveProp('isChecked', false);
  });

  it('correctly adds a new source', function () {
    let wrapper = getShallowComponent();
    let sourceLinkTextInputs = wrapper.find(NxTextInput);
    let sourceLinkToggles = wrapper.find(NxToggle);
    expect(sourceLinkTextInputs.length).toBe(2);
    expect(sourceLinkToggles.length).toBe(2);
    const addSourceButton = wrapper.find('#add-source-link');
    addSourceButton.simulate('click');

    sourceLinkTextInputs = wrapper.find(NxTextInput);
    sourceLinkToggles = wrapper.find(NxToggle);
    expect(sourceLinkTextInputs.length).toBe(3);
    expect(sourceLinkToggles.length).toBe(3);
    expect(sourceLinkTextInputs.at(2)).toHaveProp('isPristine', true);
  });

  it('modify source value', function () {
    let wrapper = getShallowComponent();
    let sourceLinkTextInputs = wrapper.find(NxTextInput);
    let form = wrapper.find(NxForm);
    expect(form).toExist();

    expect(form).toHaveProp('validationErrors', 'No modifications');

    sourceLinkTextInputs.at(0).simulate('change', 'Newly updated source1');

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);

    sourceLinkTextInputs = wrapper.find(NxTextInput);
    expect(sourceLinkTextInputs.at(0)).toHaveProp('value', 'Newly updated source1');
    expect(sourceLinkTextInputs.at(0)).toHaveProp('isPristine', false);
  });

  it('modify scope value', function () {
    let wrapper = getShallowComponent();
    let scopeSelect = wrapper.find('#edit-original-sources-scope-selection');
    let form = wrapper.find(NxForm);
    expect(form).toExist();
    expect(scopeSelect).toExist();
    expect(scopeSelect).toHaveProp('value', 'ROOT_ORGANIZATION_ID');

    expect(form).toHaveProp('validationErrors', 'No modifications');

    scopeSelect.simulate('change', {
      target: { value: 'some-application-id' },
    });

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);
  });

  it('Existing override at higher scope', function () {
    let props = pathSet(['component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'], 'someOrg', minimalProps);
    props = pathSet(['component', 'licenseLegalData', 'componentCopyrightId'], 'ccId', props);
    let wrapper = getShallowComponent(props);
    let scopeSelect = wrapper.find('#edit-original-sources-scope-selection');
    let form = wrapper.find(NxForm);
    expect(form).toExist();
    expect(scopeSelect).toExist();
    expect(scopeSelect).toHaveProp('value', 'ROOT_ORGANIZATION_ID');

    expect(form).toHaveProp('validationErrors', 'No modifications');

    scopeSelect.simulate('change', {
      target: { value: 'some-application-id' },
    });

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);
  });
});
