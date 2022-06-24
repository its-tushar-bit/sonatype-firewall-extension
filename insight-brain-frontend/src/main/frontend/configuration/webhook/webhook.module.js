/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import EditWebhookContainer from './editWebhook/EditWebhookContainer';
import ListWebhooksContainer from './listWebhooks/ListWebhooksContainer';

export default angular
  .module('webhook.module', [], webhookModuleConfiguration)
  .component('editWebhook', iqReact2Angular(EditWebhookContainer, [], ['$ngRedux', '$state']))
  .component('listWebhooks', iqReact2Angular(ListWebhooksContainer, [], ['$ngRedux', '$state']));

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
