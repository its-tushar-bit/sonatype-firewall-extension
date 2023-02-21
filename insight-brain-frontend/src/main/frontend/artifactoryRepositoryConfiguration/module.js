/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import ArtifactoryRepositoryBaseConfigurations from 'MainRoot/artifactoryRepositoryConfiguration/ArtifactoryRepositoryBaseConfigurations';

export default angular
  .module('artifactoryRepositoryConfigurationModule', [])
  .component(
    'artifactoryRepositoryBaseConfigurations',
    iqReact2Angular(ArtifactoryRepositoryBaseConfigurations, [], ['$ngRedux'])
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
        isDirty: ['artifactoryRepositoryBaseConfigurations', 'isDirty'],
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
