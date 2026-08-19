/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions } from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSlice';
import * as selectors from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import { getApplicableOrganizationCategories, getApplicationCategoriesUrl } from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

describe('assignApplicationCategoriesActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store;

  beforeEach(function () {
    jest.useFakeTimers();
    const state = {
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
  });

  afterEach(function () {
    jest.useRealTimers();
  });

  describe('loadApplicableCategories', () => {
    it('loads applicable categories successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableOrganizationCategories('alpine-test')]: Promise.resolve({
            data: [
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                organizationId: 'ROOT_ORGANIZATION_ID',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                color: 'dark-green',
              },
            ],
          }),
          [getApplicationCategoriesUrl('alpine-test')]: Promise.resolve({
            data: [
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                organizationId: 'ROOT_ORGANIZATION_ID',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                color: 'dark-green',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadApplicableCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/api/v2/applicationCategories/application/alpine-test/applicable');
        expect(axios.get).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test');

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/loadApplicableCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/fulfilled',
          'applicationCategories/assign/loadApplicableCategories/fulfilled',
        ]);

        expect(actions[3].payload).toEqual([
          {
            id: '13dfce231ca24289bec319fddf4bef88',
            organizationId: 'ROOT_ORGANIZATION_ID',
            name: 'Internal',
            description: 'Applications that are used only by your employees',
            color: 'dark-green',
          },
        ]);

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicableOrganizationCategories('alpine-test')]: () => Promise.reject('could not load tags'),
        },
      });

      store.dispatch(actions.loadApplicableCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test');
        expect(axios.get).toHaveBeenCalledWith('/api/v2/applicationCategories/application/alpine-test/applicable');

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/loadApplicableCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/rejected',
          'applicationCategories/assign/loadApplicableCategories/rejected',
        ]);
        expect(actions[3].payload).toBe('could not load tags');

        done();
      });
    });
  });

  describe('loadAppliedCategories', () => {
    it('loads applied tags successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationCategoriesUrl('alpine-test')]: Promise.resolve({
            data: [
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                organizationId: 'ROOT_ORGANIZATION_ID',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                color: 'dark-green',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.loadAppliedCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/loadAppliedCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/fulfilled',
        ]);

        expect(actions[1].payload).toEqual([
          {
            id: '13dfce231ca24289bec319fddf4bef88',
            organizationId: 'ROOT_ORGANIZATION_ID',
            name: 'Internal',
            description: 'Applications that are used only by your employees',
            color: 'dark-green',
          },
        ]);

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getApplicationCategoriesUrl('alpine-test')]: () => Promise.reject('could not load tags'),
        },
      });

      store.dispatch(actions.loadAppliedCategories()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/loadAppliedCategories/pending',
          'applicationCategories/assign/loadAppliedCategories/rejected',
        ]);
        expect(actions[1].payload).toBe('could not load tags');

        done();
      });
    });
  });

  describe('saveAppliedCategories', () => {
    it('saves applied tags successfully', (done) => {
      jest.spyOn(selectors, 'selectAppliedCategories').mockReturnValue([
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ]);
      mockAxiosCalls({
        put: {
          [getApplicationCategoriesUrl('alpine-test')]: Promise.resolve({
            data: [
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                organizationId: 'ROOT_ORGANIZATION_ID',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                color: 'dark-green',
              },
            ],
          }),
        },
        get: {
          [getApplicationCategoriesUrl('alpine-test')]: Promise.resolve({
            data: [
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                organizationId: 'ROOT_ORGANIZATION_ID',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                color: 'dark-green',
              },
            ],
          }),
        },
      });

      store.dispatch(actions.saveAppliedCategories()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test', [
          {
            id: '13dfce231ca24289bec319fddf4bef88',
            organizationId: 'ROOT_ORGANIZATION_ID',
            name: 'Internal',
            description: 'Applications that are used only by your employees',
            color: 'dark-green',
          },
        ]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/saveAppliedCategories/pending',
          'applicationCategories/assign/saveAppliedCategories/fulfilled',
        ]);

        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/saveAppliedCategories/pending',
          'applicationCategories/assign/saveAppliedCategories/fulfilled',
          'applicationCategories/assign/saveMaskTimerDone',
        ]);

        done();
      });
    });

    it('dispatches rejected action if save request fails', (done) => {
      jest.spyOn(selectors, 'selectAppliedCategories').mockReturnValue([
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          organizationId: 'ROOT_ORGANIZATION_ID',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          color: 'dark-green',
        },
      ]);
      mockAxiosCalls({
        put: {
          [getApplicationCategoriesUrl('alpine-test')]: () => Promise.reject('could not save tags'),
        },
      });

      store.dispatch(actions.saveAppliedCategories()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/appliedTag/application/alpine-test', [
          {
            id: '13dfce231ca24289bec319fddf4bef88',
            organizationId: 'ROOT_ORGANIZATION_ID',
            name: 'Internal',
            description: 'Applications that are used only by your employees',
            color: 'dark-green',
          },
        ]);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/saveAppliedCategories/pending',
          'applicationCategories/assign/saveAppliedCategories/rejected',
        ]);
        expect(actions[1].payload).toBe('could not save tags');

        done();
      });
    });
  });

  describe('goToEditCategories', () => {
    beforeEach(() => {
      jest.spyOn(routerSelectors, 'selectRouterSlice').mockReturnValue({
        currentState: {
          name: 'application.somewhere',
        },
        currentParams: {
          applicationPublicId: 'applicationPublicId',
        },
      });
    });

    it('redirects to proper edit category path', (done) => {
      store.dispatch(actions.goToEditCategories()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'applicationCategories/assign/goToEditCategories/pending',
          '@@reduxUiRouter/stateGo',
          'applicationCategories/assign/goToEditCategories/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({
          to: 'management.edit.application.category',
          params: {
            applicationPublicId: 'applicationPublicId',
          },
          options: undefined,
        });

        done();
      });
    });
  });
});
