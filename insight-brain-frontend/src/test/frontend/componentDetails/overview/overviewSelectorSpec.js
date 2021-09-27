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
  selectInsufficientPermission,
  selectShowInsufficientPermissionsModal,
  selectLatestInnerSourceComponentVersion,
  selectRemediationData,
  selectComponentDetailsRequestData,
} from '../../../../main/frontend/componentDetails/overview/overviewSelectors';

describe('overviewSelectors', () => {
  let versionExplorerData, innerSourceProducerData, mockState;

  beforeEach(() => {
    versionExplorerData = {
      loading: false,
      loadError: null,
      data: {
        someData: 'data',
      },
    };

    innerSourceProducerData = {
      reportUrl: 'http://localhost:8070/ui/links/application/appProducer/latestReport/build',
      latestInnerSourceComponentVersion: '2.0.0',
      insufficientPermission: false,
      loading: false,
      loadError: null,
      showInnerSourcePermissionsModal: false,
      showInnerSourceProducerReportModal: false,
    };

    mockState = {
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
        versionExplorerData,
        innerSourceProducerData,
      },
    };
  });

  describe('selectVersionExplorerData', () => {
    it('selects the versionExplorerData slice of the state', () => {
      const actualSelection = selectVersionExplorerData(mockState);
      expect(actualSelection).toBe(versionExplorerData);
    });
  });

  describe('selectComponentDetailsRequestData', () => {
    it('selects the data for Component Details request', () => {
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
      };
      const actualSelection = selectComponentDetailsRequestData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('sets componentIdentifier to null if matchState is null', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].matchState = null;
      const actualSelection = selectComponentDetailsRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if matchState is "unknown"', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].matchState = 'unknown';
      const actualSelection = selectComponentDetailsRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if coordinates is null', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].componentIdentifier.coordinates = null;
      const actualSelection = selectComponentDetailsRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });
  });

  describe('selectComponentDetailsRequestData', () => {
    it('selects the data for allVersions request', () => {
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
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection).toEqual(expectedSelection);
    });

    it('sets componentIdentifier to null if matchState is null', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].matchState = null;
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if matchState is "unknown"', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].matchState = 'unknown';
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });

    it('sets componentIdentifier to null if coordinates is null', () => {
      mockState.applicationReport.selectedReport.displayedEntries[0].componentIdentifier.coordinates = null;
      const actualSelection = selectVersionExplorerRequestData(mockState);
      expect(actualSelection.componentIdentifier).toBeNull();
    });
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

  it('selects the selectRemediationData slice of the state', () => {
    const expectedSelection = {
      currentVersion: '2.4.9',
      stageId: 'build',
    };
    const actualSelection = selectRemediationData(mockState);
    expect(actualSelection).toEqual(expectedSelection);
  });
});
