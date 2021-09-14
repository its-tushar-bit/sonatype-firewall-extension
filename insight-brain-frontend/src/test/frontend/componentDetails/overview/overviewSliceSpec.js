/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../main/frontend/componentDetails/overview/overviewSlice';

describe('componentDetailspolicyViolationsSlice', () => {
  const stateConstantObject = { value: 'test value' };

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({
        foo: 'bar',
        graphExplorerData: {
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

  describe('componentDetailsOverview/loadVersionGraphData/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        graphExplorerData: {
          loading: false,
          loadError: 'There is an error',
          data: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionGraphData/pending',
      });

      expect(newState.graphExplorerData.loading).toBe(true);
      expect(newState.graphExplorerData.loadError).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsOverview/loadVersionGraphData/fulfilled action', () => {
    it('sets loading flag to false, unsets the loadError and fills the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        graphExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
      });
      const versionsList = ['list'];
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
      const payload = {
        data: {
          remediation: remediation,
          allVersions: versionsList,
        },
      };

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionGraphData/fulfilled',
        payload,
      });

      expect(newState.graphExplorerData.loading).toBe(false);
      expect(newState.graphExplorerData.loadError).toBe(null);
      expect(newState.graphExplorerData.data).toEqual({ versions: versionsList });
      expect(newState.remediation).toEqual(remediation);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsOverview/loadVersionGraphData/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        graphExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionGraphData/rejected',
        payload: 'loadError',
      });

      expect(newState.graphExplorerData.loading).toBe(false);
      expect(newState.graphExplorerData.loadError).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });
});
