/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseDetailsTile from '../../../main/frontend/legal/LicenseDetailsTile';

describe('LicenseDetailsTile component', function () {
  let getShallowComponent;

  const minimalProps = {
    licenseNames: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
  };

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(
      LicenseDetailsTile,
      minimalProps
    );
  });

  it('renders a header with label `License Details`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Licenses');
  });

  it('renders the given licenses', function () {
    const wrapper = getShallowComponent();
    let licenseSpans = wrapper.find('span.nx-list__text');
    expect(licenseSpans.length).toBe(3);
    expect(licenseSpans.at(0)).toHaveText('License-1.0');
    expect(licenseSpans.at(1)).toHaveText('License-2.0');
    expect(licenseSpans.at(2)).toHaveText('License-1.0-License-2.0');
  });

  it('renders None found if there are no licenses', function () {
    const wrapper = enzymeUtils.getShallowComponent(LicenseDetailsTile, {
      licenseNames: [],
    })();
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
  });
});
