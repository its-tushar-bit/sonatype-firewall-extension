/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../SpecUtil';
import axios from 'axios';

import { getDashboardStageUrl, getCliStageUrl, getActionStageUrl } from 'MainRoot/util/CLMLocation';
import { validPurposes, loadStageTypes, actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';

const responseData = [
  { stageTypeId: 1, stageName: 'name 1' },
  { stageTypeId: 2, stageName: 'name 2' },
  { stageTypeId: 3, stageName: 'name 3' },
];
const state = {
  stages: {
    action: {
      stageTypes: [1],
    },
    dashboard: {
      stageTypes: [1],
    },
  },
};

describe('stages actions', function () {
  describe('validPurposes', function () {
    it('contains dashboard, action, and cli', function () {
      expect(validPurposes).toContain('dashboard');
      expect(validPurposes).toContain('action');
      expect(validPurposes).toContain('cli');
    });
  });

  describe('loadStageTypes', function () {
    it('reject with error if the specified purpose is not valid', async function () {
      const store = SpecUtil.mockReduxStore(state);
      await store.dispatch(loadStageTypes('foo'));

      const actions = store.getActions();

      expect(actions).toHaveActionTypesInOrder(['stages/loadStageTypes/pending', 'stages/loadStageTypes/rejected']);
      expect(actions[1].payload).toEqual('purpose must be one of dashboard, action, cli, sbom');
    });

    describe('when corresponding stageTypes are already present', function () {
      it('does not fetch data', async function () {
        const store = SpecUtil.mockReduxStore(state);

        jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: responseData }));

        await store.dispatch(stagesActions.loadActionStages());

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(axios.get).not.toHaveBeenCalled();
        expect(actions).toHaveActionTypesInOrder(['stages/loadStageTypes/pending', 'stages/loadStageTypes/fulfilled']);
      });
    });

    describe('when corresponding stageTypes are not already present', function () {
      it('dispatches pending and fulfilled actions', async function () {
        const mockState = { stages: { dashboard: { stageTypes: null } } };
        const store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: responseData }));

        await store.dispatch(stagesActions.loadDashboardStages());

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['stages/loadStageTypes/pending', 'stages/loadStageTypes/fulfilled']);
        expect(actions[1].payload.data).toEqual([
          { stageTypeId: 1, stageName: 'name 1', shortName: 'name 1' },
          { stageTypeId: 2, stageName: 'name 2', shortName: 'name 2' },
          { stageTypeId: 3, stageName: 'name 3', shortName: 'name 3' },
        ]);
      });

      it('calls the correct URL for each purpose', async function () {
        const mockState = {
          stages: {
            dashboard: {
              stageTypes: null,
            },
            action: {
              stageTypes: null,
            },
            cli: {
              stageTypes: null,
            },
          },
        };
        const store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: [] }));

        expect(axios.get).not.toHaveBeenCalled();

        await store.dispatch(stagesActions.loadDashboardStages());

        expect(axios.get).toHaveBeenCalledWith(getDashboardStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getActionStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getCliStageUrl());

        await store.dispatch(stagesActions.loadActionStages());

        expect(axios.get).toHaveBeenCalledWith(getActionStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getCliStageUrl());

        await store.dispatch(stagesActions.loadCliStages());

        expect(axios.get).toHaveBeenCalledWith(getCliStageUrl());
      });

      it('dispatches stages/loadStageTypes/rejected when the response fails', async function () {
        const mockState = {
          stages: {
            cli: {
              stageTypes: null,
            },
          },
        };
        const responseError = 'errrr!';
        const store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockImplementation(() => Promise.reject(responseError));

        await store.dispatch(stagesActions.loadCliStages());

        const actions = store.getActions();
        expect(axios.get).toHaveBeenCalledWith(getCliStageUrl());
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder(['stages/loadStageTypes/pending', 'stages/loadStageTypes/rejected']);
        expect(actions[1].payload).toEqual(responseError);
      });
    });
  });
});
