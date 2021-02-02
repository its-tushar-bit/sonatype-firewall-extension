/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationsTile from '../../../main/frontend/legal/LicenseObligationsTile';

describe('LicenseObligationsTile component', function() {

  let getShallowComponent;
  const licenseObligations = [{
    name: 'obligation 1',
    licenses: [{
      name: 'license1',
      texts: ['text1', 'text2']
    }],
    status: 'OPEN'
  }, {
    name: 'obligation 2',
    licenses: [{
      name: 'license1',
      texts: ['text3', 'text4']
    }, {
      name: 'license2',
      texts: ['text5', 'text6']
    }],
    status: 'IGNORED'
  }, {
    name: 'obligation 3',
    licenses: [{
      name: 'license2',
      texts: ['text7', 'text8']
    }],
    status: 'FULFILLED'
  }, {
    name: 'obligation 4',
    licenses: [{
      name: 'license3',
      texts: ['text9']
    }],
    status: 'FLAGGED'
  }];
  const minimalProps = {
    licenseObligations
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseObligationsTile, minimalProps);
  });

  it('renders a header with label `License Obligations`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('License Obligations');
  });

  it('renders the given license obligations', function() {
    const wrapper = getShallowComponent();
    let licenseObligationSections = wrapper.find('NxAccordion');
    expect(licenseObligationSections.length).toBe(4);

    let licenseObligation1Section = licenseObligationSections.at(0);
    let licenseObligation1Name = licenseObligation1Section.find('h3');
    expect(licenseObligation1Name.length).toBe(1);
    expect(licenseObligation1Name.at(0)).toHaveText('obligation 1');
    let licenseObligation1LicenseNames = licenseObligation1Section.find('h4');
    expect(licenseObligation1LicenseNames.length).toBe(1);
    expect(licenseObligation1LicenseNames.at(0)).toHaveText('license1');
    let licenseObligation1LicenseTexts = licenseObligation1Section.find('.obligation-text');
    expect(licenseObligation1LicenseTexts.length).toBe(2);
    expect(licenseObligation1LicenseTexts.at(0)).toHaveText('text1');
    expect(licenseObligation1LicenseTexts.at(1)).toHaveText('text2');
    let licenseObligation1Dropdown = licenseObligation1Section.find('NxDropdown').at(0);
    let licenseObligation1DropdownIcon = licenseObligation1Dropdown.prop('label').props['children'][0];
    expect(licenseObligation1DropdownIcon).toBeUndefined();
    expect(licenseObligation1Dropdown.prop('label').props['children'][1]).toBe('Unreviewed');
    let licenseObligation1DropdownOptions = licenseObligation1Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation1DropdownOptions.length).toBe(3);
    let licenseObligation1DropdownOptionTexts = [
      licenseObligation1DropdownOptions.at(0).text(),
      licenseObligation1DropdownOptions.at(1).text(),
      licenseObligation1DropdownOptions.at(2).text()
    ];
    expect(licenseObligation1DropdownOptionTexts).toContain('Mark as Not Applicable');
    expect(licenseObligation1DropdownOptionTexts).toContain('Mark as Flagged');
    expect(licenseObligation1DropdownOptionTexts).toContain('Mark as Fulfilled');

    let licenseObligation2Section = licenseObligationSections.at(1);
    let licenseObligation2Name = licenseObligation2Section.find('h3');
    expect(licenseObligation2Name.length).toBe(1);
    expect(licenseObligation2Name.at(0)).toHaveText('obligation 2 (2)');
    let licenseObligation2LicenseNames = licenseObligation2Section.find('h4');
    expect(licenseObligation2LicenseNames.length).toBe(2);
    expect(licenseObligation2LicenseNames.at(0)).toHaveText('license1');
    expect(licenseObligation2LicenseNames.at(1)).toHaveText('license2');
    let licenseObligation2LicenseTexts = licenseObligation2Section.find('.obligation-text');
    expect(licenseObligation2LicenseTexts.length).toBe(4);
    expect(licenseObligation2LicenseTexts.at(0)).toHaveText('text3');
    expect(licenseObligation2LicenseTexts.at(1)).toHaveText('text4');
    expect(licenseObligation2LicenseTexts.at(2)).toHaveText('text5');
    expect(licenseObligation2LicenseTexts.at(3)).toHaveText('text6');
    let licenseObligation2Dropdown = licenseObligation2Section.find('NxDropdown').at(0);
    let licenseObligation2DropdownIcon = licenseObligation2Dropdown.prop('label').props['children'][0];
    expect(licenseObligation2DropdownIcon).not.toBeUndefined();
    expect(licenseObligation2Dropdown.prop('label').props['children'][1]).toBe('Not Applicable');
    let licenseObligation2DropdownOptions = licenseObligation2Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation2DropdownOptions.length).toBe(3);
    let licenseObligation2DropdownOptionTexts = [
      licenseObligation2DropdownOptions.at(0).text(),
      licenseObligation2DropdownOptions.at(1).text(),
      licenseObligation2DropdownOptions.at(2).text()
    ];
    expect(licenseObligation2DropdownOptionTexts).toContain('Mark as Unreviewed');
    expect(licenseObligation2DropdownOptionTexts).toContain('Mark as Flagged');
    expect(licenseObligation2DropdownOptionTexts).toContain('Mark as Fulfilled');

    let licenseObligation3Section = licenseObligationSections.at(2);
    let licenseObligation3Name = licenseObligation3Section.find('h3');
    expect(licenseObligation3Name.length).toBe(1);
    expect(licenseObligation3Name.at(0)).toHaveText('obligation 3');
    let licenseObligation3LicenseNames = licenseObligation3Section.find('h4');
    expect(licenseObligation3LicenseNames.length).toBe(1);
    expect(licenseObligation3LicenseNames.at(0)).toHaveText('license2');
    let licenseObligation3LicenseTexts = licenseObligation3Section.find('.obligation-text');
    expect(licenseObligation3LicenseTexts.length).toBe(2);
    expect(licenseObligation3LicenseTexts.at(0)).toHaveText('text7');
    expect(licenseObligation3LicenseTexts.at(1)).toHaveText('text8');
    let licenseObligation3Dropdown = licenseObligation3Section.find('NxDropdown').at(0);
    let licenseObligation3DropdownIcon = licenseObligation3Dropdown.prop('label').props['children'][0];
    expect(licenseObligation3DropdownIcon).not.toBeUndefined();
    expect(licenseObligation3Dropdown.prop('label').props['children'][1]).toBe('Fulfilled');
    let licenseObligation3DropdownOptions = licenseObligation3Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation3DropdownOptions.length).toBe(3);
    let licenseObligation3DropdownOptionTexts = [
      licenseObligation3DropdownOptions.at(0).text(),
      licenseObligation3DropdownOptions.at(1).text(),
      licenseObligation3DropdownOptions.at(2).text()
    ];
    expect(licenseObligation3DropdownOptionTexts).toContain('Mark as Unreviewed');
    expect(licenseObligation3DropdownOptionTexts).toContain('Mark as Flagged');
    expect(licenseObligation3DropdownOptionTexts).toContain('Mark as Not Applicable');

    let licenseObligation4Section = licenseObligationSections.at(3);
    let licenseObligation4Name = licenseObligation4Section.find('h3');
    expect(licenseObligation4Name.length).toBe(1);
    expect(licenseObligation4Name.at(0)).toHaveText('obligation 4');
    let licenseObligation4LicenseNames = licenseObligation4Section.find('h4');
    expect(licenseObligation4LicenseNames.length).toBe(1);
    expect(licenseObligation4LicenseNames.at(0)).toHaveText('license3');
    let licenseObligation4LicenseTexts = licenseObligation4Section.find('.obligation-text');
    expect(licenseObligation4LicenseTexts.length).toBe(1);
    expect(licenseObligation4LicenseTexts.at(0)).toHaveText('text9');
    let licenseObligation4Dropdown = licenseObligation4Section.find('NxDropdown').at(0);
    let licenseObligation4DropdownIcon = licenseObligation4Dropdown.prop('label').props['children'][0];
    expect(licenseObligation4DropdownIcon).not.toBeUndefined();
    expect(licenseObligation4Dropdown.prop('label').props['children'][1]).toBe('Flagged');
    let licenseObligation4DropdownOptions = licenseObligation4Dropdown.find('.nx-dropdown-button');
    expect(licenseObligation4DropdownOptions.length).toBe(3);
    let licenseObligation4DropdownOptionTexts = [
      licenseObligation4DropdownOptions.at(0).text(),
      licenseObligation4DropdownOptions.at(1).text(),
      licenseObligation4DropdownOptions.at(2).text()
    ];
    expect(licenseObligation4DropdownOptionTexts).toContain('Mark as Not Applicable');
    expect(licenseObligation4DropdownOptionTexts).toContain('Mark as Unreviewed');
    expect(licenseObligation4DropdownOptionTexts).toContain('Mark as Fulfilled');
  });
});
