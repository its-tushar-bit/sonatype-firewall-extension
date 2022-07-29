/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/api/apiPageSlice';
import { getEndpointsUrl } from 'MainRoot/util/CLMLocation';
import { omit } from 'ramda';

describe('apiPageSliceActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  describe('load', () => {
    it('immediately dispatches a apiPage/loadOpenApi/pending action and an appropriate request', () => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getEndpointsUrl('endpointType')]: Promise.resolve({}),
        },
      });

      store.dispatch(actions.loadOpenApi('endpointType'));

      expect(store.getActions()).toHaveAction({
        type: 'apiPage/loadOpenApi/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(1);
      expect(axios.get).toHaveBeenCalledWith('/api/v2/endpoints/endpointType');
    });

    it('dispatches a apiPage/loadOpenApi/fulfilled action after a successful request', (done) => {
      store = SpecUtil.mockReduxStore(state);
      const payload = 'payload';
      mockAxiosCalls({
        get: {
          [getEndpointsUrl('endpointType')]: Promise.resolve({
            data: payload,
          }),
        },
      });

      store.dispatch(actions.loadOpenApi('endpointType')).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'apiPage/loadOpenApi/pending',
          },
          {
            type: 'apiPage/loadOpenApi/fulfilled',
            payload: { endpoint: 'endpointType', data: payload },
          },
        ]);
        done();
      });
    });

    it('dispatches a apiPage/loadOpenApi/rejected action after a failed request', (done) => {
      store = SpecUtil.mockReduxStore(state);
      mockAxiosCalls({
        get: {
          [getEndpointsUrl('endpointType')]: () => Promise.reject('someError'),
        },
      });

      store.dispatch(actions.loadOpenApi('endpointType')).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([
          {
            type: 'apiPage/loadOpenApi/pending',
          },
          {
            type: 'apiPage/loadOpenApi/rejected',
            payload: 'someError',
          },
        ]);
        done();
      });
    });
  });
});
