/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectComponentDetailsOverviewVersionExplorerSlice,
  selectInnerSourceProducerData,
  selectVersionExplorerRequestData,
  selectInnerSourceProducerUrl,
  selectShowInnerSourceProducerReportModal,
  selectInsufficientPermission,
  selectShowInsufficientPermissionsModal,
  selectLatestInnerSourceComponentVersion,
  selectRemediationData,
  selectComponenDetailsOverviewRemediationSlice,
} from '../../../../main/frontend/componentDetails/overview/overviewSelectors';

describe('overviewSelectors', () => {
  const graphExplorerData = {
    loading: false,
    loadError: null,
    data: {
      someData: 'data',
    },
  };

  const innerSourceProducerData = {
    reportUrl: 'http://localhost:8070/ui/links/application/appProducer/latestReport/build',
    latestInnerSourceComponentVersion: '2.0.0',
    insufficientPermission: false,
    loading: false,
    loadError: null,
    showInnerSourcePermissionsModal: false,
    showInnerSourceProducerReportModal: false,
  };

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
  const mockState = {
    router: {
      currentParams: {
        hash: 'some-component-hash',
        publicId: 'publicId',
        scanId: 'scanId',
      },
    },
    applicationReport: {
      selectedReport: {
        displayedEntries: [
          {
            hash: 'some-component-hash',
            matchState: 'exact',
            proprietary: false,
            identificationSource: 'is',
            derivedDependencyType: 'transitive',
            componentIdentifier: {
              format: 'format',
              coordinates: {
                version: '2.4.9',
              },
            },
          },
        ],
      },
      metadata: {
        stageId: 'build',
      },
    },
    componentDetailsOverview: {
      remediation,
      graphExplorerData,
      innerSourceProducerData,
    },
  };

  describe('selectComponentDetailsOverviewSlice', () => {
    it('selects the graphExplorerData slice of the state', () => {
      const expectedSelection = graphExplorerData;
      const actualSelection = selectComponentDetailsOverviewVersionExplorerSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects the selectVersionExplorerRequestData slice of the state', () => {
      const expectedSelection = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        matchState: 'exact',
        proprietary: false,
        identificationSource: 'is',
        componentIdentifier: {
          componentType: 'format',
          coordinates: {
            version: '2.4.9',
          },
        },
        hash: 'some-component-hash',
        scanId: 'scanId',
        stageId: 'build',
        dependencyType: 'transitive',
      };
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects innerSourceProducerData', () => {
      const expectedSelection = innerSourceProducerData;
      const actualSelection = selectInnerSourceProducerData(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects selectInnerSourceProducerUrl', () => {
      const expectedSelection = 'http://localhost:8070/ui/links/application/appProducer/latestReport/build';
      const actualSelection = selectInnerSourceProducerUrl(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects selectShowInnerSourceProducerReportModal', () => {
      const expectedSelection = false;
      const actualSelection = selectShowInnerSourceProducerReportModal(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects selectInsufficientPermission', () => {
      const expectedSelection = false;
      const actualSelection = selectInsufficientPermission(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects selectShowInsufficientPermissionsModal', () => {
      const expectedSelection = false;
      const actualSelection = selectShowInsufficientPermissionsModal(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects selectLatestInnerSourceComponentVersion', () => {
      const expectedSelection = '2.0.0';
      const actualSelection = selectLatestInnerSourceComponentVersion(mockState);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects the selectComponenDetailsOverviewRemediationSlice slice of the state', () => {
      const expectedSelection = remediation;
      const actualSelection = selectComponenDetailsOverviewRemediationSlice(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects the selectRemediationData slice of the state', () => {
      const expectedSelection = {
        actualVersion: '2.4.9',
        stageId: 'build',
      };
      const actualSelection = selectRemediationData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });
  });
});
