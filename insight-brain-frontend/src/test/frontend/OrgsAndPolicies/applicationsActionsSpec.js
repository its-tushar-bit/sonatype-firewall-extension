/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';
import * as applicationsSelectors from 'MainRoot/OrgsAndPolicies/applicationsSelectors';
import { getApplicationsUrl } from 'MainRoot/util/CLMLocation';

describe('orgsAndPoliciesApplicationsActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state, applicationsSelectorsSpy;

  beforeEach(() => {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'alpine-test',
        },
        currentState: {
          name: 'application',
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    applicationsSelectorsSpy = spyOn(applicationsSelectors, 'selectApplications').and.returnValue([]);
  });

  describe('loadApplications', () => {
    it('loads applications successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationsUrl()]: Promise.resolve({
            data: [
              {
                contact: null,
                id: '430b39e52a2e4ca48d708913f0f4b10d',
                name: 'alpine test',
                organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
                organizationName: 'wencel org',
                publicId: 'alpine-test',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadApplications()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/application');

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'applications/loadApplications/pending',
          'applications/setOwnerName',
          'applications/loadApplications/fulfilled',
        ]);

        expect(actions[1].payload).toBe('alpine test');
        expect(actions[2].payload).toEqual([
          {
            contact: null,
            id: '430b39e52a2e4ca48d708913f0f4b10d',
            name: 'alpine test',
            organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
            organizationName: 'wencel org',
            publicId: 'alpine-test',
          },
        ]);

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationsUrl()]: () => Promise.reject('could not load applications'),
        },
      });

      store.dispatch(actions.loadApplications()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/application');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applications/loadApplications/pending',
          'applications/loadApplications/rejected',
        ]);
        expect(actions[1].payload).toBe('could not load applications');

        done();
      });
    });
  });

  describe('loadApplicationsIfNeeded', () => {
    it('loads applications successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationsUrl()]: Promise.resolve({
            data: [
              {
                contact: null,
                id: '430b39e52a2e4ca48d708913f0f4b10d',
                name: 'alpine test',
                organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
                organizationName: 'wencel org',
                publicId: 'alpine-test',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadApplicationsIfNeeded()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/application');

        const actions = store.getActions();

        expect(actions.length).toBe(5);
        expect(actions).toHaveActionTypesInOrder([
          'applications/loadApplicationsIfNeeded/pending',
          'applications/loadApplications/pending',
          'applications/setOwnerName',
          'applications/loadApplications/fulfilled',
          'applications/loadApplicationsIfNeeded/fulfilled',
        ]);

        expect(actions[2].payload).toBe('alpine test');

        done();
      });
    });

    it('uses cached applications', (done) => {
      applicationsSelectorsSpy.and.returnValue([
        {
          contact: null,
          id: '430b39e52a2e4ca48d708913f0f4b10d',
          name: 'alpine test',
          organizationId: '5b862dfe2c95486f8395eca90c06dcfe',
          organizationName: 'wencel org',
          publicId: 'alpine-test',
        },
      ]);

      mockAxiosCalls({ get: {} });

      store.dispatch(actions.loadApplicationsIfNeeded()).then(() => {
        expect(axios.get).not.toHaveBeenCalled();

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'applications/loadApplicationsIfNeeded/pending',
          'applications/setOwnerName',
          'applications/loadApplicationsIfNeeded/fulfilled',
        ]);

        expect(actions[1].payload).toBe('alpine test');

        done();
      });
    });
  });
});
