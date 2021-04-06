/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import LicensesModal from '../../../../../main/frontend/legal/files/licenses/LicensesModal';

describe('LicensesModalContainer', function() {
  let store,
      state,
      vdom,
      LicensesModalContainer,
      cancelLicensesModalSpy,
      setLicenseContentSpy,
      setLicenseStatusSpy,
      addLicenseSpy,
      setLicensesScopeSpy,
      saveLicensesSpy;

  beforeEach(function() {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              originalComponentLicensesScopeOwnerId: 'originalComponentLicensesScopeOwnerId',
              componentLicensesScopeOwnerId: 'componentLicensesScopeOwnerId',
              licenseFiles: 'licenseFiles',
              licensesError: 'licensesError',
              saveLicensesSubmitMask: 'saveLicensesSubmitMask',
              obligations: [
                {
                  'name': 'Inclusion of License',
                  'status': 'FLAGGED'
                },
                {
                  'name': 'Something else'
                }
              ]
            }
          }
        },
        availableScopes: 'availableScopes'
      }
    };
    cancelLicensesModalSpy = jasmine.createSpy().and.returnValue({ type: 'cancelLicensesModalSpy' });
    setLicenseContentSpy = jasmine.createSpy().and.returnValue({ type: 'setLicenseContentSpy' });
    setLicenseStatusSpy = jasmine.createSpy().and.returnValue({ type: 'setLicenseStatusSpy' });
    addLicenseSpy = jasmine.createSpy().and.returnValue({ type: 'addLicenseSpy' });
    setLicensesScopeSpy = jasmine.createSpy().and.returnValue({ type: 'setLicensesScopeSpy' });
    saveLicensesSpy = jasmine.createSpy().and.returnValue({ type: 'saveLicensesSpy' });
    LicensesModalContainer =
        require('inject-loader!../../../../../main/frontend/legal/files/licenses/LicensesModalContainer')({
          '../advancedLegalFileActions': {
            cancelLicensesModal: cancelLicensesModalSpy,
            setLicenseContent: setLicenseContentSpy,
            setLicenseStatus: setLicenseStatusSpy,
            addLicense: addLicenseSpy,
            setLicensesScope: setLicensesScopeSpy,
            saveLicenses: saveLicensesSpy
          }
        }).default;
    store = configureStore()(() => state);
    vdom = <LicensesModalContainer store={store}/>;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('scope', 'componentLicensesScopeOwnerId');
    expect(wrapper).toHaveProp('originalScope', 'originalComponentLicensesScopeOwnerId');
    expect(wrapper).toHaveProp('availableScopes', 'availableScopes');
    expect(wrapper).toHaveProp('licenses', 'licenseFiles');
    expect(wrapper).toHaveProp('error', 'licensesError');
    expect(wrapper).toHaveProp('submitMaskState', 'saveLicensesSubmitMask');
  });

  it('correctly maps the action creators to the LicensesModalContainer props', function() {
    const wrapper = shallow(vdom).dive();
    expect(wrapper.prop('cancelLicensesModal')()).toEqual({ type: 'cancelLicensesModalSpy' });
    expect(wrapper.prop('setLicenseContent')()).toEqual({ type: 'setLicenseContentSpy' });
    expect(wrapper.prop('setLicenseStatus')()).toEqual({ type: 'setLicenseStatusSpy' });
    expect(wrapper.prop('addLicense')()).toEqual({ type: 'addLicenseSpy' });
    expect(wrapper.prop('setLicensesScope')()).toEqual({ type: 'setLicensesScopeSpy' });
    expect(wrapper.prop('saveLicenses')()).toEqual({ type: 'saveLicensesSpy' });
  });

  it('renders the LicensesModal component', function() {
    const licensesModal = shallow(vdom).find(LicensesModal);
    expect(licensesModal).toExist();
  });
});
