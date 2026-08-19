/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import EditWebhookContainer from './editWebhook/EditWebhookContainer';
import ListWebhooksContainer from './listWebhooks/ListWebhooksContainer';

router.stateRegistry.register({
  name: 'listWebhooks',
  url: '/webhooks/list',
  component: ListWebhooksContainer,
  data: {
    title: 'Webhooks',
  },
});

router.stateRegistry.register({
  name: 'addWebhook',
  url: '/webhooks/create',
  component: EditWebhookContainer,
  data: {
    title: 'Create Webhook',
    isDirty: ['webhooks', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'editWebhook',
  url: '/webhooks/{webhookId}',
  component: EditWebhookContainer,
  data: {
    title: 'Edit Webhook',
    isDirty: ['webhooks', 'isDirty'],
  },
});
