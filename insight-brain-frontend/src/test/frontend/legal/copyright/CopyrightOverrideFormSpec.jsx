/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import CopyrightOverrideForm from '../../../../main/frontend/legal/copyright/CopyrightOverrideForm';
import * as enzymeUtils from '../../enzymeUtils';
import {NxForm, NxModal, NxToggle, NxTextInput} from '@sonatype/react-shared-components';
import {pathSet} from '../../../../main/frontend/util/jsUtil';
import ObligationStatusComponent from '../../../../main/frontend/legal/shared/ObligationStatusComponent';

describe('CopyrightOverrideForm component', function() {
  let getShallowComponent,
      mountedComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        copyrights:
            [
              {
                id: null,
                content: 'Copyright 2043',
                originalContentHash: 'originalContentHash1',
                status: 'enabled'
              },
              {
                id: null,
                content: 'Copyright 2',
                originalContentHash: 'originalContentHash2',
                status: 'disabled'
              }
            ],
        componentCopyrightScopeOwnerId: null,
        componentCopyrightId: null
      }
    },
    availableScopes: {
      values: [
        {id: 'ROOT_ORGANIZATION_ID', name: 'Root Organization', label: 'Organization'},
        {id: 'someOrg', name: 'Some Other Organization', label: 'Organization'},
        {id: 'some-application-id', name: 'Some Application', label: 'Application'}
      ]
    },
    existingObligation: {
      'id': 'd387da0b87a9428fbc352f437c8294cf',
      'name': 'Inclusion of Copyright',
      'status': 'FLAGGED',
      'originalStatus': 'FLAGGED',
      'comment': 'Test comment',
      'ownerId': 'ROOT_ORGANIZATION_ID'
    }
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(CopyrightOverrideForm, minimalProps);
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

  it('returns an NxSubmitMask component if saveCopyrightSaving is true', function() {
    let wrapper = getShallowComponent();
    expect(wrapper.find(NxForm)).toHaveProp('submitMaskState', undefined);
    wrapper = getShallowComponent({
      submitMaskState: true
    });
    expect(wrapper.find(NxForm)).toHaveProp('submitMaskState', true);
  });

  it('checks that copyrights are visible', function() {
    let wrapper = getShallowComponent();
    const copyrightTextInputs = wrapper.find(NxTextInput);
    expect(copyrightTextInputs.length).toBe(2);
    expect(copyrightTextInputs.at(0)).toHaveProp('value', 'Copyright 2043');
    expect(copyrightTextInputs.at(0)).toHaveProp('isPristine', true);
    expect(copyrightTextInputs.at(0)).toHaveProp('disabled', false);
    expect(copyrightTextInputs.at(1)).toHaveProp('value', 'Copyright 2');
    expect(copyrightTextInputs.at(1)).toHaveProp('isPristine', true);
    expect(copyrightTextInputs.at(1)).toHaveProp('disabled', true);

    const copyrightStatusToggles = wrapper.find(NxToggle);
    expect(copyrightStatusToggles.length).toBe(2);

    expect(copyrightStatusToggles.at(0)).toHaveProp('isChecked', true);
    expect(copyrightStatusToggles.at(1)).toHaveProp('isChecked', false);
  });

  it('displays the obligation status', function() {
    let wrapper = getShallowComponent();
    const statusDropdown = wrapper.find(ObligationStatusComponent);
    const propObligation = statusDropdown.prop('existingObligation');
    expect(propObligation.status).toBe('FLAGGED');
  });

  it('modify toggle', function() {
    let wrapper = getShallowComponent();
    let copyrightStatusToggles = wrapper.find(NxToggle);
    copyrightStatusToggles.at(0).simulate('change', {target: {checked: false}});

    const copyrightTextInputs = wrapper.find(NxTextInput);
    expect(copyrightTextInputs.length).toBe(2);
    expect(copyrightTextInputs.at(0)).toHaveProp('value', 'Copyright 2043');
    expect(copyrightTextInputs.at(0)).toHaveProp('disabled', true);
    expect(copyrightTextInputs.at(1)).toHaveProp('value', 'Copyright 2');
    expect(copyrightTextInputs.at(1)).toHaveProp('disabled', true);

    copyrightStatusToggles = wrapper.find(NxToggle);
    expect(copyrightStatusToggles.length).toBe(2);

    expect(copyrightStatusToggles.at(0)).toHaveProp('isChecked', false);
    expect(copyrightStatusToggles.at(1)).toHaveProp('isChecked', false);
  });

  it('add new copyright', function() {
    let wrapper = getShallowComponent();
    let copyrightTextInputs = wrapper.find(NxTextInput);
    let copyrightStatusToggles = wrapper.find(NxToggle);
    expect(copyrightTextInputs.length).toBe(2);
    expect(copyrightStatusToggles.length).toBe(2);

    const addCopyrightButton = wrapper.find('#add-copyright');
    addCopyrightButton.simulate('click');

    copyrightTextInputs = wrapper.find(NxTextInput);
    copyrightStatusToggles = wrapper.find(NxToggle);
    expect(copyrightTextInputs.length).toBe(3);
    expect(copyrightStatusToggles.length).toBe(3);
    expect(copyrightTextInputs.at(2)).toHaveProp('isPristine', true);
  });

  it('modify copyright value', function() {
    let wrapper = getShallowComponent();
    let copyrightTextInputs = wrapper.find(NxTextInput);
    let form = wrapper.find(NxForm);
    expect(form).toExist();

    expect(form).toHaveProp('validationErrors', 'No modifications');

    copyrightTextInputs.at(0).simulate('change', 'Newly updated Copyright 3000');

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);

    copyrightTextInputs = wrapper.find(NxTextInput);
    expect(copyrightTextInputs.at(0)).toHaveProp('value', 'Newly updated Copyright 3000');
    expect(copyrightTextInputs.at(0)).toHaveProp('isPristine', false);
  });

  it('modify scope value', function() {
    let wrapper = getShallowComponent();
    let scopeSelect = wrapper.find('#edit-copyright-scope-selection');
    let form = wrapper.find(NxForm);
    expect(form).toExist();
    expect(scopeSelect).toExist();
    expect(scopeSelect).toHaveProp('value', 'ROOT_ORGANIZATION_ID');

    expect(form).toHaveProp('validationErrors', 'No modifications');

    scopeSelect.simulate('change', {target: {value: 'some-application-id'}});

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);
  });

  it('Existing override at higher scope', function() {
    let props = pathSet(['component', 'licenseLegalData', 'componentCopyrightScopeOwnerId'], 'someOrg', minimalProps);
    props = pathSet(['component', 'licenseLegalData', 'componentCopyrightId'], 'ccId', props);
    let wrapper = getShallowComponent(props);
    let scopeSelect = wrapper.find('#edit-copyright-scope-selection');
    let form = wrapper.find(NxForm);
    expect(form).toExist();
    expect(scopeSelect).toExist();
    expect(scopeSelect).toHaveProp('value', 'someOrg');

    expect(form).toHaveProp('validationErrors', 'No modifications');

    scopeSelect.simulate('change', {target: {value: 'some-application-id'}});

    form = wrapper.find(NxForm);
    expect(form).toHaveProp('validationErrors', null);
  });

});
