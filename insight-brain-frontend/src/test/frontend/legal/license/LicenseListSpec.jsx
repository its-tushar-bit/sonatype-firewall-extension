/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import LicenseList from '../../../../main/frontend/legal/license/LicenseList';
import { licenseState } from './licenseCommonState';

describe('LicenseList component', function () {
  let getShallowComponent, $state;

  const minimalProps = licenseState;

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

    getShallowComponent = enzymeUtils.getShallowComponent(LicenseList, minimalProps);
  });

  it('renders the list of licenses by hash', function () {
    const wrapper = getShallowComponent();
    let licenses = wrapper.find('li.nx-list__item');
    expect(licenses.length).toBe(2);

    let license = licenses.at(0);
    expect(license).toHaveText('GPL<NxThreatIndicator />Weak');
    expect(license.find('a')).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"ownerType":"organization","ownerId":"org","hash":"fooHash","licenseIndex":0}'
    );

    license = licenses.at(1);
    expect(licenses.at(1)).toHaveText('GPL-2<NxThreatIndicator />Weak');
    expect(license.find('a')).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"ownerType":"organization","ownerId":"org","hash":"fooHash","licenseIndex":1}'
    );
  });

  it('renders the list of licenses by component identifier', function () {
    const wrapper = getShallowComponent({ ...minimalProps, hash: undefined });
    let licenses = wrapper.find('li.nx-list__item');
    expect(licenses.length).toBe(2);

    let license = licenses.at(0);
    expect(license).toHaveText('GPL<NxThreatIndicator />Weak');
    expect(license.find('a')).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"ownerType":"organization","ownerId":"org","licenseIndex":0}'
    );

    license = licenses.at(1);
    expect(licenses.at(1)).toHaveText('GPL-2<NxThreatIndicator />Weak');
    expect(license.find('a')).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"ownerType":"organization","ownerId":"org","licenseIndex":1}'
    );
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
