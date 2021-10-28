/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import LicenseList from '../../../../main/frontend/legal/license/LicenseList';
import { licenseState } from './licenseCommonState';

describe('LicenseList component', function () {
  let getShallowComponent;

  const minimalProps = licenseState;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseList, minimalProps);
  });

  it('renders the list of licenses', function () {
    const wrapper = getShallowComponent();
    let licenses = wrapper.find('li.nx-list__item');
    expect(licenses.length).toBe(2);
    expect(licenses.at(0)).toHaveText('GPL<NxThreatIndicator />Weak');
    expect(licenses.at(1)).toHaveText('GPL-2<NxThreatIndicator />Weak');
  });

  it('renders no licenses when there is no metadata', function () {
    const wrapper = enzymeUtils.getShallowComponent(LicenseList, {
      ...minimalProps,
      licenseLegalMetadata: [],
    })();
    let licenses = wrapper.find('li.nx-list__item');
    expect(licenses.length).toBe(0);
  });
});
