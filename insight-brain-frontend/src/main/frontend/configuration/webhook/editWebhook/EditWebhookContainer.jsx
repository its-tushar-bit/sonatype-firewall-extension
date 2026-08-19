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
import { selectTenantMode } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default connect(
  (state) => {
    const { webhooks, router } = state;
    const tenantMode = selectTenantMode(state);
    return {
      ...webhooks,
      router,
      tenantMode,
    };
  },
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
