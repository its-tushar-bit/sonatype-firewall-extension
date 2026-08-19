/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRelayWebhookSecret, getRelayWebhookUrl } from 'MainRoot/util/CLMLocation';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { omit } from 'ramda';

describe('fetchRelayWebhookUrl thunk', () => {
  let axiosMock, store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = SpecUtil.mockReduxStore({});
  });

  it('resolves with the webhookUrl on 200', (done) => {
    axiosMock.onGet(getRelayWebhookUrl()).reply(200, { webhookUrl: 'https://relay.example.com/webhook/abc/github' });

    store.dispatch(actions.fetchRelayWebhookUrl()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookUrl/pending', payload: undefined },
        {
          type: 'sourceControl/fetchRelayWebhookUrl/fulfilled',
          payload: 'https://relay.example.com/webhook/abc/github',
        },
      ]);
      done();
    });
  });

  it('resolves with null on 404 (relay enabled, IQ not registered)', (done) => {
    axiosMock.onGet(getRelayWebhookUrl()).reply(404);

    store.dispatch(actions.fetchRelayWebhookUrl()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookUrl/pending', payload: undefined },
        { type: 'sourceControl/fetchRelayWebhookUrl/fulfilled', payload: null },
      ]);
      done();
    });
  });

  it('resolves with null on 412 (feature flag off)', (done) => {
    axiosMock.onGet(getRelayWebhookUrl()).reply(412);

    store.dispatch(actions.fetchRelayWebhookUrl()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookUrl/pending', payload: undefined },
        { type: 'sourceControl/fetchRelayWebhookUrl/fulfilled', payload: null },
      ]);
      done();
    });
  });

  it('rejects on 500', (done) => {
    axiosMock.onGet(getRelayWebhookUrl()).reply(500, 'boom');

    store.dispatch(actions.fetchRelayWebhookUrl()).then(() => {
      const types = store.getActions().map((a) => a.type);
      expect(types).toEqual([
        'sourceControl/fetchRelayWebhookUrl/pending',
        'sourceControl/fetchRelayWebhookUrl/rejected',
      ]);
      done();
    });
  });
});

describe('fetchRelayWebhookSecret thunk', () => {
  let axiosMock, store;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    store = SpecUtil.mockReduxStore({});
  });

  it('resolves with the webhookSecret on 200', (done) => {
    axiosMock.onGet(getRelayWebhookSecret()).reply(200, { webhookSecret: 'whsec_abc123' });

    store.dispatch(actions.fetchRelayWebhookSecret()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookSecret/pending', payload: undefined },
        { type: 'sourceControl/fetchRelayWebhookSecret/fulfilled', payload: 'whsec_abc123' },
      ]);
      done();
    });
  });

  it('resolves with null on 404 (no PAT-mode registration)', (done) => {
    axiosMock.onGet(getRelayWebhookSecret()).reply(404);

    store.dispatch(actions.fetchRelayWebhookSecret()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookSecret/pending', payload: undefined },
        { type: 'sourceControl/fetchRelayWebhookSecret/fulfilled', payload: null },
      ]);
      done();
    });
  });

  it('resolves with null on 412 (feature flag off)', (done) => {
    axiosMock.onGet(getRelayWebhookSecret()).reply(412);

    store.dispatch(actions.fetchRelayWebhookSecret()).then(() => {
      const dispatched = store.getActions().map((a) => omit(['meta', 'error'], a));
      expect(dispatched).toEqual([
        { type: 'sourceControl/fetchRelayWebhookSecret/pending', payload: undefined },
        { type: 'sourceControl/fetchRelayWebhookSecret/fulfilled', payload: null },
      ]);
      done();
    });
  });

  it('rejects on 500', (done) => {
    axiosMock.onGet(getRelayWebhookSecret()).reply(500, 'boom');

    store.dispatch(actions.fetchRelayWebhookSecret()).then(() => {
      const types = store.getActions().map((a) => a.type);
      expect(types).toEqual([
        'sourceControl/fetchRelayWebhookSecret/pending',
        'sourceControl/fetchRelayWebhookSecret/rejected',
      ]);
      done();
    });
  });
});

