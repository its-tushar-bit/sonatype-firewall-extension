/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';

export default angular
  .module('innerSourceRepositoryConfigurationModule', [])
  .component(
    'innerSourceRepositoryBaseConfigurations',
    iqReact2Angular(InnerSourceRepositoryBaseConfigurations, [], ['$ngRedux'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('repositoryBaseConfigurations', {
      abstract: true,
      url: '/management/edit',
      component: 'innerSourceRepositoryBaseConfigurations',
      data: {
        title: 'Repository Configurations',
        isDirty: ['innerSourceRepositoryBaseConfigurations', 'isDirty'],
      },
    })
    .state('repositoryBaseConfigurations.organization', {
      url: '/organization/{organizationId}/repositoryBaseConfigurations',
    })
    .state('repositoryBaseConfigurations.application', {
      url: '/application/{applicationId}/repositoryBaseConfigurations',
    });
}

routes.$inject = ['$stateProvider'];
