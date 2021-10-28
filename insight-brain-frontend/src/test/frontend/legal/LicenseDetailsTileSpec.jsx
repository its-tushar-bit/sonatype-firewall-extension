/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseDetailsTile from '../../../main/frontend/legal/LicenseDetailsTile';
import LicensesModalContainer from '../../../main/frontend/legal/license/LicensesModalContainer';

describe('LicenseDetailsTile component', function () {
  let getShallowComponent, getMountedComponent, $state, minimalProps, setShowLicensesModalMock;

  beforeEach(function () {
    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });
    const licenseLegalMetadata = [
      {
        licenseId: 'License-1.0',
        licenseName: 'License-1.0',
        singleLicenseIds: [],
        isMulti: false,
      },
      {
        licenseId: 'License-2.0',
        licenseName: 'License-2.0',
        singleLicenseIds: [],
        isMulti: false,
      },
      {
        licenseId: 'License-1.0-License-2.0',
        licenseName: 'License-1.0 or License-2.0',
        singleLicenseIds: ['License-1.0', 'License-2.0'],
        isMulti: true,
      },
    ];

    const component = {
      licenseLegalData: {
        effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
        observedLicenses: ['License-2.0', 'License-1.0-License-2.0'],
        declaredLicenses: ['License-1.0-License-2.0'],
        effectiveLicenseStatus: 'Selected',
      },
    };

    setShowLicensesModalMock = jasmine.createSpy();

    minimalProps = {
      component,
      licenseLegalMetadata,
      setShowLicensesModal: setShowLicensesModalMock,
      $state,
      hash: 'hash-test',
    };

    getShallowComponent = enzymeUtils.getShallowComponent(LicenseDetailsTile, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(LicenseDetailsTile, minimalProps);
  });

  it('renders a header with label `License Details`', function () {
    const wrapper = getMountedComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('Licenses');
  });

  it('renders an edit button', function () {
    const wrapper = getShallowComponent();
    const editButton = wrapper.find('#edit-licenses');
    editButton.simulate('click');
    expect(setShowLicensesModalMock).toHaveBeenCalledWith(true);
  });

  it('does not show the modal if showLicensesModal is false', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(LicensesModalContainer)).not.toExist();
  });

  it('does not show the modal if showLicensesModal is true', function () {
    const wrapper = getShallowComponent({ showLicensesModal: true });
    expect(wrapper.find(LicensesModalContainer)).toExist();
  });

  it('renders the given licenses', function () {
    const wrapper = getMountedComponent();
    let licenseLinks = wrapper.find('.license-details-tile__effective-licenses .nx-text-link span');
    expect(licenseLinks.length).toBe(4);
    expect(licenseLinks.at(0)).toHaveText('License-1.0');
    expect(licenseLinks.at(1)).toHaveText('License-1.0');
    expect(licenseLinks.at(2)).toHaveText('License-2.0');
    expect(licenseLinks.at(3)).toHaveText('License-2.0');

    licenseLinks = wrapper.find('.license-details-tile__observed-licenses .nx-text-link span');
    expect(licenseLinks.length).toBe(3);
    expect(licenseLinks.at(0)).toHaveText('License-1.0');
    expect(licenseLinks.at(1)).toHaveText('License-2.0');
    expect(licenseLinks.at(2)).toHaveText('License-2.0');

    licenseLinks = wrapper.find('.license-details-tile__declared-licenses .nx-text-link span');
    expect(licenseLinks.length).toBe(2);
    expect(licenseLinks.at(0)).toHaveText('License-1.0');
    expect(licenseLinks.at(1)).toHaveText('License-2.0');
  });

  it('renders the links to the licenses details pages by hash', function () {
    const wrapper = getMountedComponent();
    let licenseLinks = wrapper.find('.license-details-tile__effective-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(4);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":0}'
    );
    expect(licenseLinks.at(2)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":1}'
    );
    expect(licenseLinks.at(3)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":1}'
    );

    licenseLinks = wrapper.find('.license-details-tile__observed-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(3);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":1}'
    );
    expect(licenseLinks.at(2)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":1}'
    );

    licenseLinks = wrapper.find('.license-details-tile__declared-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(2);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetails-{"hash":"hash-test","licenseIndex":1}'
    );
  });

  it('renders the links to the licenses details pages by component identifier', function () {
    const wrapper = getMountedComponent({ ...minimalProps, hash: undefined, componentIdentifier: 'ci' });
    let licenseLinks = wrapper.find('.license-details-tile__effective-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(4);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":0}'
    );
    expect(licenseLinks.at(2)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":1}'
    );
    expect(licenseLinks.at(3)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":1}'
    );

    licenseLinks = wrapper.find('.license-details-tile__observed-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(3);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":1}'
    );
    expect(licenseLinks.at(2)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":1}'
    );

    licenseLinks = wrapper.find('.license-details-tile__declared-licenses a.nx-text-link');
    expect(licenseLinks.length).toBe(2);
    expect(licenseLinks.at(0)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":0}'
    );
    expect(licenseLinks.at(1)).toHaveProp(
      'href',
      'legal.componentLicenseDetailsByComponentIdentifier-{"componentIdentifier":"ci","licenseIndex":1}'
    );
  });

  it('renders None found if there are no licenses', function () {
    const newMinimalProps = {
      ...minimalProps,
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          effectiveLicenses: [],
          observedLicenses: [],
          declaredLicenses: [],
          effectiveLicenseStatus: null,
        },
      },
      licenseLegalMetadata: [],
    };

    const wrapper = enzymeUtils.getMountedComponent(LicenseDetailsTile, newMinimalProps)();
    let content = wrapper.find('.nx-tile-content .license-details-tile__effective-licenses span');
    expect(content).toHaveText('None found');

    content = wrapper.find('.nx-tile-content .license-details-tile__observed-licenses span');
    expect(content).toHaveText('None found');

    content = wrapper.find('.nx-tile-content .license-details-tile__declared-licenses span');
    expect(content).toHaveText('None found');
  });

  it('renders the selected tag when effectiveLicenseStatus is Selected', function () {
    const wrapper = getMountedComponent();
    let SelectedNxTag = wrapper.find('.license-details-tile__effective-licenses .nx-tag.nx-selectable-color--indigo');
    expect(SelectedNxTag).toHaveText('Selected');
  });

  it('renders the selected tag when effectiveLicenseStatus is Overridden', function () {
    const newMinimalProps = {
      ...minimalProps,
      component: {
        ...minimalProps.component,
        licenseLegalData: {
          ...minimalProps.component.licenseLegalData,
          effectiveLicenseStatus: 'Overridden',
        },
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(LicenseDetailsTile, newMinimalProps)();
    let SelectedNxTag = wrapper.find('.license-details-tile__effective-licenses .nx-tag.nx-selectable-color--purple');
    expect(SelectedNxTag).toHaveText('Overridden');
  });
});
