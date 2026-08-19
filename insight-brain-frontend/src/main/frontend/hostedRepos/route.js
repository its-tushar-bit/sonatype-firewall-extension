/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import HostedReposListPage from 'MainRoot/hostedRepos/HostedReposListPage';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import HostedReposPage from './HostedReposPage';
import RepositoryComponentsList from './RepositoryComponentsList';
import { selectIsHostedRepositoryEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

router.stateRegistry.register({
  name: 'hostedRepos',
  url: '/hostedRepos',
  component: withStoreProvider(HostedReposPage),
  data: {
    title: 'Repository Managers',
    authenticationRequired: true,
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
});

router.stateRegistry.register({
  name: 'hostedRepositories',
  url: '/hostedRepos/{repositoryManagerId}',
  component: withStoreProvider(HostedReposListPage),
  data: {
    title: 'Hosted Repositories',
    authenticationRequired: true,
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
});

router.stateRegistry.register({
  name: 'hostedRepoComponents',
  url: '/hostedRepos/{repositoryManagerId}/{repositoryId}/components?{repositoryPublicId}',
  component: withStoreProvider(RepositoryComponentsList),
  data: {
    title: 'Repository Components',
    authenticationRequired: true,
    featureEnabled: selectIsHostedRepositoryEvaluationEnabled,
  },
});
