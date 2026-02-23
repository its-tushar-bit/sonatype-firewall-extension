/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { actions } from 'MainRoot/componentDetails/claim/claimSlice';
import * as claimSelectors from 'MainRoot/componentDetails/claim/claimSelectors';
import { getClaimComponentUrl } from 'MainRoot/util/CLMLocation';

import 'TestRoot/SpecUtil';

describe('claimActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {};
    store = SpecUtil.mockReduxStore(state);

    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  describe('claim', () => {
    const requestData = {
      hash: '203',
      comment: 'comment',
      createTime: 1611180000000,
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'artifactId',
          classifier: 'classifier',
          extension: 'extension',
          groupId: 'groupId',
          version: 'version',
        },
      },
    };

    beforeEach(() => {
      jest.spyOn(claimSelectors, 'selectClaimRequestData').mockReturnValue(requestData);
      jest.spyOn(claimSelectors, 'selectClaimId').mockReturnValue('300');
    });

    const { claim } = actions;

    it('claims component successfully', (done) => {
      mockAxiosCalls({
        put: {
          [getClaimComponentUrl()]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(claim()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/component/identified', requestData);

        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/claim/pending',
          'componentDetailsClaim/claim/fulfilled',
          'componentDetailsClaim/claimMaskTimerDone',
        ]);

        expect(actions[1].payload).toEqual({});

        done();
      });
    });

    it('dispatches rejected action if claim request fails', (done) => {
      mockAxiosCalls({
        put: {
          [getClaimComponentUrl()]: () => Promise.reject('something went wrong'),
        },
      });

      store.dispatch(claim()).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/component/identified', requestData);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/claim/pending',
          'componentDetailsClaim/claim/rejected',
        ]);

        expect(actions[1].payload).toBe('something went wrong');

        done();
      });
    });
  });

  describe('revoke', () => {
    beforeEach(() => {
      jest.spyOn(claimSelectors, 'selectSelectedComponentHash').mockReturnValue('200');
    });

    const { revoke } = actions;

    it('revokes component claim successfully', (done) => {
      mockAxiosCalls({
        del: {
          [getClaimComponentUrl('200')]: Promise.resolve({ data: '' }),
        },
      });

      store.dispatch(revoke()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();

        expect(actions.length).toBe(4);

        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/revoke/pending',
          'componentDetailsClaim/revoke/fulfilled',
          'componentDetailsClaim/revokeMaskTimerDone',
          'componentDetailsClaim/toggleShowRevokeModal',
        ]);

        done();
      });
    });

    it('dispatches rejected action if revoke request fails', (done) => {
      mockAxiosCalls({
        del: {
          [getClaimComponentUrl('200')]: () => Promise.reject('could not revoke claim for the given component'),
        },
      });

      store.dispatch(revoke()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/revoke/pending',
          'componentDetailsClaim/revoke/rejected',
        ]);

        expect(actions[1].payload).toBe('could not revoke claim for the given component');

        done();
      });
    });
  });

  describe('loadComponentIdentified', () => {
    beforeEach(() => {
      jest.spyOn(claimSelectors, 'selectSelectedComponentHash').mockReturnValue('200');
    });

    const { loadComponentIdentified } = actions;

    it('loads component claim information', (done) => {
      mockAxiosCalls({
        get: {
          [getClaimComponentUrl('200')]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(loadComponentIdentified()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/loadComponentIdentified/pending',
          'componentDetailsClaim/loadComponentIdentified/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({});

        done();
      });
    });

    it('dispatches rejected action if load claim request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getClaimComponentUrl('200')]: () => Promise.reject('could not find claim for the given component'),
        },
      });

      store.dispatch(loadComponentIdentified()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'componentDetailsClaim/loadComponentIdentified/pending',
          'componentDetailsClaim/loadComponentIdentified/rejected',
        ]);

        expect(actions[1].payload).toBe('could not find claim for the given component');

        done();
      });
    });
  });
});
