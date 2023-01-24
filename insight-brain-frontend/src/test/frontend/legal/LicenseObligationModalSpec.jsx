/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationModal from '../../../main/frontend/legal/obligation/LicenseObligationModal';
import { NxDropdown, NxFormSelect, NxStatefulForm, NxTextInput } from '@sonatype/react-shared-components';

describe('LicenseObligationModal', function () {
  let getShallowComponent,
    minimalProps,
    setObligationStatusSpy,
    setObligationCommentSpy,
    setObligationScopeSpy,
    saveObligationSpy,
    cancelObligationModalSpy,
    createObligationStatusIconSpy;

  beforeEach(function () {
    setObligationStatusSpy = jasmine.createSpy('setObligationStatusSpy');
    setObligationCommentSpy = jasmine.createSpy('setObligationCommentSpy');
    setObligationScopeSpy = jasmine.createSpy('setObligationScopeSpy');
    saveObligationSpy = jasmine.createSpy('saveObligationSpy');
    cancelObligationModalSpy = jasmine.createSpy('cancelObligationModalSpy');
    createObligationStatusIconSpy = jasmine.createSpy('createObligationStatusIconSpy').and.returnValue('possibleIcon');
    minimalProps = {
      setObligationStatus: setObligationStatusSpy,
      setObligationComment: setObligationCommentSpy,
      setObligationScope: setObligationScopeSpy,
      saveObligation: saveObligationSpy,
      cancelObligationModal: cancelObligationModalSpy,
      createObligationStatusIcon: createObligationStatusIconSpy,
      licenseObligation: {
        name: 'name',
        originalStatus: 'OPEN',
        status: 'OPEN',
        originalComment: 'comment',
        comment: 'comment',
        originalOwnerId: 'ROOT_ORGANIZATION_ID',
        ownerId: 'ROOT_ORGANIZATION_ID',
        attributions: [],
      },
      availableScopes: {
        loading: false,
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
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationModal, minimalProps);
  });

  it('renders the modal with the OPEN status', function () {
    const wrapper = getShallowComponent();
    const statusDropdown = wrapper.find(NxDropdown);
    const statusLabelChildren = statusDropdown.prop('label').props['children'];
    expect(statusLabelChildren[0]).toBe('possibleIcon');
    expect(statusLabelChildren[1].props['children']).toEqual('Unreviewed');
    const statusOptions = statusDropdown.find('button');
    expect(statusOptions.length).toBe(3);
    expect(statusOptions.at(0).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(0).childAt(1)).toHaveText('Fulfilled');
    expect(statusOptions.at(1).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(1).childAt(1)).toHaveText('Flagged');
    expect(statusOptions.at(2).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(2).childAt(1)).toHaveText('Not Applicable');
  });

  it('renders the modal with the FULFILLED status', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        status: 'FULFILLED',
      },
    });
    const statusDropdown = wrapper.find(NxDropdown);
    const statusLabelChildren = statusDropdown.prop('label').props['children'];
    expect(statusLabelChildren[0]).toBe('possibleIcon');
    expect(statusLabelChildren[1].props['children']).toEqual('Fulfilled');
    const statusOptions = statusDropdown.find('button');
    expect(statusOptions.length).toBe(3);
    expect(statusOptions.at(0).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(0).childAt(1)).toHaveText('Flagged');
    expect(statusOptions.at(1).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(1).childAt(1)).toHaveText('Not Applicable');
    expect(statusOptions.at(2).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(2).childAt(1)).toHaveText('Unreviewed');
  });

  it('renders the modal with the FLAGGED status', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        status: 'FLAGGED',
      },
    });
    const statusDropdown = wrapper.find(NxDropdown);
    const statusLabelChildren = statusDropdown.prop('label').props['children'];
    expect(statusLabelChildren[0]).toBe('possibleIcon');
    expect(statusLabelChildren[1].props['children']).toEqual('Flagged');
    const statusOptions = statusDropdown.find('button');
    expect(statusOptions.length).toBe(3);
    expect(statusOptions.at(0).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(0).childAt(1)).toHaveText('Fulfilled');
    expect(statusOptions.at(1).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(1).childAt(1)).toHaveText('Not Applicable');
    expect(statusOptions.at(2).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(2).childAt(1)).toHaveText('Unreviewed');
  });

  it('renders the modal with the IGNORED status', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        status: 'IGNORED',
      },
    });
    const statusDropdown = wrapper.find(NxDropdown);
    const statusLabelChildren = statusDropdown.prop('label').props['children'];
    expect(statusLabelChildren[0]).toBe('possibleIcon');
    expect(statusLabelChildren[1].props['children']).toEqual('Not Applicable');
    const statusOptions = statusDropdown.find('button');
    expect(statusOptions.length).toBe(3);
    expect(statusOptions.at(0).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(0).childAt(1)).toHaveText('Fulfilled');
    expect(statusOptions.at(1).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(1).childAt(1)).toHaveText('Flagged');
    expect(statusOptions.at(2).childAt(0)).toHaveText('possibleIcon');
    expect(statusOptions.at(2).childAt(1)).toHaveText('Unreviewed');
  });

  it('renders the modal with the correct comment', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxTextInput)).toHaveProp('value', 'comment');
  });

  it('renders the modal with the correct scope', function () {
    const wrapper = getShallowComponent();
    const select = wrapper.find(NxFormSelect);
    expect(select).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
    const options = wrapper.find('option');
    expect(options.length).toBe(3);
    expect(options.at(0)).toHaveText('Application - app');
    expect(options.at(1)).toHaveText('Organization - org');
    expect(options.at(2)).toHaveText('Organization - Root Organization');
  });

  it('disables the submit button if nothing is dirty', function () {
    const wrapper = getShallowComponent();
    const result = wrapper.find(NxStatefulForm);
    expect(result).toHaveProp('validationErrors', 'Must change obligation status, or comments, or scope.');
  });

  it('enables the submit button if the status is dirty', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        status: 'IGNORED',
      },
    });
    const result = wrapper.find(NxStatefulForm);
    expect(result).toHaveProp('validationErrors', undefined);
  });

  it('enables the submit button if the comment is dirty', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        comment: 'other',
      },
    });
    const result = wrapper.find(NxStatefulForm);
    expect(result).toHaveProp('validationErrors', undefined);
  });

  it('enables the submit button if the ownerId is dirty', function () {
    const wrapper = getShallowComponent({
      ...minimalProps,
      licenseObligation: {
        ...minimalProps.licenseObligation,
        ownerId: 'other',
      },
    });
    const result = wrapper.find(NxStatefulForm);
    expect(result).toHaveProp('validationErrors', undefined);
  });
});
