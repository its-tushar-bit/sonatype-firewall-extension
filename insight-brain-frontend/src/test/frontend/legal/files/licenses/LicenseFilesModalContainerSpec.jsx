/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import LicenseFilesModal from '../../../../../main/frontend/legal/files/licenses/LicenseFilesModal';

describe('LicenseFilesModalContainer', function () {
  let store,
    state,
    vdom,
    LicenseFilesModalContainer,
    cancelLicensesModalSpy,
    setLicenseContentSpy,
    setLicenseStatusSpy,
    addLicenseSpy,
    setLicensesScopeSpy,
    saveLicensesSpy;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              originalComponentLicensesScopeOwnerId: 'originalComponentLicenseFilesScopeOwnerId',
              componentLicensesScopeOwnerId: 'componentLicenseFilesScopeOwnerId',
              licenseFiles: 'licenseFiles',
              licensesError: 'licensesError',
              saveLicenseFilesSubmitMask: 'saveLicenseFilesSubmitMask',
              obligations: [
                {
                  name: 'Inclusion of License',
                  status: 'FLAGGED',
                },
                {
                  name: 'Something else',
                },
              ],
            },
          },
        },
        availableScopes: 'availableScopes',
      },
    };
    cancelLicensesModalSpy = jasmine.createSpy().and.returnValue({ type: 'cancelLicensesModalSpy' });
    setLicenseContentSpy = jasmine.createSpy().and.returnValue({ type: 'setLicenseContentSpy' });
    setLicenseStatusSpy = jasmine.createSpy().and.returnValue({ type: 'setLicenseStatusSpy' });
    addLicenseSpy = jasmine.createSpy().and.returnValue({ type: 'addLicenseSpy' });
    setLicensesScopeSpy = jasmine.createSpy().and.returnValue({ type: 'setLicensesScopeSpy' });
    saveLicensesSpy = jasmine.createSpy().and.returnValue({ type: 'saveLicensesSpy' });
    LicenseFilesModalContainer = require('inject-loader!../../../../../main/frontend/legal/files/licenses/LicenseFilesModalContainer')(
      {
        '../advancedLegalFileActions': {
          cancelLicenseFilesModal: cancelLicensesModalSpy,
          setLicenseFileContent: setLicenseContentSpy,
          setLicenseFileStatus: setLicenseStatusSpy,
          addLicenseFile: addLicenseSpy,
          setLicenseFilesScope: setLicensesScopeSpy,
          saveLicenseFiles: saveLicensesSpy,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <LicenseFilesModalContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scope', 'componentLicenseFilesScopeOwnerId');
    expect(wrapper).toHaveProp('originalScope', 'originalComponentLicenseFilesScopeOwnerId');
    expect(wrapper).toHaveProp('availableScopes', 'availableScopes');
    expect(wrapper).toHaveProp('licenses', 'licenseFiles');
    expect(wrapper).toHaveProp('error', 'licensesError');
    expect(wrapper).toHaveProp('submitMaskState', 'saveLicenseFilesSubmitMask');
  });

  it('correctly maps the action creators to the LicenseFilesModalContainer props', function () {
    const wrapper = shallow(vdom).dive();
    expect(wrapper.prop('cancelLicenseFilesModal')()).toEqual({
      type: 'cancelLicensesModalSpy',
    });
    expect(wrapper.prop('setLicenseFileContent')()).toEqual({
      type: 'setLicenseContentSpy',
    });
    expect(wrapper.prop('setLicenseFileStatus')()).toEqual({
      type: 'setLicenseStatusSpy',
    });
    expect(wrapper.prop('addLicenseFile')()).toEqual({ type: 'addLicenseSpy' });
    expect(wrapper.prop('setLicenseFilesScope')()).toEqual({
      type: 'setLicensesScopeSpy',
    });
    expect(wrapper.prop('saveLicenseFiles')()).toEqual({ type: 'saveLicensesSpy' });
  });

  it('renders the LicenseFilesModal component', function () {
    const licenseFilesModal = shallow(vdom).find(LicenseFilesModal);
    expect(licenseFilesModal).toExist();
  });
});
