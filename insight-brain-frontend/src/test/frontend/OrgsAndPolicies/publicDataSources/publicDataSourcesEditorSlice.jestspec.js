/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions, initialState } from 'MainRoot/OrgsAndPolicies/publicDataSources/publicDataSourcesSlice';
import * as orgsAndPoliciesSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import * as CLMLocation from 'MainRoot/util/CLMLocation';
import axios from 'axios';
import configureStore from 'redux-mock-store';
import thunk from 'redux-thunk';

jest.mock('axios');

const middlewares = [thunk];
const mockStore = configureStore(middlewares);

describe('publicDataSourcesSlice', () => {
  beforeEach(() => {
    jest
      .spyOn(orgsAndPoliciesSelectors, 'selectOwnerProperties')
      .mockImplementation(() => ({ ownerType: 'organization' }));
    jest.spyOn(orgsAndPoliciesSelectors, 'selectSelectedOwnerId').mockImplementation(() => 'owner1');
    jest.spyOn(CLMLocation, 'getCpeConfigurationUrl').mockImplementation(() => '/api/cpe-config');
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('reducers', () => {
    it('should return the initial state', () => {
      expect(reducer(undefined, { type: '@@INIT' })).toEqual(initialState);
    });

    it('setCpeStatus should set inherited', () => {
      const state = {
        ...initialState,
        serverData: { enabledInParent: true, inheritedFromOrganizationName: 'foo', enabled: false },
        data: { enabled: false, inheritedFromOrganizationName: null },
      };
      const nextState = reducer(state, actions.setCpeStatus({ inherited: true }));
      expect(nextState.data.inheritedFromOrganizationName).toEqual('foo');
    });

    it('setCpeStatus should set enabled', () => {
      const state = { ...initialState, data: { enabled: false, inheritedFromOrganizationName: 'foo' } };
      const nextState = reducer(state, actions.setCpeStatus({ inherited: false, enabled: true }));
      expect(nextState.data.enabled).toBe(true);
      expect(nextState.data.inheritedFromOrganizationName).toBeNull();
    });

    it('toggleCpeOverride should toggle allowOverride', () => {
      const state = { ...initialState, data: { ...initialState.data, allowOverride: false } };
      const nextState = reducer(state, actions.toggleCpeOverride());
      expect(nextState.data.allowOverride).toBe(true);
    });

    it('saveMaskTimerDone should set submitMaskState to null', () => {
      const state = { ...initialState, submitMaskState: true };
      const nextState = reducer(state, actions.saveMaskTimerDone());
      expect(nextState.submitMaskState).toBeNull();
    });
  });

  describe('thunks', () => {
    let store;
    beforeEach(() => {
      store = mockStore({
        orgsAndPolicies: {
          root: {
            selectedOwner: { id: 'owner1', name: 'Owner 1' },
          },
        },
      });
    });

    describe('loadCpeConfiguration', () => {
      it('dispatches fulfilled on success', async () => {
        axios.get.mockResolvedValue({ data: { enabled: true, allowOverride: false } });

        await store.dispatch(actions.loadCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched[0].type).toMatch(/pending/);
        expect(dispatched[1].type).toMatch(/fulfilled/);
        expect(dispatched[1].payload).toEqual({ enabled: true, allowOverride: false });
      });

      it('does not call API if ownerType or ownerId is missing', async () => {
        await store.dispatch(actions.loadCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched[0].type).toMatch(/pending/);
        expect(dispatched[1].type).toMatch(/rejected/);
        expect(dispatched[1].payload).toBeUndefined();
      });

      it('dispatches rejected on error', async () => {
        axios.get.mockRejectedValue(new Error('fail'));

        await store.dispatch(actions.loadCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched[1].type).toMatch(/rejected/);
      });
    });

    describe('saveCpeConfiguration', () => {
      beforeEach(() => {
        store = mockStore({
          orgsAndPolicies: {
            publicDataSources: initialState,
            root: {
              selectedOwner: { id: 'owner1', name: 'Owner 1' },
            },
          },
        });
      });

      it('dispatches fulfilled on PUT', async () => {
        store = mockStore({
          orgsAndPolicies: {
            publicDataSources: {
              ...initialState,
              data: {
                ...initialState.data,
                enabled: true,
                inheritedFromOrganizationName: 'inherit',
              },
            },
            root: {
              selectedOwner: { id: 'owner1', name: 'Owner 1' },
            },
          },
        });
        axios.put.mockResolvedValue({ data: { enabled: true, allowOverride: false } });
        axios.all = jest.fn((promises) =>
          Promise.all(promises).then(() => ({
            data: {
              enabled: true,
              allowOverride: false,
            },
          }))
        );

        await store.dispatch(actions.saveCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched[0].type).toMatch(/pending/);
        expect(dispatched.some((a) => a.type.match(/fulfilled/))).toBe(true);
      });

      it('dispatches fulfilled when saving inherited configuration', async () => {
        store = mockStore({
          orgsAndPolicies: {
            publicDataSources: {
              ...initialState,
              data: {
                ...initialState.data,
                enabled: null,
                inheritedFromOrganizationName: 'inherit',
              },
            },
            root: {
              selectedOwner: { id: 'owner1', name: 'Owner 1' },
            },
          },
        });

        axios.put.mockResolvedValue({ data: {} });
        axios.all = jest.fn((promises) => Promise.all(promises).then(() => ({ data: {} })));

        await store.dispatch(actions.saveCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched.some((a) => a.type.match(/fulfilled/))).toBe(true);
        expect(axios.put).toHaveBeenCalled();
      });

      it('dispatches rejected on error', async () => {
        axios.put.mockRejectedValue(new Error('fail'));
        axios.all = jest.fn(() => Promise.reject(new Error('fail')));

        await store.dispatch(actions.saveCpeConfiguration());
        const dispatched = store.getActions();
        expect(dispatched.some((a) => a.type.match(/rejected/))).toBe(true);
      });
    });
  });
});
