/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import ArtifactoryRepositoryBaseConfigurations from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryBaseConfigurations';

export default angular
  .module('artifactoryRepositoryConfigurationModule', [])
  .component(
    'artifactoryRepositoryBaseConfigurations',
    react2angular(withStoreProvider(ArtifactoryRepositoryBaseConfigurations), [], ['$ngRedux'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('artifactoryRepositoryBaseConfigurations', {
      abstract: true,
      url: '/management/edit',
      component: 'artifactoryRepositoryBaseConfigurations',
      data: {
        title: 'Artifactory Repository Configurations',
      },
    })
    .state('artifactoryRepositoryBaseConfigurations.organization', {
      url: '/organization/{organizationId}/artifactoryRepositoryBaseConfigurations',
    })
    .state('artifactoryRepositoryBaseConfigurations.application', {
      url: '/application/{applicationId}/artifactoryRepositoryBaseConfigurations',
    });
}

routes.$inject = ['$stateProvider'];
