/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../SpecUtil';
import reducer from 'MainRoot/OrgsAndPolicies/stagesSlice';

const stagesTypes = ['cli', 'action', 'dashboard'];

describe('stages reducer', function () {
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

  describe('stages/loadStageTypes/pending action', function () {
    stagesTypes.forEach((stagesType) => {
      it(`sets the loading flag on ${stagesType} stages to true`, function () {
        const initialState = reducer(undefined, {});
        const newState = reducer(initialState, {
          type: 'stages/loadStageTypes/pending',
          meta: { arg: stagesType },
        });

        expect(newState[stagesType]).toEqual({
          loading: true,
          error: null,
          stageTypes: null,
        });

        stagesTypes.filter((s) => s !== stagesType).forEach((s) => expect(newState[s]).toBe(initialState[s]));
      });
    });
  });

  describe('stages/loadStageTypes/fulfilled action', function () {
    stagesTypes.forEach((stagesType) => {
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
            type: 'stages/loadStageTypes/fulfilled',
            payload: {
              purpose: stagesType,
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

        expect(newState[stagesType]).toEqual({
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

        stagesTypes.filter((s) => s !== stagesType).forEach((s) => expect(newState[s]).toBe(initialState[s]));
      });
    });
  });

  describe('stages/loadStageTypes/rejected action', function () {
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
          type: 'stages/loadStageTypes/rejected',
          meta: { arg: 'cli' },
          payload: "It's broken",
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
