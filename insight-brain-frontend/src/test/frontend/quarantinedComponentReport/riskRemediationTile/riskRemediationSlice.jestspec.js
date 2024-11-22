/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/quarantinedComponentReport/riskRemediationTile/riskRemediationSlice';

describe('quarantinedReportRiskRemediationSliceSpec', () => {
  const stateConstantObject = { value: 'test value' };
  const selectedVersionData = {
    loading: false,
    loadError: null,
    selectedVersionDetails: null,
    selectedVersion: null,
  };

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({
        foo: 'bar',
        versionExplorerData: {
          loading: false,
          loadError: null,
          data: null,
        },
      });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('quarantinedReportRiskRemediation/loadVersionExplorerData/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: false,
          loadError: 'There is an error',
          data: null,
        },
        expanded: true,
      });

      const newState = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadVersionExplorerData/pending',
      });

      expect(newState.versionExplorerData.loading).toBe(true);
      expect(newState.versionExplorerData.loadError).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('quarantinedReportRiskRemediation/loadVersionExplorerData/fulfilled action', () => {
    it('sets loading flag to false, unsets the loadError and fills the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
        expanded: false,
      });
      const allVersions = ['list'];
      const source = 'https://repo.sonatype.com/';
      const remediation = {
        versionChanges: [
          {
            type: 'next-non-failing',
            data: {
              component: {
                packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
                hash: null,
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'h2',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'com.h2database',
                    version: '1.4.200',
                  },
                },
                displayName: 'com.h2database : h2 : 1.4.200',
              },
            },
          },
          {
            type: 'next-non-failing-with-dependencies',
            data: {
              component: {
                packageUrl: 'pkg:maven/com.h2database/h2@1.4.200?type=jar',
                hash: null,
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'h2',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'com.h2database',
                    version: '1.4.200',
                  },
                },
                displayName: 'com.h2database : h2 : 1.4.200',
              },
            },
          },
        ],
      };
      const currentVersionDetails = {};
      const payload = {
        componentVersionsData: {
          remediation,
          allVersions,
          source,
        },
        currentVersionDetails,
      };

      const newState = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadVersionExplorerData/fulfilled',
        payload,
      });

      expect(newState.versionExplorerData.loading).toBe(false);
      expect(newState.versionExplorerData.loadError).toBe(null);
      expect(newState.versionExplorerData.versions).toBe(allVersions);
      expect(newState.versionExplorerData.remediation).toBe(remediation);
      expect(newState.versionExplorerData.source).toBe(source);
      expect(newState.versionExplorerData.currentVersionDetails).toBe(currentVersionDetails);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('quarantinedReportRiskRemediation/loadVersionExplorerData/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
      });

      const newState = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadVersionExplorerData/rejected',
        payload: 'loadError',
      });

      expect(newState.versionExplorerData.loading).toBe(false);
      expect(newState.versionExplorerData.loadError).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('quarantinedReportRiskRemediation/setSelectedVersion', () => {
    it('sets version value for selectedVersion', () => {
      const state = Object.freeze({ selectedVersionData });

      const {
        selectedVersionData: { selectedVersion },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/setSelectedVersion',
        payload: '2.3',
      });

      expect(selectedVersion).toBe('2.3');
    });
  });

  describe('quarantinedReportRiskRemediation/resetSelectedVersionData', () => {
    it('sets version value for selectedVersion', () => {
      const state = Object.freeze({
        selectedVersionData: {
          ...selectedVersionData,
          loadError: 'error',
          selectedVersion: '2.3',
        },
      });

      const {
        selectedVersionData: { selectedVersion, loadError, loading, selectedVersionDetails },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/resetSelectedVersionData',
      });

      expect(selectedVersion).toBeNull();
      expect(loadError).toBeNull();
      expect(selectedVersionDetails).toBeNull();
      expect(loading).toBe(false);
    });
  });

  describe('quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/pending action', () => {
    it('sets the loadError to null and the loading flag to true', () => {
      const state = Object.freeze({
        selectedVersionData: {
          ...selectedVersionData,
          loadError: 'error',
          selectedVersion: '2.3',
          selectedVersionDetails: {
            some: 'data',
          },
        },
      });

      const {
        selectedVersionData: { loading, loadError, selectedVersionDetails },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
      expect(selectedVersionDetails).toBeNull();
    });
  });

  describe('quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        selectedVersionData: {
          loading: true,
          loadError: 'error',
          selectedVersion: '2.3',
          selectedVersionDetails: {
            data: 'some data',
          },
        },
      });

      const {
        selectedVersionData: { loading, loadError, selectedVersionDetails },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
      expect(selectedVersionDetails).toBeNull();
    });

    it('sets selectedVersionDetails to null if request was aborted by client', () => {
      const state = Object.freeze({
        selectedVersionData: {
          loading: true,
          loadError: null,
          selectedVersion: '2.3',
          selectedVersionDetails: {
            data: 'some data',
          },
        },
      });

      const {
        selectedVersionData: { loading, loadError, selectedVersionDetails },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/rejected',
        payload: { message: 499 },
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
      expect(selectedVersionDetails).toBeNull();
    });
  });

  describe('quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/fulfilled action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        selectedVersionData: {
          loading: true,
          loadError: 'error',
          selectedVersion: '2.3',
        },
      });

      const {
        selectedVersionData: { loading, loadError, selectedVersionDetails },
      } = reducer(state, {
        type: 'quarantinedReportRiskRemediation/loadComponentDetailsByVersionNumber/fulfilled',
        payload: {
          data: 'version data',
        },
      });

      expect(loading).toBe(false);
      expect(loadError).toBeNull();
      expect(selectedVersionDetails).toEqual({ data: 'version data' });
    });
  });
});
