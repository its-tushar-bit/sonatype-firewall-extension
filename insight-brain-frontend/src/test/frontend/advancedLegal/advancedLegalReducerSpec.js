/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/advancedLegal/advancedLegalReducer.js';
import {
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
  ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED,
  ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
  ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
  ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE
} from '../../../main/frontend/advancedLegal/advancedLegalActions.js';
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

      expect(newState.viewStateApplications.loading).toBeFalsy();
      expect(newState.viewStateApplications.error).toBeNull();
      expect(newState.applications.length).toBe(0);

      expect(newState.viewStateApplicationReport.loading).toBeFalsy();
      expect(newState.viewStateApplicationReport.error).toBeNull();
      expect(newState.applicationReport).toBeNull();
      expect(newState.component.loading).toBeTruthy();

      expect(newState.availableScopes.loading).toBeFalsy();
      expect(newState.availableScopes.error).toBeNull();
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED action', function () {
    it('sets in viewStateApplications loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED
      });

      const { component } = newState;
      expect(component.loading).toBeTruthy();
      expect(component.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED action', function () {
    it('sets in viewStateApplications loading to false, applications to payload and error to null', function () {
      const state = {
        component: {
          loading: true,
          error: null
        }
      };
      const componentInfo = {
        foo: 'bar',
        obligations: []
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo
      });

      const { component } = newState;
      expect(component.loading).toBeFalsy();
      expect(component.error).toBeUndefined();
      expect(component.foo).toBe('bar');
    });

    it('sets the obligation attribution data for text based obligations without attributions', function () {
      const state = {
        component: {
          loading: true,
          error: null
        }
      };
      const componentInfo = {
        foo: 'bar',
        obligations: []
      };
      TEXT_BASED_OBLIGATIONS.forEach(element => {
        componentInfo.obligations.push({ name: element, status: 'status', attributions: [] });
      });
      componentInfo.obligations.push({ name: 'other', status: 'status', attributions: [] });
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo
      });

      const { component } = newState;
      component.obligations.forEach(obligation => {
        expect(obligation.originalStatus).toBe(obligation.status);
        if (TEXT_BASED_OBLIGATIONS.indexOf(obligation.name) >= 0) {
          expect(obligation.attributions.length).toBeGreaterThan(0);
          const attribution = obligation.attributions[0];
          expect(attribution.id).toBeNull();
          expect(attribution.content).toBe('');
          expect(attribution.ownerId).toBe('ROOT_ORGANIZATION_ID');
          expect(attribution.originalContent).toBe(attribution.content);
          expect(attribution.originalOwnerId).toBe(attribution.ownerId);
          expect(attribution.showAttributionModal).toBeFalsy();
          expect(attribution.error).toBeNull();
          expect(attribution.saveAttributionSubmitMask).toBeNull();
        }
        else {
          expect(obligation.attributions.length).toBe(0);
        }
      });
    });

    it('sets the obligation attribution data for text based obligations with attributions', function () {
      const state = {
        component: {
          loading: true,
          error: null
        }
      };
      const componentInfo = {
        foo: 'bar',
        obligations: []
      };
      TEXT_BASED_OBLIGATIONS.forEach(element => {
        componentInfo.obligations.push(
            { name: element, status: 'status', attributions: [{ id: 'id', content: 'content', ownerId: 'ownerId' }] });
      });
      componentInfo.obligations.push({ name: 'other', status: 'status', attributions: [] });
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
        payload: componentInfo
      });

      const { component } = newState;
      component.obligations.forEach(obligation => {
        expect(obligation.originalStatus).toBe(obligation.status);
        if (TEXT_BASED_OBLIGATIONS.indexOf(obligation.name) >= 0) {
          expect(obligation.attributions.length).toBeGreaterThan(0);
          const attribution = obligation.attributions[0];
          expect(attribution.id).toBe('id');
          expect(attribution.content).toBe('content');
          expect(attribution.ownerId).toBe('ownerId');
          expect(attribution.originalContent).toBe(attribution.content);
          expect(attribution.originalOwnerId).toBe(attribution.ownerId);
          expect(attribution.showAttributionModal).toBeFalsy();
          expect(attribution.error).toBeNull();
          expect(attribution.saveAttributionSubmitMask).toBeNull();
        }
        else {
          expect(obligation.attributions.length).toBe(0);
        }
      });
    });
  });

  describe('ADVANCED_LEGAL_LOAD_COMPONENT_FAILED action', function () {
    it('sets in viewStateApplications loading to false and error to payload', function () {
      const state = {
        component: {
          loading: true,
          error: null
        }
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
        payload: errorTest
      });

      const { component } = newState;
      expect(component.loading).toBeFalsy();
      expect(component.error).toBe(errorTest);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED action', function () {
    it('sets in viewStateApplications loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED
      });

      const { viewStateApplications } = newState;
      expect(viewStateApplications.loading).toBeTruthy();
      expect(viewStateApplications.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED action', function () {
    it('sets in viewStateApplications loading to false, applications to payload and error to null', function () {
      const state = {
        viewStateApplications: {
          loading: true,
          error: null
        },
        applications: []
      };
      const applications = [{
        publicId: 'a'
      },
      {
        publicId: 'b'
      }];
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
        payload: applications
      });

      const { viewStateApplications } = newState;
      expect(viewStateApplications.loading).toBeFalsy();
      expect(viewStateApplications.error).toBeNull();
      expect(newState.applications).toBe(applications);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED action', function () {
    it('sets in viewStateApplications loading to false and error to payload', function () {
      const state = {
        viewStateApplications: {
          loading: true,
          error: null
        }
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
        payload: errorTest
      });

      const { viewStateApplications } = newState;
      expect(viewStateApplications.loading).toBeFalsy();
      expect(viewStateApplications.error).toBe(errorTest);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED action', function () {
    it('sets in viewStateApplicationReport loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED
      });

      const { viewStateApplicationReport } = newState;
      expect(viewStateApplicationReport.loading).toBeTruthy();
      expect(viewStateApplicationReport.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED action', function () {
    it('sets viewStateApplicationReport loading to false, applicationReport to payload, error to null', function () {
      const state = {
        viewStateApplicationReport: {
          loading: true,
          error: null
        },
        applicationReport: {}
      };
      const applicationReport = {
        components: [{ displayName: 'groupId : artifactId : version' }],
        licenseLegalMetadata: [{ licenseId: 'License Test' }]
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
        payload: applicationReport
      });

      const { viewStateApplicationReport } = newState;
      expect(viewStateApplicationReport.loading).toBeFalsy();
      expect(viewStateApplicationReport.error).toBeNull();
      expect(newState.applicationReport).toBe(applicationReport);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED action', function () {
    it('sets in viewStateApplicationReport loading to false and error to payload', function () {
      const state = {
        viewStateApplicationReport: {
          loading: true,
          error: null
        }
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
        payload: errorTest
      });

      const { viewStateApplicationReport } = newState;
      expect(viewStateApplicationReport.loading).toBeFalsy();
      expect(viewStateApplicationReport.error).toBe(errorTest);
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
    it('sets in availableScopes loading to true and error to null', function () {
      const newState = reduce(undefined, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeTruthy();
      expect(availableScopes.error).toBeNull();
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED action', function () {
    it('sets availableScopes loading to false, error to null, and merges the payload with availableScopes', function() {
      const state = {
        availableScopes: {
          loading: true,
          error: null
        }
      };
      const applicableContext = {
        id: 'ROOT_ORGANIZATION_ID',
        name: 'Root Organization',
        type: 'organization',
        children: []
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
        payload: applicableContext
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeFalsy();
      expect(availableScopes.error).toBeNull();
      expect(newState.availableScopes).toEqual(
          { ...pick(['loading', 'error'], newState.availableScopes), ...applicableContext });
    });
  });

  describe('ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED action', function () {
    it('sets in availableScopes loading to false and error to payload', function () {
      const state = {
        availableScopes: {
          loading: true,
          error: null
        }
      };
      const errorTest = 'Error test';
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED,
        payload: errorTest
      });

      const { availableScopes } = newState;
      expect(availableScopes.loading).toBeFalsy();
      expect(availableScopes.error).toBe(errorTest);
    });
  });

  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT action', function () {
    it('sets the content of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_TEXT,
        payload: { name: 'obligation1', value: 'content' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.content).toBe('content');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED action', function () {
    it('sets the status of the matching obligation to fulfilled if the value is true', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', status: 'OPEN' }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED,
        payload: { name: 'obligation1', value: true }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.status).toBe('FULFILLED');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });

    it('sets the status of the matching obligation to its original status if the value is false', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', status: 'FULFILLED', originalStatus: 'OPEN' }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_OBLIGATION_FULFILLED,
        payload: { name: 'obligation1', value: false }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.status).toBe('OPEN');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE action', function () {
    it('sets the ownerId of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_ATTRIBUTION_SCOPE,
        payload: { name: 'obligation1', value: 'ownerId' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.ownerId).toBe('ownerId');
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL action', function () {
    it('sets showAttributionModal of the first attribution of the matching obligation', function () {
      const state = {
        component: {
          obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SET_SHOW_ATTRIBUTION_MODAL,
        payload: { name: 'obligation1', value: true }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.showAttributionModal).toBeTruthy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED action', function() {
    it('sets error and saveAttributionSubmitMask to null of the first attribution of the matching obligation',
        function() {
          const state = {
            component: {
              obligations: [{ name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }]
            }
          };
          const newState = reduce(state, {
            type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_REQUESTED,
            payload: { name: 'obligation1' }
          });
          const obligation1Attribution = newState.component.obligations[0].attributions[0];
          expect(obligation1Attribution.error).toBeNull();
          expect(obligation1Attribution.saveAttributionSubmitMask).toBeNull();
          expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
        });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED action', function() {
    it('sets the matching obligation and its first attribution to the payload', function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', status: 'OPEN', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FULFILLED,
        payload: { name: 'obligation1', value: { id: 'id', content: 'content', ownerId: 'ownerId' } }
      });
      const obligation1 = newState.component.obligations[0];
      expect(obligation1.originalStatus).toBe('OPEN');
      expect(obligation1.status).toBe('OPEN');
      const obligation1Attribution = obligation1.attributions[0];
      expect(obligation1Attribution.id).toBe('id');
      expect(obligation1Attribution.originalContent).toBe('content');
      expect(obligation1Attribution.content).toBe('content');
      expect(obligation1Attribution.originalOwnerId).toBe('ownerId');
      expect(obligation1Attribution.ownerId).toBe('ownerId');
      expect(obligation1Attribution.error).toBeNull();
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeTruthy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED action', function() {
    it('sets error to payload and saveAttributionSubmitMask to false of the first attribution of the matching' +
        'obligation',
    function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_FAILED,
        payload: { name: 'obligation1', value: 'error' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.error).toBe('error');
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeNull();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });

  describe('ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE action', function() {
    it('sets saveAttributionSubmitMask to null and showAttributionModal to false of the first attribution of the' +
        'matching obligation',
    function() {
      const state = {
        component: {
          obligations: [
            { name: 'obligation1', attributions: [{}] }, { name: 'obligation2' }
          ]
        }
      };
      const newState = reduce(state, {
        type: ADVANCED_LEGAL_SAVE_ATTRIBUTION_SUBMIT_MASK_DONE,
        payload: { name: 'obligation1' }
      });
      const obligation1Attribution = newState.component.obligations[0].attributions[0];
      expect(obligation1Attribution.saveAttributionSubmitMask).toBeNull();
      expect(obligation1Attribution.showAttributionModal).toBeFalsy();
      expect(newState.component.obligations[1]).toEqual({ name: 'obligation2' });
    });
  });
});
