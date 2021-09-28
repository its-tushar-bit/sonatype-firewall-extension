/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../main/frontend/componentDetails/overview/overviewSlice';

describe('componentDetailsOverviewReducer', () => {
  const stateConstantObject = { value: 'test value' };
  const initialInnerSourceProducerData = {
    reportUrl: '',
    latestInnerSourceComponentVersion: '',
    insufficientPermission: false,
    loading: false,
    loadError: null,
    showInnerSourcePermissionsModal: false,
    showInnerSourceProducerReportModal: false,
  };

  describe('unknown action', () => {
    it('returns original state', function () {
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

  describe('componentDetailsOverview/loadVersionExplorerData/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: false,
          loadError: 'There is an error',
          data: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionExplorerData/pending',
      });

      expect(newState.versionExplorerData.loading).toBe(true);
      expect(newState.versionExplorerData.loadError).toBe(null);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsOverview/loadVersionExplorerData/fulfilled action', () => {
    it('sets loading flag to false, unsets the loadError and fills the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
      });
      const allVersions = ['list'];
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
        },
        currentVersionDetails,
      };

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionExplorerData/fulfilled',
        payload,
      });

      expect(newState.versionExplorerData.loading).toBe(false);
      expect(newState.versionExplorerData.loadError).toBe(null);
      expect(newState.versionExplorerData.versions).toBe(allVersions);
      expect(newState.versionExplorerData.remediation).toBe(remediation);
      expect(newState.versionExplorerData.currentVersionDetails).toBe(currentVersionDetails);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsOverview/loadVersionExplorerData/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        versionExplorerData: {
          loading: true,
          loadError: 'error',
          data: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadVersionExplorerData/rejected',
        payload: 'loadError',
      });

      expect(newState.versionExplorerData.loading).toBe(false);
      expect(newState.versionExplorerData.loadError).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('componentDetailsOverview/loadInnerSourceProducerData/pending action', () => {
    it('sets the loadError to null and the loading flag to true', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          loading: false,
          loadError: 'loadError',
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadInnerSourceProducerData/pending',
      });

      expect(newState.innerSourceProducerData.loading).toBe(true);
      expect(newState.innerSourceProducerData.loadError).toBeNull();
    });
  });

  describe('componentDetailsOverview/loadInnerSourceProducerData/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          loading: true,
          loadError: null,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadInnerSourceProducerData/rejected',
        payload: 'loadError',
      });

      expect(newState.innerSourceProducerData.loading).toBe(false);
      expect(newState.innerSourceProducerData.loadError).toBe('loadError');
    });
  });

  describe('componentDetailsOverview/loadInnerSourceProducerData/fulfilled action', () => {
    it('sets the loadError to the payload and the loading flag to true', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          loading: true,
          loadError: 'loadError',
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/loadInnerSourceProducerData/fulfilled',
      });

      expect(newState.innerSourceProducerData.loading).toBe(false);
      expect(newState.innerSourceProducerData.loadError).toBeNull();
    });
  });

  describe('componentDetailsOverview/toggleInnerSourceProducerReportModal action', () => {
    it('toggles showInnerSourceProducerReportModal', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          showInnerSourceProducerReportModal: true,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/toggleInnerSourceProducerReportModal',
      });

      expect(newState.innerSourceProducerData.showInnerSourceProducerReportModal).toBe(false);
    });
  });

  describe('componentDetailsOverview/toggleInnerSourcePermissionsModal action', () => {
    it('toggles showInnerSourcePermissionsModal', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          showInnerSourcePermissionsModal: true,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/toggleInnerSourcePermissionsModal',
      });

      expect(newState.innerSourceProducerData.showInnerSourcePermissionsModal).toBe(false);
    });
  });

  describe('componentDetailsOverview/setInsufficientPermission action', () => {
    it('sets setInsufficientPermission', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          insufficientPermission: true,
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/setInsufficientPermission',
        payload: true,
      });

      expect(newState.innerSourceProducerData.showInnerSourcePermissionsModal).toBe(false);
    });
  });

  describe('componentDetailsOverview/setInnerSourceProducerReportUrl action', () => {
    it('sets reportUrl', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          reportUrl: 'old url',
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/setInnerSourceProducerReportUrl',
        payload: 'new url',
      });

      expect(newState.innerSourceProducerData.reportUrl).toBe('new url');
    });
  });

  describe('componentDetailsOverview/setLatestInnerSourceComponentVersion action', () => {
    it('sets boolean value for setInsufficientPermission', () => {
      const state = Object.freeze({
        innerSourceProducerData: {
          ...initialInnerSourceProducerData,
          latestInnerSourceComponentVersion: '1.0',
        },
      });

      const newState = reducer(state, {
        type: 'componentDetailsOverview/setLatestInnerSourceComponentVersion',
        payload: '2.0',
      });

      expect(newState.innerSourceProducerData.latestInnerSourceComponentVersion).toBe('2.0');
    });
  });

  describe('componentDetailsOverview/toggleShowSimilarMatches', () => {
    it('toggles between boolean values for showSimilarMatches', () => {
      const state = Object.freeze({
        showSimilarMatchesPopover: true,
      });

      let newState = reducer(state, { type: 'componentDetailsOverview/toggleShowSimilarMatches' });
      expect(newState.showSimilarMatchesPopover).toBe(false);

      newState = reducer(newState, { type: 'componentDetailsOverview/toggleShowSimilarMatches' });
      expect(newState.showSimilarMatchesPopover).toBe(true);
    });
  });
});
