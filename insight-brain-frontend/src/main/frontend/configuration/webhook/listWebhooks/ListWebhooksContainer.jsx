/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';
import { prop } from 'ramda';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { loadWebhookListPage } from '../webhookActions';

import ListWebhooks from '../listWebhooks/ListWebhooks';

export default connect(prop('webhooks'), { loadWebhookListPage, stateGo })(ListWebhooks);
