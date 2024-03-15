/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSlice';
import { omit } from 'ramda';
import { getRepositoryComponentsUrl } from 'MainRoot/util/CLMLocation';

describe('repositoryResultsSummaryPageSliceActions', () => {
  let axiosMock, store, state;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
    state = {
      repositoryResultsSummaryPage: {
        repositoryInfo: {
          id: 'someRepoId',
        },
      },
    };
  });

  describe('toggleAggregateAndGetRepositoryComponents', () => {
    it('dispatches repositoryResultsSummaryPage/toggleAggregate and repositoryResultsSummaryPage/getRepositoryComponents/pending actions', () => {
      axiosMock.onPost(getRepositoryComponentsUrl('repository', 'someRepoId')).reply(200, []);
      store = SpecUtil.mockReduxStore(state);

      store.dispatch(actions.toggleAggregateAndGetRepositoryComponents());

      const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
      expect(dispatchedActions).toHaveActionsInOrder([
        { type: 'repositoryResultsSummaryPage/toggleAggregate' },
        { type: 'repositoryResultsSummaryPage/getRepositoryComponents/pending' },
      ]);
      expect(axiosMock.history.post.length).toBe(1);
      expect(axiosMock.history.post[0].url).toBe(getRepositoryComponentsUrl('repository', 'someRepoId'));
    });
  });
});
