/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationsTile from '../../../main/frontend/legal/obligation/LicenseObligationsTile';
import { NxFontAwesomeIcon, NxSegmentedButton, NxStatefulAccordion } from '@sonatype/react-shared-components';

describe('LicenseObligationsTile component', function () {
  let getShallowComponent,
    minimalProps,
    setObligationStatus,
    setObligationComment,
    setObligationScope,
    saveObligation,
    setShowObligationModal,
    licenseObligations,
    availableScopes,
    licenseLegalMetadata,
    $state;

  beforeEach(function () {
    setObligationStatus = jasmine.createSpy('setObligationStatus');
    setObligationComment = jasmine.createSpy('setObligationComment');
    setObligationScope = jasmine.createSpy('setObligationScope');
    saveObligation = jasmine.createSpy('saveObligation');
    setShowObligationModal = jasmine.createSpy('setShowObligationModal');

    $state = jasmine.createSpyObj('$state', ['get', 'href']);
    $state.get.and.callFake((stateName) => stateName);
    $state.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    licenseObligations = [
      {
        name: 'obligation 1',
        originalStatus: 'OPEN',
        status: 'OPEN',
      },
      {
        name: 'obligation 2',
        originalStatus: 'IGNORED',
        status: 'IGNORED',
      },
      {
        name: 'obligation 3',
        originalStatus: 'FULFILLED',
        status: 'FULFILLED',
      },
      {
        name: 'obligation 4',
        originalStatus: 'FLAGGED',
        status: 'FLAGGED',
      },
    ];
    availableScopes = {
      values: [
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          label: 'Organization',
        },
      ],
    };
    licenseLegalMetadata = [
      {
        licenseId: 'license1',
        licenseName: 'license1',
        obligations: [
          { name: 'obligation 1', obligationTexts: ['text1', 'text2'] },
          { name: 'obligation 2', obligationTexts: ['text3', 'text4'] },
        ],
      },
      {
        licenseId: 'license2',
        licenseName: 'license2',
        obligations: [
          { name: 'obligation 2', obligationTexts: ['text5', 'text6'] },
          { name: 'obligation 3', obligationTexts: ['text7', 'text8'] },
        ],
      },
      {
        licenseId: 'license3',
        licenseName: 'license3',
        obligations: [{ name: 'obligation 4', obligationTexts: ['text9'] }],
      },
    ];
    const ownerType = 'app';
    const ownerId = 'appId';
    const hash = 'hash';
    const minimalProps = {
      setObligationStatus,
      setObligationComment,
      setObligationScope,
      saveObligation,
      setShowObligationModal,
      licenseObligations,
      availableScopes,
      licenseLegalMetadata,
      ownerType,
      ownerId,
      hash,
      $state,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationsTile, minimalProps);
  });

  it('renders a header with label `License Obligations`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('License Obligations');
  });

  it('renders the correct number of license obligation sections`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find(NxStatefulAccordion).length).toBe(4);
  });

  it('renders the license obligation review status`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    licenseObligationSections.forEach((node) => {
      expect(node.find('h4').at(0).text()).toBe('Review Status');
    });
    const expectedStatuses = ['Unreviewed', 'Not Applicable', 'Fulfilled', 'Flagged'];
    licenseObligationSections.find('.obligation-text').forEach((node, index) => {
      expect(node.text()).toBe(expectedStatuses[index]);
    });
  });

  it('renders the license obligation names and license counts`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    const expectedObligationNamesAndCounts = ['obligation 1', 'obligation 2 (2)', 'obligation 3', 'obligation 4'];
    licenseObligationSections.find('h3').forEach((node, index) => {
      expect(node.text()).toBe(expectedObligationNamesAndCounts[index]);
    });
  });

  it('renders the license obligation license names`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    const expectedObligationLicenseNames = [
      ['license1 — Obligation Text'],
      ['license1 — Obligation Text', 'license2 — Obligation Text'],
      ['license2 — Obligation Text'],
      ['license3 — Obligation Text'],
    ];
    licenseObligationSections.forEach((node1, index1) => {
      node1.find('h4').forEach((node2, index2) => {
        if (index2 !== 0) {
          expect(node2).toHaveText(expectedObligationLicenseNames[index1][index2 - 1]);
        }
      });
    });
  });

  it('renders the license obligation license texts`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    const expectedObligationLicenseTexts = [
      ['text1', 'text2'],
      ['text3', 'text4', 'text5', 'text6'],
      ['text7', 'text8'],
      ['text9'],
    ];
    licenseObligationSections.forEach((node1, index1) => {
      node1.find('blockquote').forEach((node2, index2) => {
        expect(node2).toHaveText(expectedObligationLicenseTexts[index1][index2]);
      });
    });
  });

  it('renders the license obligation license texts view full license link`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    const expectedLicenseIndex = [[0], [0, 1], [1], [2]];
    licenseObligationSections.forEach((node1, index1) => {
      node1.find('div.license-obligation-view-full-license').forEach((node2, index2) => {
        const expectedIndex = expectedLicenseIndex[index1][index2];
        expect(node2).toHaveText('View full license text');
        const licenseLink = node2.find('a');
        expect(licenseLink).toExist();
        expect(licenseLink).toHaveProp(
          'href',
          'legal.componentLicenseDetails-{"ownerType":"app","ownerId":"appId","hash":"hash","licenseIndex":' +
            expectedIndex +
            '}'
        );
        expect($state.href).toHaveBeenCalled();
      });
    });
  });

  it('renders the license obligation selected status options and icons`', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);
    const expectedSelectedObligationStatuses = ['Unreviewed', 'Not Applicable', 'Fulfilled', 'Flagged'];
    licenseObligationSections.find(NxSegmentedButton).forEach((node, index) => {
      const buttonContentPropChildren = node.prop('buttonContent').props['children'];
      if (index === 0) {
        expect(buttonContentPropChildren[0]).toBeUndefined();
      } else {
        expect(buttonContentPropChildren[0]).toBeDefined();
      }
      expect(buttonContentPropChildren[1].props['children']).toEqual(expectedSelectedObligationStatuses[index]);
    });
  });

  it('renders the license obligation unselected status options and icons', function () {
    const wrapper = getShallowComponent();
    const licenseObligationSections = wrapper.find(NxStatefulAccordion);

    let licenseObligation1Section = licenseObligationSections.at(0);
    let licenseObligation1Dropdown = licenseObligation1Section.find(NxSegmentedButton).at(0);
    let licenseObligation1DropdownOptions = licenseObligation1Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation1DropdownOptions.length).toBe(3);
    expect(licenseObligation1DropdownOptions.at(0).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation1DropdownOptions.at(0)).toIncludeText('Mark as Fulfilled');
    expect(licenseObligation1DropdownOptions.at(1).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation1DropdownOptions.at(1)).toIncludeText('Mark as Flagged');
    expect(licenseObligation1DropdownOptions.at(2).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation1DropdownOptions.at(2)).toIncludeText('Mark as Not Applicable');

    let licenseObligation2Section = licenseObligationSections.at(1);
    let licenseObligation2Dropdown = licenseObligation2Section.find(NxSegmentedButton).at(0);
    let licenseObligation2DropdownOptions = licenseObligation2Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation2DropdownOptions.length).toBe(3);
    expect(licenseObligation2DropdownOptions.at(0).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation2DropdownOptions.at(0)).toIncludeText('Mark as Fulfilled');
    expect(licenseObligation2DropdownOptions.at(1).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation2DropdownOptions.at(1)).toIncludeText('Mark as Flagged');
    expect(licenseObligation2DropdownOptions.at(2).find(NxFontAwesomeIcon).length).toBe(0);
    expect(licenseObligation2DropdownOptions.at(2)).toIncludeText('Mark as Unreviewed');

    let licenseObligation3Section = licenseObligationSections.at(2);
    let licenseObligation3Dropdown = licenseObligation3Section.find(NxSegmentedButton).at(0);
    let licenseObligation3DropdownOptions = licenseObligation3Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation3DropdownOptions.length).toBe(3);
    expect(licenseObligation3DropdownOptions.at(0).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation3DropdownOptions.at(0)).toIncludeText('Mark as Flagged');
    expect(licenseObligation3DropdownOptions.at(1).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation3DropdownOptions.at(1)).toIncludeText('Mark as Not Applicable');
    expect(licenseObligation3DropdownOptions.at(2).find(NxFontAwesomeIcon).length).toBe(0);
    expect(licenseObligation3DropdownOptions.at(2)).toIncludeText('Mark as Unreviewed');

    let licenseObligation4Section = licenseObligationSections.at(3);
    let licenseObligation4Dropdown = licenseObligation4Section.find(NxSegmentedButton).at(0);
    let licenseObligation4DropdownOptions = licenseObligation4Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation4DropdownOptions.length).toBe(3);
    expect(licenseObligation4DropdownOptions.at(0).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation4DropdownOptions.at(0)).toIncludeText('Mark as Fulfilled');
    expect(licenseObligation4DropdownOptions.at(1).find(NxFontAwesomeIcon).at(0)).toExist();
    expect(licenseObligation4DropdownOptions.at(1)).toIncludeText('Mark as Not Applicable');
    expect(licenseObligation4DropdownOptions.at(2).find(NxFontAwesomeIcon).length).toBe(0);
    expect(licenseObligation4DropdownOptions.at(2)).toIncludeText('Mark as Unreviewed');
  });

  it('renders None found if there are no obligations', function () {
    const wrapper = enzymeUtils.getShallowComponent(LicenseObligationsTile, {
      ...minimalProps,
      licenseObligations: [],
    })();
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
  });
});
