/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';
import LicensesModal from '../../../../main/frontend/legal/license/LicensesModal';

describe('LicensesModalContainer', function () {
  let store,
    state,
    vdom,
    LicensesModalContainer,
    loadLicenseModalInformationMock,
    saveLicensesMock,
    setShowLicensesModalMock;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: 'component',
          licenseLegalMetadata: 'licenseLegalMetadata',
        },
        availableScopes: 'availableScopes',
      },
      router: {
        currentParams: {
          applicationPublicId: 'appId',
        },
      },
    };

    loadLicenseModalInformationMock = jasmine
      .createSpy('loadLicenseModalInformation')
      .and.returnValue({ type: 'loadLicenseModalInformationMock' });
    saveLicensesMock = jasmine.createSpy('loadLicenseModalInformation').and.returnValue({ type: 'saveLicensesMock' });
    setShowLicensesModalMock = jasmine
      .createSpy('loadLicenseModalInformation')
      .and.returnValue({ type: 'setShowLicensesModalMock' });

    LicensesModalContainer = require('inject-loader!../../../../main/frontend/legal/license/LicensesModalContainer')({
      '../files/advancedLegalFileActions': {
        loadLicenseModalInformation: loadLicenseModalInformationMock,
        saveLicenses: saveLicensesMock,
        setShowLicensesModal: setShowLicensesModalMock,
      },
    }).default;

    store = configureStore()(() => state);
    vdom = <LicensesModalContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('availableScopes', 'availableScopes');
    expect(wrapper).toHaveProp('ownerId', 'appId');
    expect(wrapper).toHaveProp('component', 'component');
    expect(wrapper).toHaveProp('licenseLegalMetadata', 'licenseLegalMetadata');
  });

  it('correctly maps the action creators to the LicensesModalContainer props', function () {
    const wrapper = shallow(vdom).dive();
    const loadLicenseModalInformationActionCreator = wrapper.prop('loadLicenseModalInformation');
    expect(loadLicenseModalInformationActionCreator).toEqual(jasmine.any(Function));
    const saveLicensesActionCreator = wrapper.prop('saveLicenses');
    expect(saveLicensesActionCreator).toEqual(jasmine.any(Function));
    const setShowLicensesModalActionCreator = wrapper.prop('setShowLicensesModal');
    expect(setShowLicensesModalActionCreator).toEqual(jasmine.any(Function));

    expect(store.getActions()).toEqual([]);
    loadLicenseModalInformationActionCreator('test');
    expect(store.getActions()).toEqual([{ type: 'loadLicenseModalInformationMock' }]);
    saveLicensesActionCreator('test');
    expect(store.getActions()[1]).toEqual({ type: 'saveLicensesMock' });
    setShowLicensesModalActionCreator('test');
    expect(store.getActions()[2]).toEqual({ type: 'setShowLicensesModalMock' });
  });

  it('renders a LicensesModal component', function () {
    const licensesModal = shallow(vdom).find(LicensesModal);
    expect(licensesModal).toExist();
  });
});
