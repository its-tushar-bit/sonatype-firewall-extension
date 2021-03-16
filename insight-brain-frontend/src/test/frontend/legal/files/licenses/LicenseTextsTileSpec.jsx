/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import LicenseTextsTile from '../../../../../main/frontend/legal/files/licenses/LicenseTextsTile';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';

describe('LicenseTextsTile', function() {

  let getShallowComponent,
      minimalProps,
      setShowLicensesModalSpy;

  beforeEach(function() {
    setShowLicensesModalSpy = jasmine.createSpy('setShowLicensesModalSpy');
    minimalProps = {
      setShowLicensesModal: setShowLicensesModalSpy,
      licenseFiles: [
        { originalContent: 'license content 1', content: 'license content 1', relPath: 'path1/license.txt' },
        { originalContent: 'license content 2', content: 'license content 2' }
      ],
      showLicensesModal: false
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseTextsTile, minimalProps);
  });

  it('renders a header with label `License Texts`', function() {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('License Texts');
  });

  it('renders the given licenses', function() {
    const wrapper = getShallowComponent();
    const licenses = wrapper.find('.legal-file');
    expect(licenses.length).toBe(2);
    expect(licenses.at(0).find('h3')).toHaveText('path1/license.txt');
    expect(licenses.at(0).find('blockquote')).toHaveText('license content 1');
    expect(licenses.at(1).find('h3')).not.toExist();
    expect(licenses.at(1).find('blockquote')).toHaveText('license content 2');
  });

  it('renders none found if there are no licenses', function() {
    const wrapper = getShallowComponent({ licenseFiles: [] });
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
  });

  it('renders an add button if there are no licenses', function() {
    const wrapper = getShallowComponent({ licenseFiles: [] });
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPlus);
    expect(button.find('span').at(0)).toHaveText('Add');
  });

  it('renders an edit button if there is at least one license', function() {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPen);
    expect(button.find('span').at(0)).toHaveText('Edit');
  });

  it('shows the licenses modal when clicking the add/edit button', function() {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    button.simulate('click');
    expect(setShowLicensesModalSpy).toHaveBeenCalledWith(true);
  });
});
