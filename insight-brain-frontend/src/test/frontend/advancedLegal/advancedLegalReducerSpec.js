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
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED
} from '../../../main/frontend/advancedLegal/advancedLegalActions.js';
import { pick } from 'ramda';

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
        foo: 'bar'
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
});
