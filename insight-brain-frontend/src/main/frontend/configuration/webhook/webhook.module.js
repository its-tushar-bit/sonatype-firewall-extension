/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';

import EditWebhookContainer from './editWebhook/EditWebhookContainer';
import ListWebhooksContainer from './listWebhooks/ListWebhooksContainer';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';

export default angular
  .module('webhook.module', [], webhookModuleConfiguration)
  .component(
    'editWebhook',
    react2angular(withStoreProvider(withRouterStateProvider(EditWebhookContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'listWebhooks',
    react2angular(withStoreProvider(withRouterStateProvider(ListWebhooksContainer)), [], ['$ngRedux', '$state'])
  );

function webhookModuleConfiguration($stateProvider) {
  $stateProvider
    .state('listWebhooks', {
      url: '/webhooks/list',
      component: 'listWebhooks',
      data: {
        title: 'Webhooks',
      },
    })
    .state('addWebhook', {
      url: '/webhooks/create',
      component: 'editWebhook',
      data: {
        title: 'Create Webhook',
        isDirty: ['webhooks', 'isDirty'],
      },
    })
    .state('editWebhook', {
      url: '/webhooks/{webhookId}',
      component: 'editWebhook',
      data: {
        title: 'Edit Webhook',
        isDirty: ['webhooks', 'isDirty'],
      },
    });
}

webhookModuleConfiguration.$inject = ['$stateProvider'];
