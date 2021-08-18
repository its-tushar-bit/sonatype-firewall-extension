/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import ComponentLicenseOverviewTile from '../../../../main/frontend/legal/license/ComponentLicenseOverviewTile';
import { licenseState } from './licenseCommonState';

describe('ComponentLicenseOverviewTile component', function () {
  let getShallowComponent;

  const minimalProps = licenseState;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(ComponentLicenseOverviewTile, minimalProps);
  });

  it('renders the lists of licenses', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('#component-license-overview__declared-licenses')).toHaveText('GPL');
    expect(wrapper.find('#component-license-overview__effective-licenses')).toHaveText('GPL, GPL-2, GPL or GPL-2');
    expect(wrapper.find('#component-license-overview__observed-licenses')).toHaveText('GPL-2');
    expect(wrapper.find('#component-license-overview__effective-license-status')).toHaveText('Selected');
  });
});
