/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../../../enzymeUtils';
import LicenseFilesDetailsList from '../../../../../main/frontend/legal/files/licenses/LicenseFilesDetailsList';

describe('LicenseFilesDetailsList component', function () {
  let getShallowComponent;

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
    $state: {
      get: () => '',
      href: () => '',
    },
  };

  beforeEach(function () {
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
});
