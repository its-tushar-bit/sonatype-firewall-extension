/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import reducer, { initialState } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

const sbomMetadataInitialState = {
  author: [],
  manufacturer: [],
  supplier: [],
  person: [],
  organization: [],
  specification: null,
  specVersion: null,
  fileFormat: null,
  createdAt: null,
};

describe('billOfMaterialsPage reducers have the correct state when the following reducer is dispatched', function () {
  it('billOfMaterialsPage/setPublicAppId', () => {
    const state = {
      publicAppId: null,
    };

    const payload = 'app_123';

    const newState = reducer(state, {
      type: 'billOfMaterialsPage/setPublicAppId',
      payload: payload,
    });

    expect(newState.publicAppId).toBe('app_123');
  });

  describe('billOfMaterialsPage/loadSbomTableData', function () {
    it('/pending', () => {
      const state = {
        results: null,
        errorInternalAppId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/pending',
      });

      expect(newState.results).toBe(null);
      expect(newState.loadingInternalAppId).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loadingInternalAppId: false,
        errorInternalAppId: null,
        applicationName: null,
        internalAppId: null,
        publicAppId: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/rejected',
        payload: payload,
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe('payload error');
      expect(newState.applicationName).toBe(null);
      expect(newState.internalAppId).toBe(null);
      expect(newState.publicAppId).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingInternalAppId: false,
        errorInternalAppId: null,
        internalAppId: null,
        applicationName: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalAppId/fulfilled',
        payload: { id: 'abc123', name: 'Alice' },
      });

      expect(newState.loadingInternalAppId).toBe(false);
      expect(newState.errorInternalAppId).toBe(null);
      expect(newState.internalAppId).toBe('abc123');
      expect(newState.applicationName).toBe('Alice');
    });
  });

  describe('billOfMaterialsPage/loadApplicationSbomVersions', function () {
    it('/pending', () => {
      const state = {
        loadingSbomVersions: true,
        errorSbomVersions: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/pending',
      });

      expect(newState.loadingSbomVersions).toBe(true);
      expect(newState.errorSbomVersions).toBe(null);
    });

    it('/rejected', () => {
      const state = {
        loadingSbomVersions: false,
        errorSbomVersions: null,
        sbomVersions: null,
      };

      const payload = {
        response: {
          data: 'Error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/rejected',
        payload: payload,
      });

      expect(newState.loadingSbomVersions).toBe(false);
      expect(newState.errorSbomVersions).toBe(payload);
      expect(newState.sbomVersions).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingSbomVersions: false,
        errorSbomVersions: null,
        sbomVersions: null,
      };

      const payload = ['1.0-SNAPSHOT', '1.1-SNAPSHOT', '1.2-SNAPSHOT'];

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadApplicationSbomVersions/fulfilled',
        payload: payload,
      });

      expect(newState.loadingSbomVersions).toBe(false);
      expect(newState.errorSbomVersions).toBe(null);
      expect(newState.sbomVersions).toBe(payload);
    });
  });

  describe('billOfMaterialsPage/loadSbomMetadata', function () {
    it('/pending', () => {
      const state = {
        loadingSbomMetadata: true,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/pending',
      });

      expect(newState.loadingSbomMetadata).toBe(true);
      expect(newState.errorSbomMetadata).toBe(null);
      expect(newState.sbomMetadata).toEqual(sbomMetadataInitialState);
      expect(newState.scanId).toBe(null);
    });

    it('/rejected', () => {
      const state = {
        loadingSbomMetadata: false,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const payload = {
        response: {
          data: 'Error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/rejected',
        payload: payload,
      });

      expect(newState.loadingSbomMetadata).toBe(false);
      expect(newState.errorSbomMetadata).toBe(payload);
      expect(newState.sbomMetadata).toEqual(sbomMetadataInitialState);
      expect(newState.scanId).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loadingSbomMetadata: false,
        errorSbomMetadata: null,
        sbomMetadata: { ...sbomMetadataInitialState },
        scanId: null,
      };

      const payload = {
        author: ['Alice', 'Bob'],
        manufacturer: ['Orange'],
        supplier: ['Apple'],
        person: ['John', 'Jane'],
        organization: ['Sonatype'],
        specification: 'SPDX',
        specVersion: '2.3',
        fileFormat: 'json',
        createdAt: '2024-01-12T20:11:22Z',
        scanId: 'scan-id',
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadSbomMetadata/fulfilled',
        payload: payload,
      });

      expect(newState.loadingSbomMetadata).toBe(false);
      expect(newState.errorSbomMetadata).toBe(null);
      expect(newState.sbomMetadata).toEqual(omit(['scanId'], payload));
      expect(newState.scanId).toBe('scan-id');
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loadingInternalAppId: true,
        errorInternalAppId: 'some error',
        publicAppId: 'test-app-public',
        internalAppId: 'test-app-internal',
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
