/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import VersionGraphExplorer from 'MainRoot/componentDetails/overview/VersionGraphExplorer/VersionGraphExplorer';
import { RiskRemediation } from 'MainRoot/componentDetails/overview/riskRemediation/RiskRemediation';

describe('RiskRemediation', () => {
  let minimalProps, getMounted;

  const allVersions = [
    {
      matchState: 'exact',
      declaredLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      overriddenLicenses: [],
      effectiveLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      effectiveLicenseStatus: null,
      catalogDate: 1589521958000,
      relativePopularity: 29,
      website: null,
      policyMaxThreatLevelsByCategory: {},
      violatedPolicyCount: 0,
      highestSecurityVulnerabilitySeverity: 0.0,
      securityVulnerabilityCount: 0,
      majorRevisionStep: false,
      identificationSource: 'Sonatype',
      identificationSourceComment: null,
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'org.springframework.boot',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'spring-boot-jarmode-layertools',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: '2.3.0.RELEASE',
          },
        ],
        name: 'spring-boot-jarmode-layertools',
      },
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'spring-boot-jarmode-layertools',
          classifier: '',
          extension: 'jar',
          groupId: 'org.springframework.boot',
          version: '2.3.0.RELEASE',
        },
      },
      policyAlerts: [],
      breakingChangesCount: null,
    },
    {
      matchState: 'exact',
      declaredLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      observedLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      overriddenLicenses: [],
      effectiveLicenses: [
        {
          licenseId: 'Apache-2.0',
          licenseName: 'Apache-2.0',
        },
      ],
      effectiveLicenseStatus: null,
      catalogDate: 1589521958000,
      relativePopularity: 31,
      website: null,
      policyMaxThreatLevelsByCategory: {},
      violatedPolicyCount: 0,
      highestSecurityVulnerabilitySeverity: 0.0,
      securityVulnerabilityCount: 0,
      majorRevisionStep: false,
      identificationSource: 'Sonatype',
      identificationSourceComment: null,
      displayName: {
        parts: [
          {
            field: 'Group',
            value: 'org.springframework.boot',
          },
          {
            value: ' : ',
          },
          {
            field: 'Artifact',
            value: 'spring-boot-jarmode-layertools',
          },
          {
            value: ' : ',
          },
          {
            field: 'Version',
            value: '2.3.1.RELEASE',
          },
        ],
        name: 'spring-boot-jarmode-layertools',
      },
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'spring-boot-jarmode-layertools',
          classifier: '',
          extension: 'jar',
          groupId: 'org.springframework.boot',
          version: '2.3.1.RELEASE',
        },
      },
      policyAlerts: [],
      breakingChangesCount: null,
    },
  ];

  const remediation = {
    versionChanges: [
      {
        type: 'next-no-violations',
        data: {
          component: {
            packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.10?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'spring-boot-jarmode-layertools',
                classifier: '',
                extension: 'jar',
                groupId: 'org.springframework.boot',
                version: '2.4.10',
              },
            },
            displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.10',
          },
        },
      },
      {
        type: 'next-non-failing',
        data: {
          component: {
            packageUrl: 'pkg:maven/org.springframework.boot/spring-boot-jarmode-layertools@2.4.9?type=jar',
            hash: null,
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'spring-boot-jarmode-layertools',
                classifier: '',
                extension: 'jar',
                groupId: 'org.springframework.boot',
                version: '2.4.9',
              },
            },
            displayName: 'org.springframework.boot : spring-boot-jarmode-layertools : 2.4.9',
          },
        },
      },
    ],
  };

  beforeEach(function () {
    minimalProps = {
      currentVersion: '123',
      dependencyTreeSubset: [
        {
          hash: '502f98a535313e13cf18',
          displayName: 'org.springframework.data : spring-data-rest-hal-explorer : 3.4.11',
        },
      ],
      actualVersion: '2.4.19',
      stageId: 'build',
      routeName: 'applicationReport.componentDetails.overview',
      loadVersionExplorerData: jasmine.createSpy('loadVersionExplorerData'),
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: null,
        remediation: remediation,
        sourceResponse: null,
      },
      selectedVersionData: {
        loading: false,
        loadError: null,
        selectedVersionDetails: null,
        selectedVersion: null,
      },
      componentInformation: {},
    };

    getMounted = enzymeUtils.getMountedComponent(RiskRemediation, minimalProps);
  });

  it('renders Recommended Remediation section if it is a transitive dependency', () => {
    const component = getMounted({
        componentInformation: { dependencyInfo: { isDirectDependency: false } },
      }),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(1);
    const ancestorsList = dependencyInfoTile.find('.nx-list');
    expect(ancestorsList.length).toBe(1);
    const listElements = ancestorsList.find('li');
    expect(listElements.length).toBe(1);
  });

  it('renders Recommended Remediation section even if dependencyTreeSubset is empty', () => {
    const component = getMounted({
        dependencyTreeSubset: [],
        componentInformation: { dependencyInfo: { isDirectDependency: false } },
      }),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(1);
  });

  it('does not render Recommended Remediation section if it is a direct dependency', () => {
    const component = getMounted({
        componentInformation: { dependencyInfo: { isDirectDependency: true } },
      }),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(0);
  });

  it('does not render Recommended Remediation section if it has no dependencyInfo', () => {
    const component = getMounted(),
      dependencyInfoTile = component.find('.iq-dependency-information');

    expect(dependencyInfoTile.length).toBe(0);
  });

  it('calls the loadVersionExplorerData method when mounted and VersionGraphExplorer not to exists', () => {
    const component = getMounted().find(VersionGraphExplorer);
    expect(component).not.toExist();
    expect(minimalProps.loadVersionExplorerData).toHaveBeenCalledTimes(1);
  });

  it('renders the VersionGraphExplorer', () => {
    const component = getMounted({
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: allVersions,
        sourceResponse: null,
      },
    });

    const versionExplorerTile = component.find('iq-version-explorer');
    const content = versionExplorerTile.find('#aiVersionChartContainer');
    expect(content).not.toBeNull();
    const versionExplorerComponent = component.find(VersionGraphExplorer);
    expect(versionExplorerComponent).toHaveProp('versions', allVersions);
    expect(versionExplorerComponent).toHaveProp('currentVersion', '123');
    const versionExplorerRepositorySource = component.find('#iq-version-explorer-repository-source');
    expect(versionExplorerRepositorySource).toHaveSize(0);
  });

  it('renders the VersionGraphExplorer with the Repository Source', () => {
    const component = getMounted({
      versionExplorerData: {
        loading: false,
        loadError: null,
        versions: allVersions,
        sourceResponse: { source: 'https://repo.sonatype.com/' },
      },
    });

    const versionExplorerTile = component.find('iq-version-explorer');
    const content = versionExplorerTile.find('#aiVersionChartContainer');
    expect(content).not.toBeNull();
    const versionExplorerComponent = component.find(VersionGraphExplorer);
    expect(versionExplorerComponent).toHaveProp('versions', allVersions);
    expect(versionExplorerComponent).toHaveProp('currentVersion', '123');
    const versionExplorerRepositorySource = component.find('#iq-version-explorer-repository-source');
    expect(versionExplorerRepositorySource).toHaveSize(1);
    expect(versionExplorerRepositorySource).toHaveText('Repository Source: https://repo.sonatype.com/');
  });

  it('renders the Recommended Versions tile', () => {
    const component = getMounted(),
      recommendedVersionTile = component.find('.iq-recommended-version');
    expect(recommendedVersionTile).not.toBeNull();
    const recommendedVersionsList = recommendedVersionTile.find('.nx-list');
    expect(recommendedVersionsList.length).toBe(1);
    const listElements = recommendedVersionsList.find('li');
    expect(listElements.length).toBe(2);
  });

  describe('selected version load error modal', () => {
    let minProps, getShallow, cancelMock;

    beforeEach(() => {
      cancelMock = jasmine.createSpy('resetSelectedVersionData');
      minProps = {
        ...minimalProps,
        selectedVersionData: {
          loadError: 'error',
          selectedVersion: '2.3',
        },
        resetSelectedVersionData: cancelMock,
      };

      getShallow = enzymeUtils.getShallowComponent(RiskRemediation, minProps);
    });

    it('renders selected version load error modal', () => {
      const modal = getShallow().find('#selected-version-error-modal');
      expect(modal).toExist();
    });

    it('calls resetSelectedVersionData handler on modal close action', () => {
      const modal = getShallow().find('#selected-version-error-modal');

      modal.simulate('cancel');

      expect(cancelMock).toHaveBeenCalledTimes(1);
    });
  });
});
