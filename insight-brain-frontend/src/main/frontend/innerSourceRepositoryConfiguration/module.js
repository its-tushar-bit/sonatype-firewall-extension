/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import InnerSourceRepositoryConfiguration from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryConfiguration';

export default angular
  .module('innerSourceRepositoryConfigurationModule', [])
  .component(
    'innerSourceRepositoryConfiguration',
    react2angular(withStoreProvider(InnerSourceRepositoryConfiguration), [], ['$ngRedux'])
  )
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('repositoryConfiguration', {
      abstract: true,
      url: '/management/edit',
      component: 'innerSourceRepositoryConfiguration',
      data: {
        title: 'Repository Configuration',
      },
    })
    .state('repositoryConfiguration.organization', {
      url: '/organization/{organizationId}/repositoryConfiguration',
    })
    .state('repositoryConfiguration.organization.edit', {
      url: '/{repositoryConnectionId}',
    })
    .state('repositoryConfiguration.application', {
      url: '/application/{applicationId}/repositoryConfiguration',
    })
    .state('repositoryConfiguration.application.edit', {
      url: '/{repositoryConnectionId}',
    });
}

routes.$inject = ['$stateProvider'];
