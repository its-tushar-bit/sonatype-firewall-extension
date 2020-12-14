/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseObligationsTile from '../../../main/frontend/legal/LicenseObligationsTile';

describe('LicenseObligationsTile component', function() {

  let getShallowComponent;

  const licenseLegalMetadata = {
    0: {
      licenseName: 'license1',
      obligations: [{
        licenseObligation: {
          name: 'obligation 1',
          obligationTexts: [
            'text1',
            'text2'
          ]
        }
      }, {
        licenseObligation: {
          name: 'obligation 2',
          obligationTexts: [
            'text3',
            'text4'
          ]
        }
      }]
    },
    1: {
      licenseName: 'license2',
      obligations: [{
        licenseObligation: {
          name: 'obligation 2',
          obligationTexts: [
            'text5',
            'text6'
          ]
        },
        licenseObligationStatus: 0
      }, {
        licenseObligation: {
          name: 'obligation 3',
          obligationTexts: [
            'text7',
            'text8'
          ]
        }
      }]
    }
  };

  const minimalProps = {
    licenseLegalMetadata
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
    expect(licenseObligationSections.length).toBe(3);

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
  });
});
