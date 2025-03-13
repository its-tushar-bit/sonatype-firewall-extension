/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import LicenseFilesDetailsList from '../../../../../main/frontend/legal/files/licenses/LicenseFilesDetailsList';

describe('LicenseFilesDetailsList component', function () {
  let getShallowComponent, $state;

  const minimalProps = {
    component: {
      licenseLegalData: {
        licenseFiles: [
          {
            relPath: '/test/LICENSE',
            content: 'Apache-2.0',
            status: 'enabled',
          },
          {
            relPath: '/test/sub/license.txt',
            content: 'Apache-2.0-with-CPE',
            status: 'disabled',
          },
        ],
      },
    },
    hash: 'testHash',
  };

  beforeEach(function () {
    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    minimalProps.$state = $state;

    getShallowComponent = enzymeUtils.getShallowComponent(LicenseFilesDetailsList, minimalProps);
  });

  it('renders the given license files', function () {
    const wrapper = getShallowComponent();
    let licenseTexts = wrapper.find('div.nx-list__text');
    let licenseTextStatus = wrapper.find('div.nx-list__subtext');
    expect(licenseTexts.length).toBe(2);
    expect(licenseTexts.at(0).text()).toContain('/test/LICENSE');
    expect(licenseTextStatus.at(0).text()).toContain('Included in attribution report');
    expect(licenseTexts.at(1).text()).toContain('/test/sub/license.txt');
    expect(licenseTextStatus.at(1).text()).toContain('Excluded from the report');
  });

  it('renders the given license files links by hash', function () {
    const testLicenseFileLinksByHash = (props, expectedHrefPrefix) => {
      const wrapper = getShallowComponent(props);
      let licenseFileLinks = wrapper.find('a.nx-list__link');

      let licenseFileLink = licenseFileLinks.at(0);
      expect(licenseFileLink).toHaveProp('href', `${expectedHrefPrefix}-{"hash":"testHash","licenseIndex":0}`);

      licenseFileLink = licenseFileLinks.at(1);
      expect(licenseFileLink).toHaveProp('href', `${expectedHrefPrefix}-{"hash":"testHash","licenseIndex":1}`);
    };

    testLicenseFileLinksByHash(minimalProps, 'legal.componentLicenseFilesDetails.licenseFilesDetails');
    testLicenseFileLinksByHash(
      {
        ...minimalProps,
        isSbomManager: true,
      },
      'sbomManager.legal.componentLicenseFilesDetails.licenseFilesDetails'
    );
  });

  it('renders the given license files links by component identifier', function () {
    const testLicenseFileLinksByComponentIdentifier = (props, expectedHrefPrefix) => {
      const wrapper = getShallowComponent(props);
      let licenseFileLinks = wrapper.find('a.nx-list__link');

      let licenseFileLink = licenseFileLinks.at(0);
      expect(licenseFileLink).toHaveProp(
        'href',
        `${expectedHrefPrefix}-{"componentIdentifier":"testComponentIdentifier","licenseIndex":0}`
      );

      licenseFileLink = licenseFileLinks.at(1);
      expect(licenseFileLink).toHaveProp(
        'href',
        `${expectedHrefPrefix}-{"componentIdentifier":"testComponentIdentifier","licenseIndex":1}`
      );
    };

    const props = {
      ...minimalProps,
      hash: undefined,
      componentIdentifier: 'testComponentIdentifier',
    };

    testLicenseFileLinksByComponentIdentifier(
      props,
      'legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails'
    );

    testLicenseFileLinksByComponentIdentifier(
      {
        ...props,
        isSbomManager: true,
      },
      'sbomManager.legal.componentLicenseFilesDetailsByComponentIdentifier.licenseFilesDetails'
    );
  });
});
