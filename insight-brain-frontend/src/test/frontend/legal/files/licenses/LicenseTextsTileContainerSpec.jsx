/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configureStore from 'redux-mock-store';
import React from 'react';
import { shallow } from 'enzyme';
import LicenseTextsTile from '../../../../../main/frontend/legal/files/licenses/LicenseTextsTile';

describe('LicenseTextsTileContainer', function () {
  let store, state, vdom, LicenseTextsTileContainer, setShowLicensesModalSpy;

  beforeEach(function () {
    state = {
      advancedLegal: {
        component: {
          component: {
            licenseLegalData: {
              licenseFiles: 'licenseFiles',
              showLicensesModal: 'showLicensesModal',
            },
          },
        },
      },
    };
    setShowLicensesModalSpy = jasmine
      .createSpy()
      .and.returnValue({ type: 'setShowLicensesModalSpy' });

    LicenseTextsTileContainer = require('inject-loader!../../../../../main/frontend/legal/files/licenses/LicenseTextsTileContainer')(
      {
        '../advancedLegalFileActions': {
          setShowLicensesModal: setShowLicensesModalSpy,
        },
      }
    ).default;
    store = configureStore()(() => state);
    vdom = <LicenseTextsTileContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    const wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('licenseFiles', 'licenseFiles');
    expect(wrapper).toHaveProp('showLicensesModal', 'showLicensesModal');
  });

  it('correctly maps the action creators to the LicenseTextsTileContainer props', function () {
    const wrapper = shallow(vdom).dive();
    expect(wrapper.prop('setShowLicensesModal')()).toEqual({
      type: 'setShowLicensesModalSpy',
    });
  });

  it('renders the LicenseTextsTile component', function () {
    const licenseTextsTile = shallow(vdom).find(LicenseTextsTile);
    expect(licenseTextsTile).toExist();
  });
});
