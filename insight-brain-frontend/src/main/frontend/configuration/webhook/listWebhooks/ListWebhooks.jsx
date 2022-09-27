/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import { NxButton, NxFontAwesomeIcon, NxList, NxLoadWrapper } from '@sonatype/react-shared-components';
import WebhookListItem from './WebhookListItem';
import webhookPropType from '../webhookPropType';

function ListWebhooks({ isLoading, loadError, loadWebhookListPage, webhooks, isAppWebhooksSupported, stateGo }) {
  useEffect(() => {
    loadWebhookListPage();
  }, []);

  const addWebhook = () => {
    stateGo('addWebhook');
  };

  return (
    <main className="nx-page-main" id="webhooks-list">
      <div className="nx-page-title">
        <h1 className="nx-h1">Webhooks</h1>
      </div>
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadWebhookListPage}>
        <section className="nx-tile">
          <header className="nx-tile-header">
            <div className="nx-tile-header__title">
              <h2 className="nx-h2">Configure Webhooks</h2>
            </div>
            <div className="nx-tile-header__subtitle">
              IQ Server will send a POST request to the specified URL when the associated events occur
            </div>
            <div className="nx-tile__actions">
              <NxButton variant="tertiary" id="create-webhook" onClick={addWebhook}>
                <NxFontAwesomeIcon icon={faPlus} />
                <span>Add a Webhook</span>
              </NxButton>
            </div>
          </header>
          <div className="nx-tile-content">
            <NxList emptyMessage="No webhooks are defined">
              {webhooks.map((webhook) => (
                <WebhookListItem webhook={webhook} isAppWebhooksSupported={isAppWebhooksSupported} key={webhook.id} />
              ))}
            </NxList>
          </div>
        </section>
      </NxLoadWrapper>
    </main>
  );
}

export default ListWebhooks;

ListWebhooks.propTypes = {
  isLoading: PropTypes.bool,
  isAppWebhooksSupported: PropTypes.bool,
  loadError: PropTypes.string,
  webhooks: PropTypes.arrayOf(webhookPropType).isRequired,
  loadWebhookListPage: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
};
