/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/stages/stagesReducer';

describe('stagesReducer', function () {
  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState.dashboard.loading).toBe(false);
      expect(newState.dashboard.error).toBe(null);
      expect(newState.dashboard.stageTypes).toBe(null);
      expect(newState.action.loading).toBe(false);
      expect(newState.action.error).toBe(null);
      expect(newState.action.stageTypes).toBe(null);
      expect(newState.cli.loading).toBe(false);
      expect(newState.cli.error).toBe(null);
      expect(newState.cli.stageTypes).toBe(null);
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      // Overall state object
      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      // Nested object-properties
      expect(() => {
        state.dashboard = {};
      }).toThrowError(TypeError);

      expect(() => {
        state.dashboard.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.action.error = 'Broke';
      }).toThrowError(TypeError);

      expect(() => {
        state.cli.stageTypes = [1, 2, 3];
      }).toThrowError(TypeError);
    });
  });

  describe('FETCH_STAGE_TYPES_REQUESTED action', function () {
    it('sets the loading flag on the given subobject to true', function () {
      const initialState = reducer(undefined, {});
      const newState = reducer(initialState, {
        type: 'FETCH_STAGE_TYPES_REQUESTED',
        payload: 'dashboard',
      });

      expect(newState.dashboard).toEqual({
        loading: true,
        error: null,
        stageTypes: null,
      });

      expect(newState.action).toBe(initialState.action);
      expect(newState.cli).toBe(initialState.cli);
    });
  });

  describe('FETCH_STAGE_TYPES_FULFILLED action', function () {
    it('resets error and loading and sets stageTypes to the provided data', function () {
      const initialState = {
          dashboard: {
            loading: true,
            error: 'foo',
            stageTypes: null,
          },
          action: {
            loading: true,
            error: 'foo',
            stageTypes: null,
          },
          cli: {
            loading: true,
            error: 'foo',
            stageTypes: null,
          },
        },
        newState = reducer(initialState, {
          type: 'FETCH_STAGE_TYPES_FULFILLED',
          payload: {
            purpose: 'action',
            data: [
              {
                stageTypeId: 'foo',
                stageName: 'Foo',
              },
              {
                stageTypeId: 'bar',
                stageName: 'Bar',
              },
            ],
          },
        });

      expect(newState.action).toEqual({
        loading: false,
        error: null,
        stageTypes: [
          expect.objectContaining({
            stageTypeId: 'foo',
            stageName: 'Foo',
          }),
          expect.objectContaining({
            stageTypeId: 'bar',
            stageName: 'Bar',
          }),
        ],
      });

      expect(newState.dashboard).toBe(initialState.dashboard);
      expect(newState.cli).toBe(initialState.cli);
    });

    it(
      'adds a shortName field to the action and dashboard purposes, equivalent to the stageName except for ' +
        'stage-release for which it is "Stage"',
      function () {
        const initialState = {
            dashboard: {
              loading: true,
              error: null,
              stageTypes: null,
            },
            action: {
              loading: true,
              error: null,
              stageTypes: null,
            },
            cli: {
              loading: true,
              error: null,
              stageTypes: null,
            },
          },
          newState1 = reducer(initialState, {
            type: 'FETCH_STAGE_TYPES_FULFILLED',
            payload: {
              purpose: 'action',
              data: [
                {
                  stageTypeId: 'build',
                  stageName: 'Build',
                },
                {
                  stageTypeId: 'operate',
                  stageName: 'Operate',
                },
                {
                  stageTypeId: 'proxy',
                  stageName: 'Proxy',
                },
                {
                  stageTypeId: 'stage-release',
                  stageName: 'Stage Release',
                },
                {
                  stageTypeId: 'develop',
                  stageName: 'Develop',
                },
                {
                  stageTypeId: 'release',
                  stageName: 'Release',
                },
              ],
            },
          }),
          newState2 = reducer(initialState, {
            type: 'FETCH_STAGE_TYPES_FULFILLED',
            payload: {
              purpose: 'dashboard',
              data: [
                {
                  stageTypeId: 'build',
                  stageName: 'Build',
                },
                {
                  stageTypeId: 'operate',
                  stageName: 'Operate',
                },
                {
                  stageTypeId: 'proxy',
                  stageName: 'Proxy',
                },
                {
                  stageTypeId: 'stage-release',
                  stageName: 'Stage Release',
                },
                {
                  stageTypeId: 'develop',
                  stageName: 'Develop',
                },
                {
                  stageTypeId: 'release',
                  stageName: 'Release',
                },
              ],
            },
          }),
          newState3 = reducer(initialState, {
            type: 'FETCH_STAGE_TYPES_FULFILLED',
            payload: {
              purpose: 'cli',
              data: [
                {
                  stageTypeId: 'build',
                  stageName: 'Build',
                },
                {
                  stageTypeId: 'operate',
                  stageName: 'Operate',
                },
                {
                  stageTypeId: 'proxy',
                  stageName: 'Proxy',
                },
                {
                  stageTypeId: 'stage-release',
                  stageName: 'Stage Release',
                },
                {
                  stageTypeId: 'develop',
                  stageName: 'Develop',
                },
                {
                  stageTypeId: 'release',
                  stageName: 'Release',
                },
              ],
            },
          });

        expect(newState1.action).toEqual({
          loading: false,
          error: null,
          stageTypes: [
            {
              stageTypeId: 'build',
              stageName: 'Build',
              shortName: 'Build',
            },
            {
              stageTypeId: 'operate',
              stageName: 'Operate',
              shortName: 'Operate',
            },
            {
              stageTypeId: 'proxy',
              stageName: 'Proxy',
              shortName: 'Proxy',
            },
            {
              stageTypeId: 'stage-release',
              stageName: 'Stage Release',
              shortName: 'Stage',
            },
            {
              stageTypeId: 'develop',
              stageName: 'Develop',
              shortName: 'Develop',
            },
            {
              stageTypeId: 'release',
              stageName: 'Release',
              shortName: 'Release',
            },
          ],
        });

        expect(newState2.dashboard).toEqual({
          loading: false,
          error: null,
          stageTypes: [
            {
              stageTypeId: 'build',
              stageName: 'Build',
              shortName: 'Build',
            },
            {
              stageTypeId: 'operate',
              stageName: 'Operate',
              shortName: 'Operate',
            },
            {
              stageTypeId: 'proxy',
              stageName: 'Proxy',
              shortName: 'Proxy',
            },
            {
              stageTypeId: 'stage-release',
              stageName: 'Stage Release',
              shortName: 'Stage',
            },
            {
              stageTypeId: 'develop',
              stageName: 'Develop',
              shortName: 'Develop',
            },
            {
              stageTypeId: 'release',
              stageName: 'Release',
              shortName: 'Release',
            },
          ],
        });

        // no shortNames here
        expect(newState3.cli).toEqual({
          loading: false,
          error: null,
          stageTypes: [
            {
              stageTypeId: 'build',
              stageName: 'Build',
            },
            {
              stageTypeId: 'operate',
              stageName: 'Operate',
            },
            {
              stageTypeId: 'proxy',
              stageName: 'Proxy',
            },
            {
              stageTypeId: 'stage-release',
              stageName: 'Stage Release',
            },
            {
              stageTypeId: 'develop',
              stageName: 'Develop',
            },
            {
              stageTypeId: 'release',
              stageName: 'Release',
            },
          ],
        });
      }
    );
  });

  describe('FETCH_STAGE_TYPES_FAILED action', function () {
    it('resets stageTypes and loading and sets error to the specified value', function () {
      const initialState = {
          dashboard: {
            loading: true,
            error: null,
            stageTypes: [1, 2, 3],
          },
          action: {
            loading: true,
            error: null,
            stageTypes: [1, 2, 3],
          },
          cli: {
            loading: true,
            error: null,
            stageTypes: [1, 2, 3],
          },
        },
        newState = reducer(initialState, {
          type: 'FETCH_STAGE_TYPES_FAILED',
          payload: {
            purpose: 'cli',
            error: "It's broken",
          },
        });

      expect(newState.cli).toEqual({
        loading: false,
        error: "It's broken",
        stageTypes: null,
      });

      expect(newState.dashboard).toBe(initialState.dashboard);
      expect(newState.action).toBe(initialState.action);
    });
  });
});
