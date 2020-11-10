/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseDetailsTile from '../../../main/frontend/legal/LicenseDetailsTile';

describe('LicenseDetailsTile component', function() {

  let getShallowComponent;

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseDetailsTile);
  });

  it('renders a header with label `License Details`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Licenses');
  });
});
