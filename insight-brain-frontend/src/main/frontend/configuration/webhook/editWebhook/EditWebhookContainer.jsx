/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import {
  loadWebhookPage,
  toggleEventType,
  setUrl,
  setDescription,
  setSecretKey,
  saveWebhook,
  deleteWebhook,
} from '../webhookActions';
import { stateGo } from '../../../reduxUiRouter/routerActions';

import EditWebhook from './EditWebhook';

export default connect(
  ({ webhooks, router }) => ({
    ...webhooks,
    router,
  }),
  {
    loadWebhookPage,
    toggleEventType,
    setUrl,
    setDescription,
    setSecretKey,
    saveWebhook,
    deleteWebhook,
    stateGo,
  }
)(EditWebhook);
