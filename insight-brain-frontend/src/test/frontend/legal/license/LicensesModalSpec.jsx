/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import LicensesModal from '../../../../main/frontend/legal/license/LicensesModal';
import * as enzymeUtils from '../../enzymeUtils';
import { NxForm, NxFormGroup, NxModal, NxThreatIndicator } from '@sonatype/react-shared-components';

describe('LicensesModal', function () {
  let getShallowComponent,
    getMountedComponent,
    minimalProps,
    setShowLicensesModalSpy,
    saveLicensesSpy,
    loadLicenseModalInformationSpy,
    mountPoint;

  beforeEach(function () {
    setShowLicensesModalSpy = jasmine.createSpy('setShowLicensesModal');
    saveLicensesSpy = jasmine.createSpy('saveLicenses');
    loadLicenseModalInformationSpy = jasmine.createSpy('loadLicenseModalInformation');

    const licenseLegalMetadata = [
      {
        licenseId: 'License-1.0',
        licenseName: 'License-1.0',
        singleLicenseIds: [],
        isMulti: false,
        threatGroup: { name: 'Liberal', threatLevel: 0 },
      },
      {
        licenseId: 'License-2.0',
        licenseName: 'License-2.0',
        singleLicenseIds: [],
        isMulti: false,
        threatGroup: { name: 'Liberal', threatLevel: 3 },
      },
      {
        licenseId: 'License-1.0-License-2.0',
        licenseName: 'License-1.0 or License-2.0',
        singleLicenseIds: ['License-1.0', 'License-2.0'],
        isMulti: true,
      },
      {
        licenseId: 'License-2.0 or License-3.0',
        licenseName: 'License-2.0 or License-3.0',
        singleLicenseIds: ['License-2.0', 'License-3.0'],
        isMulti: true,
      },
    ];

    const component = {
      licenseLegalData: {
        effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
        observedLicenses: ['License-2.0', 'License-1.0-License-2.0'],
        declaredLicenses: ['License-1.0-License-2.0'],
        effectiveLicenseStatus: 'Selected',
        hierarchy: [
          {
            licenseOverride: 'anOverride',
            ownerId: 'appPublicId',
          },
        ],
      },
      componentIdentifier: {
        coordinates: { name: 'jquery-form', qualifier: '', version: '3.50.0' },
        format: 'a-name',
        displayName: 'jquery-form 3.50.0',
        hash: '9c06887e03feb6c996ab',
      },
    };
    minimalProps = {
      setShowLicensesModal: setShowLicensesModalSpy,
      saveLicenses: saveLicensesSpy,
      loadLicenseModalInformation: loadLicenseModalInformationSpy,
      availableScopes: {
        values: [
          { id: 'appId', name: 'app', publicId: 'appPublicId', label: 'Application', type: 'application' },
          { id: 'orgId', name: 'org', publicId: 'orgPublicId', label: 'Organization' },
          {
            id: 'ROOT_ORGANIZATION_ID',
            publicId: 'ROOT_ORGANIZATION_ID',
            name: 'Root Organization',
            label: 'Organization',
          },
        ],
      },
      component,
      licenseLegalMetadata,
      allLicenses: ['license1', 'license2', 'license3'],
      error: 'error',
      submitMaskState: 'submitMaskState',
      ownerId: 'org',
      hash: 'hash123',
    };

    mountPoint = document.createElement('div');
    document.body.appendChild(mountPoint);

    getShallowComponent = enzymeUtils.getShallowComponent(LicensesModal, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(LicensesModal, minimalProps, { attachTo: mountPoint });
  });

  afterEach(() => {
    document.body.removeChild(mountPoint);
  });

  it('renders an NxModal', () => {
    const wrapper = getShallowComponent();
    const modal = wrapper.find(NxModal);
    expect(modal).toExist();
  });

  describe('license group sidebar groups', () => {
    it('renders declared licenses', () => {
      const wrapper = getShallowComponent();
      const modal = wrapper.find(NxModal);
      const licenseGroup = modal.find('#licenses-modal-declared-licenses');
      expect(licenseGroup).toExist();
      expect(licenseGroup.find('h3.nx-h3')).toHaveText('Declared Licenses');
      const license1 = licenseGroup.find('.license-modal--license').at(0);
      expect(license1.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 0);
      expect(license1.find('span')).toHaveText('License-1.0');
      const license2 = licenseGroup.find('.license-modal--license').at(1);
      expect(license2.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 3);
      expect(license2.find('span')).toHaveText('License-2.0');
    });
    it('renders effective licenses', () => {
      const wrapper = getShallowComponent();
      const modal = wrapper.find(NxModal);
      const licenseGroup = modal.find('#licenses-modal-effective-licenses');
      expect(licenseGroup).toExist();
      expect(licenseGroup.find('h3.nx-h3')).toHaveText('Effective Licenses');
      const license1 = licenseGroup.find('.license-modal--license').at(0);
      expect(license1.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 0);
      expect(license1.find('span')).toHaveText('License-1.0');
      const license2 = licenseGroup.find('.license-modal--license').at(1);
      expect(license2.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 3);
      expect(license2.find('span')).toHaveText('License-2.0');
    });
    it('renders observed licenses', () => {
      const wrapper = getShallowComponent();
      const modal = wrapper.find(NxModal);
      const licenseGroup = modal.find('#licenses-modal-observed-licenses');
      expect(licenseGroup).toExist();
      expect(licenseGroup.find('h3.nx-h3')).toHaveText('Observed Licenses');
      const license1 = licenseGroup.find('.license-modal--license').at(0);
      expect(license1.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 0);
      expect(license1.find('span')).toHaveText('License-1.0');
      const license2 = licenseGroup.find('.license-modal--license').at(1);
      expect(license2.find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 3);
      expect(license2.find('span')).toHaveText('License-2.0');
    });
  });

  it('calls loadLicenseModalInformation with the correct information', () => {
    getMountedComponent();
    const expectedIdentifier =
      '{"coordinates":{"name":"jquery-form","qualifier":"","version":"3.50.0"},"format":"a-name","displayName":"jquery-form 3.50.0","hash":"9c06887e03feb6c996ab"}';
    expect(loadLicenseModalInformationSpy).toHaveBeenCalledWith({
      ownerType: 'application',
      ownerId: 'appPublicId',
      componentIdentifier: expectedIdentifier,
    });
  });

  describe('form group', () => {
    it('renders the form correctly for acknowledged state', () => {
      const wrapper = getMountedComponent({
        component: {
          licenseLegalData: {
            effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
            observedLicenses: ['License-2.0', 'License-1.0-License-2.0'],
            declaredLicenses: ['License-1.0-License-2.0'],
            effectiveLicenseStatus: 'Acknowledged',
            hierarchy: [
              {
                licenseOverride: 'anOverride',
                ownerId: 'appPublicId',
              },
              {
                licenseOverride: 'anOverride2',
                ownerId: 'orgPublicId',
              },
            ],
          },
        },
      });
      const formGroups = wrapper.find(NxFormGroup);
      const statusGroup = formGroups.at(0);
      expect(statusGroup.find('select')).toHaveProp('value', 'Acknowledged');
      expect(statusGroup.find('option').length).toEqual(6);
      const licenseDiv = wrapper.find('.nx-scrollable--edit-license-modal-licenses');
      expect(licenseDiv).not.toExist();
      const scopeGroup = formGroups.at(2);
      let scopeSelect = scopeGroup.find('#edit-licenses-scope-selection');
      expect(scopeSelect).toHaveProp('value', 'appId');
      expect(scopeSelect.find('option').length).toEqual(3);
    });
    it('renders the form correctly for selected state', () => {
      const wrapper = getMountedComponent({
        ownerId: 'orgPublicId',
        component: {
          licenseLegalData: {
            effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0', 'License-2.0 or License-3.0'],
            observedLicenses: ['License-2.0', 'License-1.0-License-2.0', 'License-2.0 or License-3.0'],
            declaredLicenses: ['License-1.0-License-2.0'],
            effectiveLicenseStatus: 'Selected',
            hierarchy: [
              {
                licenseOverride: null,
                ownerId: 'appPublicId',
              },
              {
                licenseOverride: null,
                ownerId: 'orgPublicId',
              },
              {
                licenseOverride: {
                  comment: 'anOverride3',
                  status: 'CONFIRMED',
                },
                ownerId: 'ROOT_ORGANIZATION_ID',
              },
            ],
          },
        },
      });
      const formGroups = wrapper.find(NxFormGroup);
      const statusGroup = formGroups.at(0);
      expect(statusGroup.find('select')).toHaveProp('value', 'Selected');
      expect(statusGroup.find('option').length).toEqual(6);
      expect(statusGroup.find('option').at(0).prop('value')).toEqual('Open');
      expect(statusGroup.find('option').at(1).prop('value')).toEqual('Acknowledged');
      expect(statusGroup.find('option').at(2).prop('value')).toEqual('Overridden');
      expect(statusGroup.find('option').at(3).prop('value')).toEqual('Selected');
      expect(statusGroup.find('option').at(4).prop('value')).toEqual('Confirmed');
      expect(statusGroup.find('option').at(5).prop('value')).toEqual('Inherit');
      expect(statusGroup.find('option').at(5).childAt(0).text()).toEqual('Inherit Status (Confirmed)');
      const licenseDiv = wrapper.find('.nx-scrollable--edit-license-modal-licenses');
      expect(licenseDiv).toExist();
      expect(licenseDiv.children().length).toEqual(3);
      expect(licenseDiv.childAt(0).prop('id')).toEqual('License-1.0');
      expect(licenseDiv.childAt(1).prop('id')).toEqual('License-2.0');
      expect(licenseDiv.childAt(2).prop('id')).toEqual('License-3.0');
      const scopeGroup = formGroups.at(2);
      let scopeSelect = scopeGroup.find('#edit-licenses-scope-selection');
      expect(scopeSelect).toHaveProp('value', 'ROOT_ORGANIZATION_ID');
      expect(scopeSelect.find('option').length).toEqual(3);
    });
    it('renders the form correctly for overridden state', () => {
      const wrapper = getMountedComponent({
        component: {
          licenseLegalData: {
            effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
            observedLicenses: ['License-2.0', 'License-1.0-License-2.0'],
            declaredLicenses: ['License-1.0-License-2.0'],
            effectiveLicenseStatus: 'Overridden',
            hierarchy: [
              {
                licenseOverride: null,
                ownerId: 'appPublicId',
              },
              {
                licenseOverride: 'anOverride2',
                ownerId: 'orgPublicId',
              },
            ],
          },
        },
      });
      const formGroups = wrapper.find(NxFormGroup);
      const statusGroup = formGroups.at(0);
      expect(statusGroup.find('select')).toHaveProp('value', 'Overridden');
      expect(statusGroup.find('option').length).toEqual(6);
      const licenseDiv = wrapper.find('.nx-scrollable--edit-license-modal-licenses');
      expect(licenseDiv).toExist();
      const scopeGroup = formGroups.at(2);
      let scopeSelect = scopeGroup.find('#edit-licenses-scope-selection');
      expect(scopeSelect).toHaveProp('value', 'orgId');
      expect(scopeSelect.find('option').length).toEqual(3);
    });

    it('sets scope value to the value of the first scope if there are no overrides', () => {
      const wrapper = getMountedComponent({
        component: {
          licenseLegalData: {
            effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0'],
            observedLicenses: ['License-2.0', 'License-1.0-License-2.0'],
            declaredLicenses: ['License-1.0-License-2.0'],
            effectiveLicenseStatus: 'Acknowledged',
            hierarchy: [
              {
                licenseOverride: null,
                ownerId: 'appPublicId',
              },
              {
                licenseOverride: null,
                ownerId: 'orgPublicId',
              },
            ],
          },
        },
      });
      const formGroups = wrapper.find(NxFormGroup);
      const statusGroup = formGroups.at(0);
      expect(statusGroup.find('select')).toHaveProp('value', 'Acknowledged');
      expect(statusGroup.find('option').length).toEqual(6);
      const licenseDiv = wrapper.find('.nx-scrollable--edit-license-modal-licenses');
      expect(licenseDiv).not.toExist();
      const scopeGroup = formGroups.at(2);
      let scopeSelect = scopeGroup.find('#edit-licenses-scope-selection');
      expect(scopeSelect).toHaveProp('value', 'appId');
      expect(scopeSelect.find('option').length).toEqual(3);
    });
  });

  it('calls saveLicensesSpy with the correct information when you save', () => {
    const componentIdentifier = Object({
      coordinates: Object({ name: 'jquery-form', qualifier: '', version: '3.50.0' }),
      format: 'a-name',
      displayName: 'jquery-form 3.50.0',
      hash: '9c06887e03feb6c996ab',
    });
    const expectedPostBody = {
      ownerType: 'application',
      ownerId: 'appPublicId',
      postBody: Object({
        componentIdentifier,
        comment: '',
        status: 'ACKNOWLEDGED',
        ownerId: 'appPublicId',
        licenseIds: [],
      }),
      hash: 'hash123',
      componentIdentifier: JSON.stringify(componentIdentifier),
    };
    const wrapper = getMountedComponent();
    const form = wrapper.find(NxForm);
    const formGroups = wrapper.find(NxFormGroup);
    const statusGroup = formGroups.at(0);
    const statusSelect = statusGroup.find('select');
    statusSelect.simulate('change', { target: { value: 'Acknowledged' } });
    form.simulate('submit');
    expect(saveLicensesSpy).toHaveBeenCalledWith(expectedPostBody);
  });

  it('calls saveLicensesSpy with the correct information when you save an inherited status', () => {
    const componentIdentifier = Object({
      coordinates: Object({ name: 'jquery-form', qualifier: '', version: '3.50.0' }),
      format: 'a-name',
      displayName: 'jquery-form 3.50.0',
      hash: '9c06887e03feb6c996ab',
    });
    const expectedPostBody = {
      ownerType: undefined,
      ownerId: 'ROOT_ORGANIZATION_ID',
      postBody: Object({
        componentIdentifier,
        comment: '',
        status: 'SELECTED',
        ownerId: 'ROOT_ORGANIZATION_ID',
        licenseIds: ['overriddenLicense'],
      }),
      hash: 'hash123',
      componentIdentifier: JSON.stringify(componentIdentifier),
    };
    const wrapper = getMountedComponent({
      ownerId: 'orgPublicId',
      component: {
        licenseLegalData: {
          effectiveLicenses: ['License-1.0', 'License-2.0', 'License-1.0-License-2.0', 'License-2.0 or License-3.0'],
          observedLicenses: ['License-2.0', 'License-1.0-License-2.0', 'License-2.0 or License-3.0'],
          declaredLicenses: ['License-1.0-License-2.0'],
          effectiveLicenseStatus: 'Selected',
          hierarchy: [
            {
              licenseOverride: null,
              ownerId: 'appPublicId',
            },
            {
              licenseOverride: null,
              ownerId: 'orgPublicId',
            },
            {
              licenseOverride: {
                comment: 'anOverride3',
                status: 'SELECTED',
                licenseIds: ['overriddenLicense'],
              },
              ownerId: 'ROOT_ORGANIZATION_ID',
            },
          ],
        },
        componentIdentifier: {
          coordinates: { name: 'jquery-form', qualifier: '', version: '3.50.0' },
          format: 'a-name',
          displayName: 'jquery-form 3.50.0',
          hash: '9c06887e03feb6c996ab',
        },
      },
    });
    const form = wrapper.find(NxForm);
    const formGroups = wrapper.find(NxFormGroup);
    const statusGroup = formGroups.at(0);
    expect(statusGroup.find('option').at(5).childAt(0).text()).toEqual('Inherit Status (Selected)');
    const statusSelect = statusGroup.find('select');
    statusSelect.simulate('change', { target: { value: 'Inherit' } });
    form.simulate('submit');
    expect(saveLicensesSpy).toHaveBeenCalledWith(expectedPostBody);
  });
});
