/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryDeleteConfigurationModalSlice';
import { getRepositoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

const TEST_REPOSITORY_CONNECTION_ID = 'someRepositoryConnectionId';

describe('innerSourceRepositoryDeleteConfigurationModalActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      innerSourceRepositoryDeleteConfigurationModal: {
        repositoryConnectionId: TEST_REPOSITORY_CONNECTION_ID,
        showModal: true,
      },
    };
  });

  describe('deleteConfiguration', () => {
    it('immediately dispatches a innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);

      mockAxiosCalls({
        del: {
          [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve(),
        },
      });

      store.dispatch(actions.deleteConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
      });
      expect(axios.delete).toHaveBeenCalledTimes(1);
      expect(axios.delete).toHaveBeenCalledWith(
        `/api/v2/config/repositoryConnection/organization/someOwnerId/${TEST_REPOSITORY_CONNECTION_ID}`
      );
    });
  });

  it('dispatches a innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled action after successful requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    jest.useFakeTimers();
    mockAxiosCalls({
      del: {
        [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: Promise.resolve({
          data: {},
        }),
      },
    });

    store.dispatch(actions.deleteConfiguration()).then(() => {
      let actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionsInOrder([
        {
          type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
        },
        {
          type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled',
        },
      ]);
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions.length).toBe(4);
      expect(actions).toHaveAction({
        type: 'innerSourceRepositoryDeleteConfigurationModal/reset',
      });
      expect(actions).toHaveAction({
        type: 'innerSourceRepositoryBaseConfigurations/load/pending',
      });
      jest.useRealTimers();
      done();
    });
  });

  it('dispatches a innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/rejected action after failed requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      del: {
        [getRepositoryConnectionUrl('organization', 'someOwnerId', TEST_REPOSITORY_CONNECTION_ID)]: () =>
          Promise.reject('someError'),
      },
    });
    store.dispatch(actions.deleteConfiguration()).then(() => {
      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionsInOrder([
        {
          type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
        },
        {
          type: 'innerSourceRepositoryDeleteConfigurationModal/deleteConfiguration/rejected',
          payload: 'someError',
        },
      ]);
      done();
    });
  });
});
