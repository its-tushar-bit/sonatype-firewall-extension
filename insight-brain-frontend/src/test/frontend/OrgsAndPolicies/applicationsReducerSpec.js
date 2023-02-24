/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/applicationsSlice';

describe('applications reducer', () => {
  const otherState = Object.freeze({
    data: 'data',
  });

  describe('applications/loadApplications/pending', () => {
    it('sets loadingApplications, resets loadApplicationsError', () => {
      const state = Object.freeze({
        otherState,
        loadingApplications: false,
        loadApplicationsError: 'error',
      });

      const { loadingApplications, loadApplicationsError, otherState: expectedOtherState } = reducer(state, {
        type: 'applications/loadApplications/pending',
      });

      expect(loadingApplications).toBeTrue();
      expect(loadApplicationsError).toBeNull();
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applications/loadApplications/fulfilled', () => {
    it('resets loadingApplications, sets applications', () => {
      const state = Object.freeze({
        otherState,
        loadingApplications: true,
        applications: [],
      });
      const expectedApplications = [
        {
          contact: null,
          id: '430b39e52a2e4ca48d708913f0f4b10d',
          name: 'alpine test',
          organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
          organizationName: 'wencel org',
          publicId: 'alpine-test',
        },
      ];

      const { loadingApplications, applications, otherState: expectedOtherState } = reducer(state, {
        type: 'applications/loadApplications/fulfilled',
        payload: expectedApplications,
      });

      expect(loadingApplications).toBeFalse();
      expect(applications).toEqual(expectedApplications);
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applications/loadApplications/rejected', () => {
    it('sets loadApplicationsError,  resets loadingApplications', () => {
      const state = Object.freeze({ otherState, loadingApplications: true, loadApplicationsError: null });

      const { loadingApplications, loadApplicationsError, otherState: expectedOtherState } = reducer(state, {
        type: 'applications/loadApplications/rejected',
        payload: 'error',
      });

      expect(loadingApplications).toBeFalse();
      expect(loadApplicationsError).toBe('error');
      expect(expectedOtherState).toEqual(otherState);
    });
  });

  describe('applications/updateApplication', () => {
    it('sets newly created application', () => {
      const newApplication = {
        id: 'newApplication',
      };
      const state = Object.freeze({
        applications: [],
      });

      const newState = reducer(state, {
        type: 'applications/updateApplication',
        payload: { application: newApplication, isNew: true },
      });

      expect(newState.applications).toEqual([newApplication]);
    });

    it('updates edited application', () => {
      const editedApplication = {
        id: 'newApplication',
        name: 'newName',
      };
      const state = Object.freeze({
        applications: [
          {
            id: 'newApplication',
            name: 'oldName',
          },
        ],
      });

      const newState = reducer(state, {
        type: 'applications/updateApplication',
        payload: { application: editedApplication, isNew: false },
      });

      expect(newState.applications).toEqual([editedApplication]);
    });
  });

  describe('applications/removeApplicationFromList', () => {
    it('removes application from the list by id', () => {
      const state = Object.freeze({
        applications: [
          {
            id: '430b39e52a2e4ca48d708913f0f4b10d',
            name: 'alpine test',
          },
        ],
      });
      const newState = reducer(state, {
        type: 'applications/removeApplicationFromList',
        payload: '430b39e52a2e4ca48d708913f0f4b10d',
      });
      expect(newState.applications).toEqual([]);
    });
  });

  describe('applications/removeApplicationsFromList', () => {
    it('removes a list of applications from the list', () => {
      const state = Object.freeze({
        applications: [
          {
            publicId: 'application-one',
            name: 'Application one',
          },
          {
            publicId: 'application-two',
            name: 'Application two',
          },
          {
            publicId: 'application-three',
            name: 'Application three',
          },
          {
            publicId: 'application-four',
            name: 'Application four',
          },
        ],
      });
      const newState = reducer(state, {
        type: 'applications/removeApplicationsFromList',
        payload: ['application-two', 'application-three'],
      });
      expect(newState.applications).toEqual([
        {
          publicId: 'application-one',
          name: 'Application one',
        },
        {
          publicId: 'application-four',
          name: 'Application four',
        },
      ]);
    });
  });
});
