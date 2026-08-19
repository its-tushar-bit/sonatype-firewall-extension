/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectRiskRemediationSlice,
  selectCurrentVersion,
  selectVersionExplorerData,
  selectSelectedVersionData,
  selectSelectedVersion,
  selectCurrentVersionDetails,
  selectSelectedVersionDetails,
} from 'MainRoot/quarantinedComponentReport/riskRemediationTile/riskRemediationSelectors';

describe('riskRemediationSelectors', () => {
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

  const selectedVersionData = {
    loading: false,
    loadError: null,
    selectedVersionDetails: {
      field: 'field',
    },
    selectedVersion: '2.3',
  };

  describe('selectCurrentVersion', () => {
    const currentVersion = {
      currentVersion: '2.3',
    };

    it('is composed from the following selectors', () => {
      expect(selectCurrentVersion.dependencies).toEqual([selectRiskRemediationSlice]);
    });

    it('selects currentVersion from the root of the state', () => {
      const actualSelection = selectCurrentVersion.resultFunc(currentVersion);

      expect(actualSelection).toBe(currentVersion.currentVersion);
    });
  });

  describe('selectVersionExplorerData', () => {
    it('is composed from the following selector', () => {
      expect(selectVersionExplorerData.dependencies).toEqual([selectRiskRemediationSlice]);
    });

    it('selects the versionExplorerData slice of the state', () => {
      const riskRemediationState = {
        versionExplorerData,
      };
      const expectedSelection = versionExplorerData;

      const actualSelection = selectVersionExplorerData.resultFunc(riskRemediationState);

      expect(actualSelection).toBe(expectedSelection);
    });
  });

  describe('selectSelectedVersionData', () => {
    it('is composed from the following selector', () => {
      expect(selectSelectedVersionData.dependencies).toEqual([selectRiskRemediationSlice]);
    });

    it('selects selectedVersionData slice of the state', () => {
      const riskRemediationSlice = {
        selectedVersionData,
      };
      const actualSelection = selectSelectedVersionData.resultFunc(riskRemediationSlice);

      expect(actualSelection).toBe(selectedVersionData);
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

  describe('selectCurrentVersionDetails', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentVersionDetails.dependencies).toEqual([selectVersionExplorerData]);
    });

    it('selects the currentVersionDetails slice of the state', () => {
      const riskRemediationState = {
        currentVersionDetails: {
          currentVersion: '2.3',
        },
      };
      const expectedSelection = riskRemediationState.currentVersionDetails;

      const actualSelection = selectCurrentVersionDetails.resultFunc(riskRemediationState);

      expect(actualSelection).toBe(expectedSelection);
    });
  });

  describe('selectSelectedVersionDetails', () => {
    it('is composed from the following selector', () => {
      expect(selectSelectedVersionDetails.dependencies).toEqual([selectSelectedVersionData]);
    });

    it('selects the selectedVersionDetails slice of the state', () => {
      const riskRemediationState = {
        selectedVersionDetails: {
          selectedVersion: '2.3',
        },
      };
      const expectedSelection = riskRemediationState.selectedVersionDetails;

      const actualSelection = selectSelectedVersionDetails.resultFunc(riskRemediationState);

      expect(actualSelection).toBe(expectedSelection);
    });
  });
});
