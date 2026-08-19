/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryDeleteConfigurationModalSlice';
import { getArtifactoryConnectionUrl } from 'MainRoot/util/CLMLocation';
import { omit } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import 'TestRoot/SpecUtil';

const TEST_ARTIFACTORY_CONNECTION_ID = 'someArtifactoryConnectionId';

describe('artifactoryRepositoryDeleteConfigurationModalActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          organizationId: 'someOwnerId',
        },
      },
      artifactoryRepositoryDeleteConfigurationModal: {
        artifactoryConnectionId: TEST_ARTIFACTORY_CONNECTION_ID,
        showModal: true,
      },
    };
  });

  describe('deleteConfiguration', () => {
    it('immediately dispatches a artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending action and appropriate requests', () => {
      store = SpecUtil.mockReduxStore(state);

      mockAxiosCalls({
        del: {
          [getArtifactoryConnectionUrl(
            'organization',
            'someOwnerId',
            TEST_ARTIFACTORY_CONNECTION_ID
          )]: Promise.resolve(),
        },
      });

      store.dispatch(actions.deleteConfiguration());

      expect(store.getActions()).toHaveAction({
        type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
      });
      expect(axios.delete).toHaveBeenCalledTimes(1);
      expect(axios.delete).toHaveBeenCalledWith(
        `/api/v2/config/artifactoryConnection/organization/someOwnerId/${TEST_ARTIFACTORY_CONNECTION_ID}`
      );
    });
  });

  it('dispatches a artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled action after successful requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    jest.useFakeTimers();
    mockAxiosCalls({
      del: {
        [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: Promise.resolve({
          data: {},
        }),
      },
    });

    store.dispatch(actions.deleteConfiguration()).then(() => {
      let actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions.length).toBe(2);
      expect(actions).toHaveActionsInOrder([
        {
          type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
        },
        {
          type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/fulfilled',
        },
      ]);
      jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions.length).toBe(4);
      expect(actions).toHaveAction({
        type: 'artifactoryRepositoryDeleteConfigurationModal/reset',
      });
      expect(actions).toHaveAction({
        type: 'artifactoryRepositoryBaseConfigurations/load/pending',
      });
      jest.useRealTimers();
      done();
    });
  });

  it('dispatches a artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/rejected action after failed requests', (done) => {
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls({
      del: {
        [getArtifactoryConnectionUrl('organization', 'someOwnerId', TEST_ARTIFACTORY_CONNECTION_ID)]: () =>
          Promise.reject('someError'),
      },
    });
    store.dispatch(actions.deleteConfiguration()).then(() => {
      const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(actions).toHaveActionsInOrder([
        {
          type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/pending',
        },
        {
          type: 'artifactoryRepositoryDeleteConfigurationModal/deleteConfiguration/rejected',
          payload: 'someError',
        },
      ]);
      done();
    });
  });
});
