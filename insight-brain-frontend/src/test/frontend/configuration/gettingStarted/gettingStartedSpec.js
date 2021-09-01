/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxLoadWrapper } from '@sonatype/react-shared-components';
import GettingStarted from '../../../../main/frontend/configuration/gettingStarted/GettingStarted';
import ProductLicenseSummary from '../../../../main/frontend/configuration/gettingStarted/components/ProductLicenseSummary';
import SystemSetup from '../../../../main/frontend/configuration/gettingStarted/components/SystemSetup';
import * as enzymeUtils from '../../enzymeUtils';

describe('gettingStarted', function () {
  let initialProps, getShallow, mockLoad;

  beforeEach(() => {
    mockLoad = jasmine.createSpy('load');
    initialProps = {
      load: mockLoad,
      loading: true,
      loadError: null,
      isDataLoaded: false,
      validPermissions: [],
      isAuthorizedToViewSystemSetup: false,
      shouldDisplayHdsUnreachable: false,
      hdsUnreachableErrorMessage: null,
      hdsUnreachableIncidentId: null,
      license: null,
      prevState: { prevPage: { url: 'test' } },
    };
    getShallow = enzymeUtils.getShallowComponent(GettingStarted, initialProps);
  });

  it('renders a component with nx-page-main class', () => {
    expect(getShallow().find('.nx-page-main')).toExist();
  });

  describe('on load', function () {
    it('calls load function', () => {
      const getMountedComponent = enzymeUtils.getMountedComponent(GettingStarted, initialProps);
      const mountedComponent = getMountedComponent();

      expect(mockLoad).toHaveBeenCalledTimes(1);
      mountedComponent.unmount();
    });

    it('sonatype services were reached', function () {
      expect(getShallow().find('#connectivity-requirements')).not.toExist();
    });

    it('License data loaded', function () {
      const shallowComponent = getShallow({
        loading: false,
        isDataLoaded: true,
        daysToExpiration: 5,
        expiryDate: 'August 01, 2021',
        isAdmin: true,
        license: {
          productEdition: 'Lifecycle',
          fingerprint: '99c9cd6be744c30439b4260010bf14d7e2c3013a',
          expiryTimestamp: 1627862400000,
          licensedUsersToDisplay: 100,
          applicationLimitToDisplay: null,
          applicationCountToDisplay: null,
          firewallUsersToDisplay: 100,
          contactName: 'Nick Cook',
          contactCompany: 'Sonatype Inc',
          contactEmail: 'ncook@sonatype.com',
          products: [
            'Nexus Lifecycle',
            'Nexus Firewall',
            'Nexus Firewall for Artifactory',
            'Nexus Advanced Development Pack',
          ],
        },
      });
      const loadWrapper = shallowComponent.find(ProductLicenseSummary);
      expect(loadWrapper).toExist();
    });
  });

  describe('Authorizations', function () {
    it('has not valid permissions', function () {
      const loadError =
        'It appears you do not have permission to access this page.\n  If you believe this to be incorrect please contact your administrator.';
      const shallowComponent = getShallow({ loadError, validPermissions: [] });
      const loadWrapper = shallowComponent.find(NxLoadWrapper);
      expect(loadWrapper).toHaveProp('error', loadError);
    });

    it('is authorized to view system setup', function () {
      const validPermissions = ['CONFIGURE_SYSTEM', 'ADD_APPLICATION'];
      const shallowComponent = getShallow({ validPermissions, isAuthorizedToViewSystemSetup: true });
      const loadWrapper = shallowComponent.find(SystemSetup);
      expect(loadWrapper).toExist();
    });
  });
});
