/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../enzymeUtils';
import LicenseTextsTile from '../../../main/frontend/legal/LicenseTextsTile';

describe('LicenseTextsTile component', function() {

  let getShallowComponent;

  const minimalProps = {
    component: {
      licenseLegalData: {
        licenseFiles: [
          { content: 'licenseText content 1', relPath: 'path1/licenseText.txt' },
          { content: 'licenseText content 2', relPath: 'path2/licenseText.txt' }
        ]
      }
    }
  };

  beforeEach(function() {
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseTextsTile, minimalProps);
  });

  it('renders a header with label `License Texts`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('License Texts');
  });

  it('renders the given license texts', function() {
    const wrapper = getShallowComponent();
    let licenseTextDivs = wrapper.find('div.legal-file');
    expect(licenseTextDivs.length).toBe(2);
    expect(licenseTextDivs.at(0).find('span.legal-file-path')).toHaveText('path1/licenseText.txt');
    expect(licenseTextDivs.at(0).find('blockquote')).toHaveText('licenseText content 1');
    expect(licenseTextDivs.at(1).find('span.legal-file-path')).toHaveText('path2/licenseText.txt');
    expect(licenseTextDivs.at(1).find('blockquote')).toHaveText('licenseText content 2');
  });
});
