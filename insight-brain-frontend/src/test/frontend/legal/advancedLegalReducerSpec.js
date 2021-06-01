/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/legal/advancedLegalReducer.js';
import {
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
} from '../../../main/frontend/legal/advancedLegalActions.js';
import { pick } from 'ramda';
import { TEXT_BASED_OBLIGATIONS } from '../../../main/frontend/legal/advancedLegalConstants';

describe('advancedLegalReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);

      expect(newState.component.loading).toBeFalsy();
      expect(newState.component.error).toBeNull();

      expect(newState.availableScopes.loading).toBeFalsy();
      expect(newState.availableScopes.error).toBeNull();
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED action', function () {
    it('sets component loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
      });

      const { component } = newState;
      expect(component.loading).toBeTruthy();
      expect(component.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED action', function () {
    it('sets component loading to false, applications to payload and error to null', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        foo: 'bar',
        component: {
          licenseLegalData: {
            noticeFiles: [],
            licenseFiles: [],
            obligations: [],
            attributions: [],
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });

      const { component } = newState;
      expect(component.loading).toBeFalsy();
      expect(component.error).toBeNull();
      expect(component.foo).toBe('bar');
    });

    it('sorts the obligations', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        component: {
          licenseLegalData: {
            noticeFiles: [],
            licenseFiles: [],
            obligations: [
              { name: 'Must Give Credit' },
              { name: 'Must State Changes' },
              { name: 'z' },
              { name: 'Inclusion of Install Instructions' },
              { name: 'Inclusion of Notice' },
              { name: 'a' },
              { name: 'Inclusion of License' },
              { name: 'Inclusion of Copyright' },
            ],
            attributions: [],
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });
      expect(newState.component.component.licenseLegalData.obligations.map((o) => pick(['name'], o))).toEqual([
        { name: 'Inclusion of Copyright' },
        { name: 'Inclusion of Notice' },
        { name: 'Inclusion of License' },
        { name: 'Inclusion of Install Instructions' },
        { name: 'Must Give Credit' },
        { name: 'Must State Changes' },
        { name: 'a' },
        { name: 'z' },
      ]);
    });

    it('sets the attribution data for additional or text based obligations without attributions', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        foo: 'bar',
        component: {
          licenseLegalData: {
            noticeFiles: [],
            licenseFiles: [],
            obligations: [],
            attributions: [],
          },
        },
      };
      TEXT_BASED_OBLIGATIONS.forEach((element) => {
        componentInfo.component.licenseLegalData.obligations.push({
          name: element,
          status: 'status',
        });
      });
      componentInfo.component.licenseLegalData.obligations.push({
        name: 'other',
        status: 'status',
      });
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });

      const { component } = newState;
      component.component.licenseLegalData.obligations.forEach((obligation) => {
        expect(obligation.originalStatus).toBe(obligation.status);
      });

      expect(component.component.licenseLegalData.attributions.length).toBe(TEXT_BASED_OBLIGATIONS.length + 1);
      component.component.licenseLegalData.attributions.forEach((attribution) => {
        if (attribution.obligationName !== null) {
          expect(TEXT_BASED_OBLIGATIONS).toContain(attribution.obligationName);
        }
        expect(attribution.obligationName).not.toBe('other');
        expect(attribution.id).toBeNull();
        expect(attribution.content).toBe('');
        expect(attribution.ownerId).toBe('ROOT_ORGANIZATION_ID');
        expect(attribution.originalContent).toBe(attribution.content);
        expect(attribution.originalOwnerId).toBe(attribution.ownerId);
        expect(attribution.showAttributionModal).toBeFalsy();
        expect(attribution.error).toBeNull();
        expect(attribution.saveAttributionSubmitMask).toBeNull();
      });
    });

    it('sets the obligation attribution data for additional or text based obligations with attributions', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        foo: 'bar',
        component: {
          licenseLegalData: {
            noticeFiles: [],
            licenseFiles: [],
            obligations: [],
            attributions: [],
          },
        },
      };
      TEXT_BASED_OBLIGATIONS.forEach((element) => {
        componentInfo.component.licenseLegalData.obligations.push({
          name: element,
          status: 'status',
        });
        componentInfo.component.licenseLegalData.attributions.push({
          id: 'id',
          obligationName: element,
          content: 'content',
          ownerId: 'ownerId',
        });
      });
      componentInfo.component.licenseLegalData.attributions.push({
        id: 'id',
        obligationName: null,
        content: 'content',
        ownerId: 'ownerId',
      });
      componentInfo.component.licenseLegalData.obligations.push({
        name: 'other',
        status: 'status',
      });
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });

      const { component } = newState;
      component.component.licenseLegalData.obligations.forEach((obligation) => {
        expect(obligation.originalStatus).toBe(obligation.status);
      });

      component.component.licenseLegalData.attributions.forEach((attribution) => {
        if (attribution.obligationName !== null) {
          expect(TEXT_BASED_OBLIGATIONS).toContain(attribution.obligationName);
        }
        expect(attribution.obligationName).not.toBe('other');
        expect(attribution.id).toBe('id');
        expect(attribution.content).toBe('content');
        expect(attribution.ownerId).toBe('ownerId');
        expect(attribution.originalContent).toBe(attribution.content);
        expect(attribution.originalOwnerId).toBe(attribution.ownerId);
        expect(attribution.showAttributionModal).toBeFalsy();
        expect(attribution.error).toBeNull();
        expect(attribution.saveAttributionSubmitMask).toBeNull();
      });
    });

    it('sets the notices and licenses view data', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        foo: 'bar',
        component: {
          licenseLegalData: {
            componentNoticesScopeOwnerId: 'appId',
            noticeFiles: [
              { content: 'content1', status: 'enabled' },
              { content: '', status: 'disabled' },
            ],
            componentLicensesScopeOwnerId: 'appId',
            licenseFiles: [
              { content: 'content2', status: 'enabled' },
              { content: '', status: 'disabled' },
            ],
            obligations: [],
            attributions: [],
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });

      expect(newState.component.component.licenseLegalData).toEqual({
        showNoticesModal: false,
        componentNoticesScopeOwnerId: 'appId',
        originalComponentNoticesScopeOwnerId: 'appId',
        noticeFiles: [
          {
            originalContent: 'content1',
            content: 'content1',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            originalContent: '',
            content: '',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
        ],
        obligations: [],
        attributions: [
          {
            id: null,
            obligationName: null,
            content: '',
            originalContent: '',
            ownerId: 'ROOT_ORGANIZATION_ID',
            originalOwnerId: 'ROOT_ORGANIZATION_ID',
            showAttributionModal: false,
            error: null,
            saveAttributionSubmitMask: null,
          },
        ],
        noticesError: null,
        saveNoticesSubmitMask: null,
        showLicenseFilesModal: false,
        componentLicensesScopeOwnerId: 'appId',
        originalComponentLicensesScopeOwnerId: 'appId',
        licenseFiles: [
          {
            originalContent: 'content2',
            content: 'content2',
            originalStatus: 'enabled',
            status: 'enabled',
            isPristine: true,
          },
          {
            originalContent: '',
            content: '',
            originalStatus: 'disabled',
            status: 'disabled',
            isPristine: true,
          },
        ],
        licensesError: null,
        saveLicenseFilesSubmitMask: null,
        showAllObligationsModal: false,
        saveAllObligationsSubmitMask: null,
        saveAllObligationsError: null,
      });
    });

    it('sets the default legal file scope owner id if there is none', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const componentInfo = {
        foo: 'bar',
        component: {
          licenseLegalData: {
            componentNoticesScopeOwnerId: null,
            componentLicensesScopeOwnerId: null,
            noticeFiles: [],
            licenseFiles: [],
            obligations: [],
            attributions: [],
          },
        },
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo,
      });

      expect(newState.component.component.licenseLegalData.originalComponentNoticesScopeOwnerId).toBe(
        'ROOT_ORGANIZATION_ID'
      );
      expect(newState.component.component.licenseLegalData.componentNoticesScopeOwnerId).toBe('ROOT_ORGANIZATION_ID');
      expect(newState.component.component.licenseLegalData.originalComponentLicensesScopeOwnerId).toBe(
        'ROOT_ORGANIZATION_ID'
      );
      expect(newState.component.component.licenseLegalData.componentLicensesScopeOwnerId).toBe('ROOT_ORGANIZATION_ID');
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_FAILED action', function () {
    it('sets component loading to false and error to payload', function () {
      const state = {
        component: {
          loading: true,
          error: null,
        },
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
        payload: errorTest,
      });

      const { component } = newState;
      expect(component.loading).toBeFalsy();
      expect(component.error).toBe(errorTest);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
    it('sets in availableScopes loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeTruthy();
      expect(availableScopes.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED action', function () {
    it('sets availableScopes loading to false, error to null, and merges the payload with availableScopes', function () {
      const state = {
        availableScopes: {
          loading: true,
          error: null,
        },
      };
      const applicableContext = {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
        children: [],
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
        payload: applicableContext,
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeFalsy();
      expect(availableScopes.error).toBeNull();
      expect(newState.availableScopes).toEqual({
        ...pick(['loading', 'error'], newState.availableScopes),
        ...applicableContext,
      });
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED action', function () {
    it('sets in availableScopes loading to false and error to payload', function () {
      const state = {
        availableScopes: {
          loading: true,
          error: null,
        },
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
        payload: errorTest,
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeFalsy();
      expect(availableScopes.error).toBe(errorTest);
    });
  });

  describe('COPYRIGHT_OVERRIDE_SAVE_FULFILLED action', function () {
    it('sets componentCopyrightId, componentCopyrightScopeOwnerId, and updated list of copyrights', function () {
      const state = {
        component: {
          component: {
            licenseLegalData: {
              componentCopyrightId: undefined,
              componentCopyrightScopeOwnerId: undefined,
              copyrights: [
                {
                  id: '1',
                  content: 'Copyright 2043',
                  originalContentHash: 'originalContentHash1',
                  status: 'enabled',
                },
              ],
            },
          },
        },
      };

      const copyrightOverrides = [
        {
          id: '1',
          content: 'Copyright 2043',
          originalContentHash: 'originalContentHash1',
          status: 'disabled',
        },
        {
          id: '2',
          content: 'Copyright 2020',
          originalContentHash: 'originalContentHash2',
          status: 'enabled',
        },
      ];

      const newState = reduce(state, {
        type: 'COPYRIGHT_OVERRIDE_SAVE_FULFILLED',
        payload: {
          id: 'componentCopyrightId',
          copyrightOverrides,
          componentCopyrightScopeOwnerId: 'owner',
        },
      });

      expect(newState.component.component.licenseLegalData.componentCopyrightId).toBe('componentCopyrightId');
      expect(newState.component.component.licenseLegalData.componentCopyrightScopeOwnerId).toBe('owner');
      expect(newState.component.component.licenseLegalData.copyrights).toBe(copyrightOverrides);
    });
  });
});
