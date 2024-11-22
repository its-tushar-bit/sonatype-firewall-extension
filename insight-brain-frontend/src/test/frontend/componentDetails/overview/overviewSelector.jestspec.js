/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectVersionExplorerData,
  selectInnerSourceProducerData,
  selectVersionExplorerRequestData,
  selectInnerSourceProducerUrl,
  selectShowInnerSourceProducerReportModal,
  selectComponentDetailsOverviewSlice,
  selectInsufficientPermission,
  selectShowInsufficientPermissionsModal,
  selectLatestInnerSourceComponentVersion,
  selectRemediationData,
  selectComponentDetailsRequestData,
  selectCurrentVersion,
  selectSelectedVersion,
  selectAvailableVersions,
  selectSelectedVersionDetailsByVersionId,
  selectComponentDetailsSelectedRequestData,
  selectSelectedVersionData,
  selectCurrentVersionDetails,
  selectShowComponentCoordinatesPopover,
  selectisLoadingApplicationReportOrComponentDetails,
} from 'MainRoot/componentDetails/overview/overviewSelectors';
import { selectComponentDetails } from 'MainRoot/componentDetails/componentDetailsSelectors';
import {
  selectSelectedComponent,
  selectApplicationReportMetaData,
  selectApplicationReportSlice,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('overviewSelectors', () => {
  const versionExplorerData = {
    versions: [
      {
        componentIdentifier: {
          coordinates: {
            artifactId: 'commons-io',
            classifier: '',
            extension: 'jar',
            groupId: 'commons-io',
            version: '2.3',
          },
          format: 'maven',
        },
        matchState: 'exact',
        identificationSource: 'Sonatype',
      },
      {
        componentIdentifier: {
          coordinates: {
            artifactId: 'commons-io',
            classifier: '',
            extension: 'jar',
            groupId: 'commons-io',
            version: '2.4.9',
          },
          format: 'maven',
        },
        matchState: 'exact',
        identificationSource: 'Sonatype',
      },
    ],
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

  const applicationReportMetaData = {
    stageId: 'build',
  };

  const routerCurrentParams = {
    hash: 'some-component-hash',
    publicId: 'publicId',
    scanId: 'scanId',
  };

  const partialSelectedComponent = {
    hash: 'some-component-hash',
    proprietary: false,
    matchState: 'exact',
    identificationSource: 'is',
  };

  const selectedComponent = {
    ...partialSelectedComponent,
    componentIdentifier: {
      format: 'format',
      coordinates: {
        version: '2.4.9',
      },
    },
  };

  const componentDetailsRequestData = {
    ...partialSelectedComponent,
    clientType: 'ci',
    ownerType: 'application',
    ownerId: 'publicId',
    componentIdentifier: '{"format":"format","coordinates":{"version":"2.4.9"}}',
    scanId: 'scanId',
  };

  const selectedVersionData = {
    loading: false,
    loadError: null,
    selectedVersionDetails: null,
    selectedVersion: '2.3',
  };

  const componentDetailsOverview = {
    versionExplorerData,
    innerSourceProducerData,
    selectedVersionData,
  };

  describe('selectVersionExplorerData', () => {
    it('is composed from the following selector', () => {
      expect(selectVersionExplorerData.dependencies).toEqual([selectComponentDetailsOverviewSlice]);
    });

    it('selects the versionExplorerData slice of the state', () => {
      const componentDetailsOverviewSlice = {
        versionExplorerData,
        innerSourceProducerData,
      };
      const expectedSelection = versionExplorerData;

      const actualSelection = selectVersionExplorerData.resultFunc(componentDetailsOverviewSlice);

      expect(actualSelection).toBe(expectedSelection);
    });
  });

  describe('selectComponentDetailsRequestData', () => {
    it('is composed from the following selectors', () => {
      expect(selectComponentDetailsRequestData.dependencies).toEqual([
        selectSelectedComponent,
        selectRouterCurrentParams,
      ]);
    });

    it('selects the data for Component Details request', () => {
      const expectedSelection = componentDetailsRequestData;

      const actualSelection = selectComponentDetailsRequestData.resultFunc(selectedComponent, routerCurrentParams);

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('sets componentIdentifier to null if matchState is null', () => {
      const localSelectedComponent = {
        ...selectedComponent,
        matchState: null,
      };

      const actualSelection = selectComponentDetailsRequestData.resultFunc(
        localSelectedComponent,
        applicationReportMetaData,
        routerCurrentParams
      );

      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if matchState is "unknown"', () => {
      const localSelectedComponent = {
        ...selectedComponent,
        matchState: 'unknown',
      };

      const actualSelection = selectComponentDetailsRequestData.resultFunc(
        localSelectedComponent,
        applicationReportMetaData,
        routerCurrentParams
      );

      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if coordinates is null', () => {
      const localSelectedComponent = {
        ...selectedComponent,
        componentIdentifier: { ...selectedComponent.componentIdentifier, coordinates: null },
      };

      const actualSelection = selectComponentDetailsRequestData.resultFunc(
        localSelectedComponent,
        applicationReportMetaData,
        routerCurrentParams
      );

      expect(actualSelection.componentIdentifier).toBeNull();
    });
  });

  describe('selectComponentDetailsRequestData', () => {
    it('is composed from the following selectors', () => {
      expect(selectVersionExplorerRequestData.dependencies).toEqual([
        selectSelectedComponent,
        selectApplicationReportMetaData,
        selectComponentDetailsRequestData,
      ]);
    });

    it('selects the data for allVersions request', () => {
      const localSelectedComponent = { derivedDependencyType: 'transitive' };
      const expectedSelection = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        matchState: 'exact',
        proprietary: false,
        identificationSource: 'is',
        componentIdentifier: '{"format":"format","coordinates":{"version":"2.4.9"}}',
        hash: 'some-component-hash',
        scanId: 'scanId',
        stageId: 'build',
        dependencyType: 'transitive',
      };

      const actualSelection = selectVersionExplorerRequestData.resultFunc(
        localSelectedComponent,
        applicationReportMetaData,
        componentDetailsRequestData
      );

      expect(actualSelection).toEqual(expectedSelection);
    });

    it('selects the data for allVersions request for an InnerSource component', () => {
      const localSelectedComponent = { innerSource: true, derivedDependencyType: 'transitive' };
      const expectedSelection = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        matchState: 'exact',
        proprietary: false,
        identificationSource: 'is',
        componentIdentifier: '{"format":"format","coordinates":{"version":"2.4.9"}}',
        hash: 'some-component-hash',
        scanId: 'scanId',
        stageId: 'build',
        dependencyType: 'innersource',
      };

      const actualSelection = selectVersionExplorerRequestData.resultFunc(
        localSelectedComponent,
        applicationReportMetaData,
        componentDetailsRequestData
      );

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectInnerSourceProducerData', () => {
    it('is composed from the following selector', () => {
      expect(selectInnerSourceProducerData.dependencies).toEqual([selectComponentDetailsOverviewSlice]);
    });

    it('selects innerSourceProducerData', () => {
      const expectedSelection = innerSourceProducerData;

      const actualSelection = selectInnerSourceProducerData.resultFunc(componentDetailsOverview);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectInnerSourceProducerUrl', () => {
    it('is composed from the following selector', () => {
      expect(selectInnerSourceProducerUrl.dependencies).toEqual([selectInnerSourceProducerData]);
    });

    it('selects innerSourceProducerUrl', () => {
      const expectedSelection = 'http://localhost:8070/ui/links/application/appProducer/latestReport/build';

      const actualSelection = selectInnerSourceProducerUrl.resultFunc(innerSourceProducerData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectShowInnerSourceProducerReportModal', () => {
    it('is composed from the following selector', () => {
      expect(selectShowInnerSourceProducerReportModal.dependencies).toEqual([selectInnerSourceProducerData]);
    });

    it('selects innerSourceProducerReportModal', () => {
      const expectedSelection = false;

      const actualSelection = selectShowInnerSourceProducerReportModal.resultFunc(innerSourceProducerData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectInsufficientPermission', () => {
    it('is composed from the following selector', () => {
      expect(selectInsufficientPermission.dependencies).toEqual([selectInnerSourceProducerData]);
    });

    it('selects insufficientPermission', () => {
      const expectedSelection = false;

      const actualSelection = selectInsufficientPermission.resultFunc(innerSourceProducerData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectShowInsufficientPermissionsModal', () => {
    it('is composed from the following selector', () => {
      expect(selectShowInsufficientPermissionsModal.dependencies).toEqual([selectInnerSourceProducerData]);
    });

    it('selects showInsufficientPermissionsModal', () => {
      const expectedSelection = false;

      const actualSelection = selectShowInsufficientPermissionsModal.resultFunc(innerSourceProducerData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectLatestInnerSourceComponentVersion', () => {
    it('is composed from the following selector', () => {
      expect(selectLatestInnerSourceComponentVersion.dependencies).toEqual([selectInnerSourceProducerData]);
    });

    it('selects innerSourceComponentVersion', () => {
      const expectedSelection = '2.0.0';

      const actualSelection = selectLatestInnerSourceComponentVersion.resultFunc(innerSourceProducerData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectRemediationData', () => {
    it('is composed from the following selectors', () => {
      expect(selectRemediationData.dependencies).toEqual([selectSelectedComponent, selectApplicationReportMetaData]);
    });

    it('selects the remediationData slice of the state', () => {
      const expectedSelection = {
        currentVersion: '2.4.9',
        stageId: 'build',
      };

      const actualSelection = selectRemediationData.resultFunc(selectedComponent, applicationReportMetaData);

      expect(actualSelection).toEqual(expectedSelection);
    });
  });

  describe('selectSelectedVersion', () => {
    it('is composed from the following selector', () => {
      expect(selectSelectedVersion.dependencies).toEqual([selectSelectedVersionData]);
    });

    it('selects selected version', () => {
      const actualSelection = selectSelectedVersion.resultFunc(selectedVersionData);

      expect(actualSelection).toBe('2.3');
    });
  });

  describe('selectCurrentVersion', () => {
    const currentVersionDetails = {
      componentIdentifier: {
        coordinates: {
          version: '2.3-compIdentifier',
        },
      },
      version: '2.3-from-fallback',
    };

    it('is composed from the following selectors', () => {
      expect(selectCurrentVersion.dependencies).toEqual([selectCurrentVersionDetails]);
    });

    it('selects current version from componentIdentifier object', () => {
      const actualSelection = selectCurrentVersion.resultFunc(currentVersionDetails);

      expect(actualSelection).toBe('2.3-compIdentifier');
    });

    it('selects current version from version prop as a fallback', () => {
      const actualSelection = selectCurrentVersion.resultFunc({
        version: '2.3-from-fallback',
      });

      expect(actualSelection).toBe('2.3-from-fallback');
    });
  });

  describe('selectAvailableVersions', () => {
    it('is composed from the following selector', () => {
      expect(selectAvailableVersions.dependencies).toEqual([selectVersionExplorerData]);
    });

    it('selects all available versions array', () => {
      const actualSelection = selectAvailableVersions.resultFunc(versionExplorerData);

      expect(actualSelection).toEqual(versionExplorerData.versions);
    });
  });

  describe('selectSelectedVersionDetailsByVersionId', () => {
    it('is composed from the following selectors', () => {
      expect(selectSelectedVersionDetailsByVersionId.dependencies).toEqual([
        selectAvailableVersions,
        selectSelectedVersion,
      ]);
    });

    it('selects matching version from available array of versions', () => {
      const actualSelection = selectSelectedVersionDetailsByVersionId.resultFunc(
        versionExplorerData.versions,
        selectedVersionData.selectedVersion
      );

      expect(actualSelection).toEqual(versionExplorerData.versions[0]);
    });

    it('selects undefined if selected version does not match any from available array of versions', () => {
      const mockSelectedVersion = '2.4';

      const actualSelection = selectSelectedVersionDetailsByVersionId.resultFunc(
        versionExplorerData.versions,
        mockSelectedVersion
      );

      expect(actualSelection).toBe(undefined);
    });
  });

  describe('selectComponentDetailsSelectedRequestData', () => {
    it('is composed from the following selectors', () => {
      expect(selectComponentDetailsSelectedRequestData.dependencies).toEqual([
        selectSelectedVersionDetailsByVersionId,
        selectRouterCurrentParams,
      ]);
    });

    it('selects the data for Component Details request ', () => {
      const expectedSelection = {
        clientType: 'ci',
        ownerType: 'application',
        ownerId: 'publicId',
        matchState: 'exact',
        identificationSource: 'Sonatype',
        componentIdentifier:
          '{"format":"maven","coordinates":{"artifactId":"commons-io","classifier":"","extension":"jar","groupId":"commons-io","version":"2.3"}}',
        hash: undefined,
        proprietary: undefined,
        scanId: 'scanId',
      };

      const actualSelection = selectComponentDetailsSelectedRequestData.resultFunc(
        versionExplorerData.versions[0],
        routerCurrentParams
      );
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('sets componentIdentifier to null if matchState is null', () => {
      const mockVersions = [...versionExplorerData.versions];
      mockVersions[0].matchState = null;

      const actualSelection = selectComponentDetailsSelectedRequestData.resultFunc(mockVersions, routerCurrentParams);

      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if matchState is "unknown"', () => {
      const mockVersions = [...versionExplorerData.versions];
      mockVersions[0].matchState = 'unknown';

      const actualSelection = selectComponentDetailsSelectedRequestData.resultFunc(mockVersions, routerCurrentParams);

      expect(actualSelection.componentIdentifier).toBeNull();
    });
  });

  describe('selectShowComponentCoordinatesPopover', () => {
    it('is composed from the following selector', () => {
      expect(selectShowComponentCoordinatesPopover.dependencies).toEqual([selectComponentDetailsOverviewSlice]);
    });

    it('selects the showComponentCoordinatesPopover ', () => {
      const actualSelection = selectShowComponentCoordinatesPopover.resultFunc({
        showComponentCoordinatesPopover: true,
      });

      expect(actualSelection).toBe(true);
    });
  });

  describe('selectisLoadingApplicationReportOrComponentDetails', () => {
    const mockState = {
      router: {
        currentParams: {
          hash: 'some-component-hash',
        },
        currentState: {
          name: 'router-state-name',
        },
      },
      applicationReport: {
        metadata: {
          application: {
            name: 'The App',
            publicId: 'TheApp',
            organization: {
              name: 'The Org',
            },
          },
          reportTime: 1623135382098,
          reportTitle: 'Title of Report',
          stageId: 'test',
        },
        selectedReport: {
          allEntries: [
            {
              derivedComponentName: 'My Component',
              hash: 'some-component-hash',
              componentIdentifier: { format: 'maven' },
              derivedDependencyType: 'transitive',
              policyThreatLevel: 10,
            },
          ],
        },
        pendingLoads: new Set(['test']),
      },
      componentDetails: {
        pendingLoads: new Set(['test']),
      },
    };

    it('is composed of the following selectors', () => {
      expect(selectisLoadingApplicationReportOrComponentDetails.dependencies).toEqual([
        selectApplicationReportSlice,
        selectComponentDetails,
      ]);
    });

    it('returns true if the application report is loading', () => {
      const state = {
        ...mockState,
        applicationReport: {
          pendingLoads: new Set(['common']),
        },
      };
      const actual = selectisLoadingApplicationReportOrComponentDetails(state);
      expect(actual).toEqual(true);
    });

    it('returns true if there are no component details in the state', () => {
      const state = {
        ...mockState,
        router: {
          currentParams: {
            hash: 'new-hash',
          },
        },
      };
      const actual = selectisLoadingApplicationReportOrComponentDetails(state);
      expect(actual).toEqual(true);
    });

    it('returns false if app report & component details are loaded', () => {
      const state = {
        ...mockState,
        componentDetails: {
          pendingLoads: new Set([]),
        },
        applicationReport: {
          ...mockState.applicationReport,
          pendingLoads: new Set([]),
        },
      };
      const actual = selectisLoadingApplicationReportOrComponentDetails(state);
      expect(actual).toEqual(false);
    });
  });
});
