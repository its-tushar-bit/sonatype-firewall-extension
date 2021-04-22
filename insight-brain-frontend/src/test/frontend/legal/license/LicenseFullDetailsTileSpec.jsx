/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import LicenseFullDetailsTile from '../../../../main/frontend/legal/license/LicenseFullDetailsTile';
import { licenseState } from './licenseCommonState';

describe('LicenseFullDetailsTile component', function () {
  let getShallowComponent;

  const minimalProps = licenseState;

  beforeEach(function () {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseFullDetailsTile, minimalProps);
  });

  it('renders a header with label `License Obligations`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('GPL-2 License Obligations');
  });

  it('renders the given license obligations', function () {
    const wrapper = getShallowComponent();
    let obligationTitles = wrapper.find('dt');
    let obligationBodies = wrapper.find('dd');
    expect(obligationTitles.length).toBe(2);
    expect(obligationBodies.length).toBe(2);
    expect(obligationTitles.at(0)).toHaveText('Inclusion of License');
    expect(obligationBodies.at(0)).toHaveText('distribute a copy of this License along with the Library');
    expect(obligationTitles.at(1)).toHaveText('Inclusion of Copyright');
    expect(obligationBodies.at(1)).toHaveText('copyright this');
  });
});
