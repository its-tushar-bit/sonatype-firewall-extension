/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { react2angular } from 'react2angular';
import withStoreProvider from 'MainRoot/reactAdapter/StoreProvider';
import InnerSourceRepositoryBaseConfigurations from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryBaseConfigurations';
import InnerSourceRepositoryConfiguration from 'MainRoot/innerSourceRepositoryConfiguration/InnerSourceRepositoryConfiguration';

export default angular
  .module('innerSourceRepositoryConfigurationModule', [])
  .component(
    'innerSourceRepositoryBaseConfigurations',
    react2angular(withStoreProvider(InnerSourceRepositoryBaseConfigurations), [], ['$ngRedux'])
  )
  .component(
    'innerSourceRepositoryConfiguration',
    react2angular(withStoreProvider(InnerSourceRepositoryConfiguration), [], ['$ngRedux'])
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
      },
    })
    .state('repositoryBaseConfigurations.organization', {
      url: '/organization/{organizationId}/repositoryBaseConfigurations',
    })
    .state('repositoryBaseConfigurations.application', {
      url: '/application/{applicationId}/repositoryBaseConfigurations',
    })
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
