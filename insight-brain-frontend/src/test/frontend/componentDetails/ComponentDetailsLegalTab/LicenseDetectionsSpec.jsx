/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import LicenseDetections from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetections';
import { NxList, NxLoadWrapper, NxThreatIndicator } from '@sonatype/react-shared-components';

describe('LicenseDetections', function () {
  let getShallow, getMounted, minimalProps, mountedComponent, toggleShowEditLicensesPopoverSpy, loadLicenses;

  beforeEach(function () {
    loadLicenses = jasmine.createSpy('loadLicenses');
    toggleShowEditLicensesPopoverSpy = jasmine.createSpy('toggleShowEditLicensesPopover');
    minimalProps = {
      declaredLicenses: null,
      effectiveLicenses: null,
      observedLicenses: null,
      loadLicenses: loadLicenses,
      loading: false,
      loadError: null,
      toggleShowEditLicensesPopover: toggleShowEditLicensesPopoverSpy,
      identificationSource: 'Sonatype',
    };
    getShallow = enzymeUtils.getShallowComponent(LicenseDetections, minimalProps);
    getMounted = enzymeUtils.getMountedComponent(LicenseDetections, minimalProps);
  });

  afterEach(() => {
    if (mountedComponent && mountedComponent.length > 0) mountedComponent.unmount();
  });

  it('calls loadLicenses on mount', function () {
    mountedComponent = getMounted();
    expect(loadLicenses).toHaveBeenCalledTimes(1);
    mountedComponent.unmount();
  });

  it('Hides the content on load', () => {
    const component = getShallow({ loading: true });
    const loadWrapper = component.find(NxLoadWrapper);

    expect(loadWrapper).toHaveProp('loading', true);

    const detectionsTile = loadWrapper.dive().find('#license-detections-tile');

    expect(detectionsTile).not.toExist();
  });

  it('renders an NxButton with label `Edit`', () => {
    const loadWrapperContents = getShallow().find(NxLoadWrapper).dive();
    const button = loadWrapperContents.find('#component-details-edit-licenses');
    expect(button.text()).toContain('Edit');
  });

  it('calls `toggleShowEditLicensesPopoverSpy` when `Edit` button clicked', () => {
    const loadWrapperContents = getShallow().find(NxLoadWrapper).dive();
    const button = loadWrapperContents.find('#component-details-edit-licenses');

    button.simulate('click');

    expect(toggleShowEditLicensesPopoverSpy).toHaveBeenCalledTimes(1);
  });

  describe('Shows the correct status', () => {
    it('no override', () => {
      const component = getShallow(),
        loadWrapperContents = component.find(NxLoadWrapper).dive(),
        status = loadWrapperContents.find('#status-subtitle');

      expect(status).toExist();
      expect(status).toHaveText('open');
    });

    it('app level', () => {
      const component = getShallow({
          licenseOverride: [
            {
              ownerId: 'wencelapp2.0',
              ownerName: 'wencel app 2.0',
              ownerType: 'application',
              licenseOverride: {
                status: 'OPEN',
              },
            },
            {
              ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
              ownerName: 'wencel org',
              ownerType: 'organization',
              licenseOverride: null,
            },
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              licenseOverride: null,
            },
          ],
        }),
        loadWrapperContents = component.find(NxLoadWrapper).dive(),
        status = loadWrapperContents.find('#status-subtitle');

      expect(status).toExist();
      expect(status).toHaveText('open');
    });

    it('org level', () => {
      const component = getShallow({
          licenseOverride: [
            {
              ownerId: 'wencelapp2.0',
              ownerName: 'wencel app 2.0',
              ownerType: 'application',
              licenseOverride: null,
            },
            {
              ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
              ownerName: 'wencel org',
              ownerType: 'organization',
              licenseOverride: {
                status: 'OVERRIDDEN',
              },
            },
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              licenseOverride: null,
            },
          ],
        }),
        loadWrapperContents = component.find(NxLoadWrapper).dive(),
        status = loadWrapperContents.find('#status-subtitle');

      expect(status).toExist();
      expect(status).toHaveText('overridden');
    });
    it('root level', () => {
      const component = getShallow({
          licenseOverride: [
            {
              ownerId: 'wencelapp2.0',
              ownerName: 'wencel app 2.0',
              ownerType: 'application',
              licenseOverride: null,
            },
            {
              ownerId: '5b862dfe2c95486f8395eca90c06dcfe',
              ownerName: 'wencel org',
              ownerType: 'organization',
              licenseOverride: null,
            },
            {
              ownerId: 'ROOT_ORGANIZATION_ID',
              ownerName: 'Root Organization',
              ownerType: 'organization',
              licenseOverride: {
                status: 'SELECTED',
              },
            },
          ],
        }),
        loadWrapperContents = component.find(NxLoadWrapper).dive(),
        status = loadWrapperContents.find('#status-subtitle');

      expect(status).toExist();
      expect(status).toHaveText('selected');
    });
  });

  describe('renders the licenses sections', () => {
    it('Effective Licenses section', () => {
      const component = getShallow({
        effectiveLicenses: ['Id1', 'Id2'],
        declaredLicenses: ['Id3'],
        observedLicenses: ['Id4', 'Id5', 'Id6', 'Id7'],
        licenseLegalMetadata: [
          {
            licenseId: 'Id1',
            licenseName: 'ELicense 1',
            isMulti: false,
            threatGroup: {
              threatLevel: 2,
            },
          },
          {
            licenseId: 'Id2',
            licenseName: 'ELicense 2',
            isMulti: false,
            threatGroup: {
              threatLevel: 7,
            },
          },
          {
            licenseId: 'Id3',
            licenseName: 'DLicense 1',
            isMulti: false,
            threatGroup: {
              threatLevel: 5,
            },
          },
          {
            licenseId: 'Id4',
            licenseName: 'OLicense 1',
            isMulti: false,
            threatGroup: {
              threatLevel: null,
            },
          },
          {
            licenseId: 'Id5',
            licenseName: 'OLicense 2',
            isMulti: false,
            threatGroup: {
              threatLevel: 0,
            },
          },
          {
            licenseId: 'Id6',
            licenseName: 'OLicense 3',
            isMulti: false,
            threatGroup: {
              threatLevel: 6,
            },
          },
          {
            licenseId: 'Id7',
            licenseName: 'OLicense 4',
            isMulti: true,
            singleLicenseIds: ['Id1', 'Id6'],
            threatGroup: {
              threatLevel: 3,
            },
          },
        ],
      });
      const loadWrapperContents = component.find(NxLoadWrapper).dive();
      const effectiveLicensesList = loadWrapperContents
          .find('#effective-licenses-container')
          .find(NxList)
          .find('div.license-list-item__license'),
        declaredLicenses = loadWrapperContents
          .find('#declared-licenses-container')
          .find(NxList)
          .find('div.license-list-item__license'),
        observedLicenses = loadWrapperContents
          .find('#observed-licenses-container')
          .find(NxList)
          .find('div.license-list-item__license');

      expect(effectiveLicensesList.length).toBe(2);

      expect(effectiveLicensesList.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 2);
      expect(effectiveLicensesList.at(0).find('span')).toHaveText('ELicense 1');

      expect(effectiveLicensesList.at(1).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 7);
      expect(effectiveLicensesList.at(1).find('span')).toHaveText('ELicense 2');

      expect(declaredLicenses.length).toBe(1);

      expect(declaredLicenses.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 5);
      expect(declaredLicenses.at(0).find('span')).toHaveText('DLicense 1');

      expect(observedLicenses.length).toBe(5);

      expect(observedLicenses.at(0).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', null);
      expect(observedLicenses.at(0).find('span')).toHaveText('OLicense 1');

      expect(observedLicenses.at(1).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 0);
      expect(observedLicenses.at(1).find('span')).toHaveText('OLicense 2');

      expect(observedLicenses.at(2).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 6);
      expect(observedLicenses.at(2).find('span')).toHaveText('OLicense 3');

      expect(observedLicenses.at(3).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 2);
      expect(observedLicenses.at(3).find('span')).toHaveText('ELicense 1');

      expect(observedLicenses.at(4).find(NxThreatIndicator)).toHaveProp('policyThreatLevel', 6);
      expect(observedLicenses.at(4).find('span')).toHaveText('OLicense 3');
    });

    it('renders licences for claimed component', () => {
      const component = getShallow({
        effectiveLicenses: ['UNSPECIFIED'],
        declaredLicenses: ['UNSPECIFIED'],
        observedLicenses: ['UNSPECIFIED'],
        licenseLegalMetadata: [
          {
            licenseId: 'UNSPECIFIED',
            licenseName: 'Not Provided',
            isMulti: false,
            threatGroup: {
              threatLevel: 5,
            },
          },
        ],
        identificationSource: 'Manual',
      });

      const loadWrapperContents = component.find(NxLoadWrapper).dive();
      const effectiveLicensesList = loadWrapperContents.find('#effective-licenses-container .license-list-item'),
        declaredLicenses = loadWrapperContents.find('#declared-licenses-container .license-list-item'),
        observedLicenses = loadWrapperContents.find('#observed-licenses-container .license-list-item');

      expect(effectiveLicensesList.length).toBe(1);

      expect(effectiveLicensesList.find('span')).toHaveText('Not Provided');

      expect(declaredLicenses.length).toBe(1);

      expect(declaredLicenses.find('span').at(0)).toHaveText('Not Provided');
      expect(declaredLicenses.find('span').at(1)).toHaveText(' (Claimed Component)');

      expect(observedLicenses.length).toBe(1);

      expect(observedLicenses.find('span').at(0)).toHaveText('Not Provided');
      expect(observedLicenses.find('span').at(1)).toHaveText(' (Claimed Component)');
    });
  });
});
