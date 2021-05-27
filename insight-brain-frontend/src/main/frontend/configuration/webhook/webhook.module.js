/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import angularCommonModule from '../../util/AngularCommon';
import permissionServiceModule from '../../util/PermissionService';
import storesModule from '../../util/Stores';
import ProductFeaturesModule from '../../util/ProductFeatures';
import WebhookViewController from './webhook.view.controller';
import WebhookListController from './webhook.list.controller';
import WebhookEditController from './webhook.edit.controller';

import viewTemplate from './webhook.view.html';
import listTemplate from './webhook.list.view.html';
import editTemplate from './webhook.edit.view.html';

import EditWebhookContainer from './editWebhook/EditWebhookContainer';
import { react2angular } from 'react2angular';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';

export default angular
  .module(
    'webhook.module',
    [
      storesModule.name,
      'ui.bootstrap',
      'ui.router',
      angularCommonModule.name,
      permissionServiceModule.name,
      ProductFeaturesModule.name,
    ],
    webhookModuleConfiguration
  )
  .controller('webhook.view.controller', WebhookViewController)
  .controller('webhook.list.controller', WebhookListController)
  .controller('webhook.edit.controller', WebhookEditController)
  .component(
    'editWebhook',
    react2angular(withStoreProvider(withRouterStateProvider(EditWebhookContainer)), [], ['$ngRedux', '$state'])
  );

function webhookModuleConfiguration($stateProvider) {
  $stateProvider
    .state('webhooks', {
      url: '/webhooks',
      abstract: true,
      template: viewTemplate,
      resolve: {
        isAuthorized: [
          'PermissionService',
          function (PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          },
        ],
      },
      controller: 'webhook.view.controller',
      controllerAs: 'vm',
    })
    .state('webhooks.list', {
      url: '/list',
      template: listTemplate,
      controller: 'webhook.list.controller',
      controllerAs: 'vm',
      data: {
        title: 'Webhook Configuration',
      },
    })
    .state('webhooks.create', {
      url: '/create',
      controller: 'webhook.edit.controller',
      controllerAs: 'vm',
      template: editTemplate,
      data: {
        title: 'Create Webhook',
      },
    })
    .state('webhooks.edit', {
      url: '/{webhookId}',
      controller: 'webhook.edit.controller',
      controllerAs: 'vm',
      template: editTemplate,
      data: {
        title: 'Edit Webhook',
      },
    })
    .state('addWebhook', {
      url: '/webhooks/add',
      component: 'editWebhook',
      data: {
        title: 'Create Webhook',
      },
    });
}

webhookModuleConfiguration.$inject = ['$stateProvider'];
