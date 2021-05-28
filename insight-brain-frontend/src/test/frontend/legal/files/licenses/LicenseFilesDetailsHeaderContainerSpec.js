/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import { licenseFilesState } from './licenseCommonState';
import LicenseFilesDetailsHeader from '../../../../../main/frontend/legal/files/licenses/LicenseFilesDetailsHeader';
import { mergeDeepRight } from 'ramda';

describe('LicenseFilesDetailsHeaderContainer', function () {
  let store, state, vdom, LicenseFilesDetailsHeaderContainer, loadComponentAndLicenseDetailsMock;

  beforeEach(function () {
    state = licenseFilesState;
    loadComponentAndLicenseDetailsMock = jasmine
      .createSpy('loadComponentAndLicenseDetails')
      .and.returnValue({ type: 'FOO' });
    LicenseFilesDetailsHeaderContainer = require('inject-loader!../../../../../main/frontend/legal/files/licenses/LicenseFilesDetailsHeaderContainer')(
      {
        './componentLicenseFilesDetailsActions': {
          loadComponentAndLicenseDetails: loadComponentAndLicenseDetailsMock,
        },
      }
    ).default;

    store = configureStore()(() => state);
    vdom = <LicenseFilesDetailsHeaderContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).not.toHaveProp('stageTypeId');
    expect(wrapper).toHaveProp('licenseIndex', '0');
  });

  it('maps the state slice to props with stageTypeId routing', () => {
    store = configureStore()(() => mergeDeepRight(state, { router: { currentParams: { stageTypeId: 'build' } } }));
    vdom = <LicenseFilesDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('stageTypeId', 'build');
    expect(wrapper).toHaveProp('licenseIndex', '0');
  });

  it('correctly maps the action creators to the LicenseFileDetailsHeaderContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadComponentAndLicenseDetailsCreator = wrapper.prop('loadComponentAndLicenseDetails');

    expect(loadComponentAndLicenseDetailsCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadComponentAndLicenseDetailsCreator('test');
    expect(store.getActions()).toEqual([{ type: 'FOO' }]);
  });

  it('renders LicenseFileDetailsHeader component', function () {
    const licenseDetailsHeader = shallow(vdom).find(LicenseFilesDetailsHeader);
    expect(licenseDetailsHeader).toExist();
  });

  it('handles route switch when current state has changed', () => {
    state = {
      ...licenseFilesState,
      router: {
        currentState: { name: 'ComponentOverview' },
        currentParams: { hash: 'fooHash', applicationPublicId: 'appId' },
        prevParams: {
          hash: 'fooHash',
          ownerType: 'organization',
          ownerId: 'org',
          licenseIndex: '0',
        },
        prevState: { name: 'componentLicenseFilesDetails.licenseFilesDetails' },
      },
    };

    store = configureStore()(() => state);
    vdom = <LicenseFilesDetailsHeaderContainer store={store} />;

    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('hash', 'fooHash');
    expect(wrapper).toHaveProp('ownerType', 'organization');
    expect(wrapper).toHaveProp('ownerId', 'org');
    expect(wrapper).toHaveProp('licenseIndex', '0');
  });
});
