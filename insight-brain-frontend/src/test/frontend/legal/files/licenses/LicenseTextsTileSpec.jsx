/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import LicenseFilesTile from '../../../../../main/frontend/legal/files/licenses/LicenseFilesTile';
import { NxButton, NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';

describe('LicenseTextsTile', function () {
  let getShallowComponent, minimalProps, setShowLicenseFilesModalSpy;

  beforeEach(function () {
    setShowLicenseFilesModalSpy = jasmine.createSpy('setShowLicenseFilesModalSpy');
    minimalProps = {
      setShowLicenseFilesModal: setShowLicenseFilesModalSpy,
      licenseFiles: [
        {
          originalStatus: 'enabled',
          originalContent: 'license content 1',
          content: 'license content 1',
          relPath: 'path1/license.txt',
        },
        {
          originalStatus: 'enabled',
          originalContent: 'license content 2',
          content: 'license content 2',
        },
        {
          originalStatus: 'disabled',
          originalContent: 'license content 3',
          content: 'license content 3',
        },
      ],
      showLicenseFilesModal: false,
      $state: {
        get: () => '',
        href: () => '',
      },
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseFilesTile, minimalProps);
  });

  it('renders a header with label `License Files`', function () {
    const wrapper = getShallowComponent();
    expect(wrapper.find('h2.nx-h2')).toHaveText('License Files');
  });

  it('renders the given licenses', function () {
    const wrapper = getShallowComponent();
    const licenses = wrapper.find('.legal-file');
    expect(licenses.length).toBe(2);
    expect(licenses.at(0).find('.legal-file-path')).toHaveText('path1/license.txt');
    expect(licenses.at(0).find('blockquote')).toHaveText('license content 1');
    expect(licenses.at(1).find('.legal-file-path')).toHaveText('');
    expect(licenses.at(1).find('blockquote')).toHaveText('license content 2');
  });

  it('renders none found if there are no licenses', function () {
    const wrapper = getShallowComponent({ licenseFiles: [] });
    const content = wrapper.find('.nx-tile-content');
    expect(content).toHaveText('None found');
  });

  it('renders an add button if there are no licenses', function () {
    const wrapper = getShallowComponent({ licenseFiles: [] });
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPlus);
    expect(button.find('span').at(0)).toHaveText('Add');
  });

  it('renders an edit button if there is at least one license', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    expect(button.find(NxFontAwesomeIcon).at(0).prop('icon')).toEqual(faPen);
    expect(button.find('span').at(0)).toHaveText('Edit');
  });

  it('shows the licenses modal when clicking the add/edit button', function () {
    const wrapper = getShallowComponent();
    const button = wrapper.find(NxButton);
    button.simulate('click');
    expect(setShowLicenseFilesModalSpy).toHaveBeenCalledWith(true);
  });
});
