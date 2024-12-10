/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import * as PropTypes from 'prop-types';
import { NxCard, NxH3, NxP, NxTextLink } from '@sonatype/react-shared-components';

export default function IntegrationsCard({ title, imgUrl, description, linkText, linkUrl, dataAnalyticsId }) {
  return (
    <NxCard className="iq-integrations-card" aria-label={title}>
      <NxCard.Header className="iq-integrations-card--flex-header">
        <NxH3>{title}</NxH3>
        <img src={imgUrl} alt="" className="iq-integrations-card-logo" />
      </NxCard.Header>

      <NxCard.Content>
        <NxCard.Text className="iq-integrations-card--align-left">
          <NxP>{description}</NxP>
        </NxCard.Text>
      </NxCard.Content>

      <NxCard.Footer className="iq-integrations-card--align-left">
        <NxTextLink external href={linkUrl} data-analytics-id={dataAnalyticsId}>
          {linkText}
        </NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}

IntegrationsCard.propTypes = {
  title: PropTypes.string.isRequired,
  imgUrl: PropTypes.string.isRequired,
  description: PropTypes.string.isRequired,
  linkText: PropTypes.string.isRequired,
  linkUrl: PropTypes.string.isRequired,
  dataAnalyticsId: PropTypes.string.isRequired,
};
