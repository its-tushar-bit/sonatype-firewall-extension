/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getDashboardStageUrl, getCliStageUrl, getActionStageUrl } from '../../../main/frontend/util/CLMLocation';
import {
  validPurposes,
  fetchStageTypes,
  FETCH_STAGE_TYPES_REQUESTED,
  FETCH_STAGE_TYPES_FULFILLED,
  FETCH_STAGE_TYPES_FAILED,
} from '../../../main/frontend/stages/stagesActions';

import 'TestRoot/SpecUtil';

describe('stagesActions', function () {
  describe('validPurposes', function () {
    it('contains dashboard, action, and cli', function () {
      expect(validPurposes).toContain('dashboard');
      expect(validPurposes).toContain('action');
      expect(validPurposes).toContain('cli');
    });
  });

  describe('fetchStageTypes', function () {
    it('throws an error if the specified purpose is not valid', function () {
      const mockState = {
          stages: {
            dashboard: {
              stageTypes: [1],
            },
          },
        },
        store = SpecUtil.mockReduxStore(mockState);

      expect(() => store.dispatch(fetchStageTypes('foo'))).toThrowError();
    });

    describe('when corresponding stageTypes are already present', function () {
      it('does not dispatch actions', function (done) {
        const mockState = {
            stages: {
              action: {
                stageTypes: [1],
              },
            },
          },
          store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get');

        store.dispatch(fetchStageTypes('action')).then(function () {
          expect(store.getActions().length).toBe(0);

          done();
        });

        expect(axios.get).not.toHaveBeenCalled();
      });
    });

    describe('when corresponding stageTypes are not already present', function () {
      it('dispatches FETCH_STAGE_TYPES_REQUESTED immediately', function () {
        const mockState = {
            stages: {
              dashboard: {
                stageTypes: null,
              },
            },
          },
          store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve());

        store.dispatch(fetchStageTypes('dashboard'));

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toEqual(FETCH_STAGE_TYPES_REQUESTED);
        expect(store.getActions()[0].payload).toBe('dashboard');
      });

      it('dispatches FETCH_STAGE_TYPES_FULFILLED with response data', function (done) {
        const mockState = {
            stages: {
              dashboard: {
                stageTypes: null,
              },
            },
          },
          responseData = [1, 2, 3],
          store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({ data: responseData }));

        store.dispatch(fetchStageTypes('dashboard')).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toEqual(FETCH_STAGE_TYPES_FULFILLED);
          expect(store.getActions()[1].payload).toEqual({
            purpose: 'dashboard',
            data: [1, 2, 3],
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(axios.get).toHaveBeenCalledWith(getDashboardStageUrl());
      });

      it('calls the correct URL for each purpose', function () {
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
          },
          store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockReturnValue(new Promise(() => {}));

        expect(axios.get).not.toHaveBeenCalled();

        store.dispatch(fetchStageTypes('dashboard'));

        expect(axios.get).toHaveBeenCalledWith(getDashboardStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getActionStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getCliStageUrl());

        store.dispatch(fetchStageTypes('action'));

        expect(axios.get).toHaveBeenCalledWith(getActionStageUrl());
        expect(axios.get).not.toHaveBeenCalledWith(getCliStageUrl());

        store.dispatch(fetchStageTypes('cli'));

        expect(axios.get).toHaveBeenCalledWith(getCliStageUrl());
      });

      it('dispatches FETCH_STAGE_TYPES_FAILED when the response fails', function (done) {
        const mockState = {
            stages: {
              cli: {
                stageTypes: null,
              },
            },
          },
          responseError = 'errrr!',
          store = SpecUtil.mockReduxStore(mockState);

        jest.spyOn(axios, 'get').mockImplementation(() => Promise.reject(responseError));

        store.dispatch(fetchStageTypes('cli')).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toEqual(FETCH_STAGE_TYPES_FAILED);
          expect(store.getActions()[1].payload).toEqual({
            purpose: 'cli',
            error: responseError,
          });
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(axios.get).toHaveBeenCalledWith(getCliStageUrl());
      });
    });
  });
});
