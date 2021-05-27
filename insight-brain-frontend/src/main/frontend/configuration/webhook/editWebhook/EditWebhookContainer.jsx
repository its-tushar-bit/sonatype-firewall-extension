/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { prop } from 'ramda';

import { loadWebhookData, toggleEventType, setUrl, setDescription, setSecretKey, saveWebhook } from './webhooksActions';
import { stateGo } from '../../../reduxUiRouter/routerActions';

import EditWebhook from './EditWebhook';

export default connect(prop('editWebhook'), {
  loadWebhookData,
  toggleEventType,
  setUrl,
  setDescription,
  setSecretKey,
  saveWebhook,
  stateGo,
})(EditWebhook);
