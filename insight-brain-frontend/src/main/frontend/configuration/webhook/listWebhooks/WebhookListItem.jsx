/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { faAngleRight } from '@fortawesome/free-solid-svg-icons';
import { NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { useRouterState } from '../../../react/RouterStateContext';
import webhookPropType from '../webhookPropType';
import { isNilOrEmpty } from '../../../util/jsUtil';

function WebhookListItem({ webhook, isAppWebhooksSupported }) {
  const { id, url, description, eventTypes } = webhook;
  const uiRouterState = useRouterState();
  const webhookLabel = description || url;
  const eventTypesDisplay =
    !isNilOrEmpty(eventTypes) &&
    eventTypes.map((eventType, index) => {
      const isDisabled = eventType === 'Application Evaluation' && !isAppWebhooksSupported;
      const comma = index === eventTypes.length - 1 ? null : ', ';
      return (
        <span key={eventType} className={classnames({ 'iq-webhook-event--disabled': isDisabled })}>
          {eventType}
          {comma}
        </span>
      );
    });

  return (
    <li className="nx-list__item nx-list__item--link">
      <NxTextLink href={uiRouterState.href('editWebhook', { webhookId: id })} className="nx-list__link">
        <span className="nx-list__text">{webhookLabel}</span>
        {eventTypesDisplay && <span className="nx-list__subtext">{eventTypesDisplay}</span>}
        <NxFontAwesomeIcon icon={faAngleRight} className="nx-chevron" />
      </NxTextLink>
    </li>
  );
}

export default WebhookListItem;

WebhookListItem.propTypes = {
  isAppWebhooksSupported: PropTypes.bool,
  webhook: webhookPropType,
};
