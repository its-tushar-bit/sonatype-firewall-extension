/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import LicenseFilesModal from '../../../../../main/frontend/legal/files/licenses/LicenseFilesModal';
import { NxButton, NxStatefulForm, NxTextInput, NxToggle, NxFormSelect } from '@sonatype/react-shared-components';

describe('LicenseFilesModal', function () {
  let getShallowComponent,
    minimalProps,
    cancelLicenseFilesModalSpy,
    setLicenseFileContentSpy,
    setLicenseFileStatusSpy,
    addLicenseFileSpy,
    setLicenseFilesScopeSpy,
    saveLicenseFilesSpy;

  beforeEach(function () {
    cancelLicenseFilesModalSpy = jasmine.createSpy('cancelLicenseFilesModalSpy');
    setLicenseFileContentSpy = jasmine.createSpy('setLicenseFileContentSpy');
    setLicenseFileStatusSpy = jasmine.createSpy('setLicenseFileStatusSpy');
    addLicenseFileSpy = jasmine.createSpy('addLicenseFileSpy');
    setLicenseFilesScopeSpy = jasmine.createSpy('setLicenseFilesScopeSpy');
    saveLicenseFilesSpy = jasmine.createSpy('saveLicenseFilesSpy');
    minimalProps = {
      cancelLicenseFilesModal: cancelLicenseFilesModalSpy,
      setLicenseFileContent: setLicenseFileContentSpy,
      setLicenseFileStatus: setLicenseFileStatusSpy,
      addLicenseFile: addLicenseFileSpy,
      setLicenseFilesScope: setLicenseFilesScopeSpy,
      saveLicenseFiles: saveLicenseFilesSpy,
      scope: 'ROOT_ORGANIZATION_ID',
      originalScope: 'ROOT_ORGANIZATION_ID',
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', label: 'Application' },
          { id: 'orgId', name: 'org', label: 'Organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      licenses: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'enabled',
          isPristine: true,
        },
        {
          id: null,
          originalContentHash: 'originalContentHash2',
          relPath: 'some/path',
          originalContent: '',
          content: '',
          originalStatus: 'disabled',
          status: 'disabled',
          isPristine: false,
        },
      ],
      error: 'error',
      submitMaskState: 'submitMaskState',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(LicenseFilesModal, minimalProps);
  });

  it('renders no license texts found if there are no licenses', function () {
    const wrapper = getShallowComponent({ licenses: [] });
    const noLicenseTextsRow = wrapper.find('tbody tr');
    expect(noLicenseTextsRow).toHaveText('No license texts found');
  });

  it('renders license contents', function () {
    const wrapper = getShallowComponent();
    const licenseContents = wrapper.find(NxTextInput);
    expect(licenseContents.length).toBe(2);
    expect(licenseContents.at(0).prop('value')).toEqual('content1');
    expect(licenseContents.at(0).prop('disabled')).toBeFalsy();
    expect(licenseContents.at(0).prop('isPristine')).toBeTruthy();
    expect(licenseContents.at(1).prop('value')).toEqual('');
    expect(licenseContents.at(1).prop('disabled')).toBeTruthy();
    expect(licenseContents.at(1).prop('isPristine')).toBeFalsy();
  });

  it('sets a license content to the input value when typed', function () {
    const wrapper = getShallowComponent();
    const licenseContents = wrapper.find(NxTextInput);
    licenseContents.at(0).simulate('change', '');
    expect(setLicenseFileContentSpy.calls.mostRecent().args[0]).toEqual({
      index: 0,
      value: '',
    });
    licenseContents.at(1).simulate('change', 'content2');
    expect(setLicenseFileContentSpy.calls.mostRecent().args[0]).toEqual({
      index: 1,
      value: 'content2',
    });
  });

  it('renders license statuses', function () {
    const wrapper = getShallowComponent();
    const licenseStatuses = wrapper.find(NxToggle);
    expect(licenseStatuses.length).toBe(2);
    expect(licenseStatuses.at(0).prop('isChecked')).toBeTruthy();
    expect(licenseStatuses.at(0)).toHaveText('Included');
    expect(licenseStatuses.at(1).prop('isChecked')).toBeFalsy();
    expect(licenseStatuses.at(1)).toHaveText('Excluded');
  });

  it('sets a license status to its opposite value when the toggle is clicked', function () {
    const wrapper = getShallowComponent();
    const licenseStatuses = wrapper.find(NxToggle);
    licenseStatuses.at(0).simulate('change');
    expect(setLicenseFileStatusSpy.calls.mostRecent().args[0]).toEqual({
      index: 0,
      value: 'disabled',
    });
    licenseStatuses.at(1).simulate('change');
    expect(setLicenseFileStatusSpy.calls.mostRecent().args[0]).toEqual({
      index: 1,
      value: 'enabled',
    });
  });

  it('adds a license when the add license button is clicked', function () {
    const wrapper = getShallowComponent();
    const addLicenseButton = wrapper.find(NxButton);
    addLicenseButton.simulate('click');
    expect(addLicenseFileSpy).toHaveBeenCalled();
  });

  it('selects the given licenses scope', function () {
    const wrapper = getShallowComponent();
    const licensesScope = wrapper.find(NxFormSelect);
    expect(licensesScope.length).toBe(1);
    expect(licensesScope.at(0).prop('value')).toEqual('ROOT_ORGANIZATION_ID');
  });

  it('has the licenses scope options', function () {
    const wrapper = getShallowComponent();
    const licensesScopeOptions = wrapper.find('option');
    expect(licensesScopeOptions.length).toBe(3);
    expect(licensesScopeOptions.at(0).prop('value')).toEqual('appId');
    expect(licensesScopeOptions.at(0)).toHaveText('Application - app');
    expect(licensesScopeOptions.at(1).prop('value')).toEqual('orgId');
    expect(licensesScopeOptions.at(1)).toHaveText('Organization - org');
    expect(licensesScopeOptions.at(2).prop('value')).toEqual('ROOT_ORGANIZATION_ID');
    expect(licensesScopeOptions.at(2)).toHaveText('Organization - Root Organization');
  });

  it('sets the licenses scope to the selected value when changed', function () {
    const wrapper = getShallowComponent();
    const licensesScope = wrapper.find(NxFormSelect);
    licensesScope.simulate('change', { currentTarget: { value: 'appId' } });
    expect(setLicenseFilesScopeSpy).toHaveBeenCalledWith('appId');
  });

  it('has a validation error if a custom license has no content', function () {
    const wrapper = getShallowComponent({
      licenses: [
        ...minimalProps.licenses,
        {
          id: null,
          originalContentHash: null,
          originalContent: '',
          content: '',
          status: 'disabled',
          isPristine: true,
        },
      ],
    });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBe('A custom license must have text.');
  });

  it('has a validation error if there has been no changes', function () {
    const wrapper = getShallowComponent({
      licenses: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'enabled',
          isPristine: true,
        },
      ],
    });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBe('Must add a new license or change the content or status of a license.');
  });

  it('has no validation error if the scope has changed', function () {
    const wrapper = getShallowComponent({ scope: 'appId' });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if a custom license was added with content', function () {
    const wrapper = getShallowComponent({
      licenses: [
        {
          id: null,
          originalContentHash: null,
          originalContent: '',
          content: 'content',
          originalStatus: 'enabled',
          status: 'enabled',
        },
      ],
    });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if the content has changed', function () {
    const wrapper = getShallowComponent({
      licenses: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'updatedContent1',
          originalStatus: 'enabled',
          status: 'enabled',
        },
      ],
    });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });

  it('has no validation error if the status has changed', function () {
    const wrapper = getShallowComponent({
      licenses: [
        {
          id: null,
          originalContentHash: 'originalContentHash1',
          relPath: 'some/path',
          originalContent: 'content1',
          content: 'content1',
          originalStatus: 'enabled',
          status: 'disabled',
        },
      ],
    });
    const form = wrapper.find(NxStatefulForm);
    expect(form.prop('validationErrors')).toBeUndefined();
  });
});
