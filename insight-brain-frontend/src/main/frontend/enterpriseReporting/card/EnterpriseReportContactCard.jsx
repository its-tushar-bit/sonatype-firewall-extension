/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import PropTypes from 'prop-types';
import { NxFontAwesomeIcon, NxH3, NxCard } from '@sonatype/react-shared-components';
import { faExternalLink } from '@fortawesome/pro-regular-svg-icons';

export default function EnterpriseReportContactCard(props) {
  const { icon, description, title, buttonText, linkUrl, external } = props;

  return (
    <NxCard className="iq-enterprise-reporting-card iq-enterprise-reporting-card--contact">
      <NxCard.Header>
        <NxH3 className="iq-enterprise-reporting-card__title">{title}</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.CallOut className="iq-enterprise-reporting-card__icon iq-enterprise-reporting-card__icon--contact">
          <NxFontAwesomeIcon icon={icon} />
        </NxCard.CallOut>
        <NxCard.Text className="iq-enterprise-reporting-card__description contact-card">{description}</NxCard.Text>
      </NxCard.Content>
      <a
        href={linkUrl}
        rel={external ? 'noreferrer' : ''}
        target={external ? '_blank' : ''}
        className="iq-enterprise-reporting-card__button nx-btn nx-btn--tertiary"
      >
        {buttonText}
        <NxFontAwesomeIcon icon={faExternalLink} />
      </a>
    </NxCard>
  );
}

EnterpriseReportContactCard.propTypes = {
  icon: PropTypes.object,
  title: PropTypes.string,
  description: PropTypes.string,
  buttonText: PropTypes.string,
  linkUrl: PropTypes.string,
  external: PropTypes.bool,
};
