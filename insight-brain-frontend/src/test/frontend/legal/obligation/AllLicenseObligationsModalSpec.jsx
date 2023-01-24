/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../enzymeUtils';
import { NxDropdown, NxTextInput, NxFormSelect } from '@sonatype/react-shared-components';
import AllLicenseObligationsModal from '../../../../main/frontend/legal/obligation/AllLicenseObligationsModal';

describe('AllLicenseObligationsModal', function () {
  let getShallowComponent,
    minimalProps,
    cancelAllObligationsModalSpy,
    createObligationStatusIconSpy,
    saveAllObligationsSpy;

  beforeEach(function () {
    cancelAllObligationsModalSpy = jasmine.createSpy('cancelObligationsModalSpy');
    createObligationStatusIconSpy = jasmine.createSpy('createObligationStatusIconSpy').and.returnValue('possibleIcon');
    saveAllObligationsSpy = jasmine.createSpy('saveAllObligationsSpy');

    minimalProps = {
      cancelAllObligationsModal: cancelAllObligationsModalSpy,
      createObligationStatusIcon: createObligationStatusIconSpy,
      saveAllObligations: saveAllObligationsSpy,
      availableScopes: {
        loading: false,
        values: [
          {
            id: 'appId',
            name: 'app',
            type: 'application',
            label: 'Application',
          },
          {
            id: 'orgId',
            name: 'org',
            type: 'organization',
            label: 'Organization',
          },
          {
            id: 'ROOT_ORGANIZATION_ID',
            type: 'organization',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
    };
    getShallowComponent = enzymeUtils.getShallowComponent(AllLicenseObligationsModal, minimalProps);
  });

  it('renders the modal with the FULFILLED status', function () {
    const wrapper = getShallowComponent();
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

  it('renders the modal with the correct comment', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxTextInput)).toHaveProp('value', '');
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
});
