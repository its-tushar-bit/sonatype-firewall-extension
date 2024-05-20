/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxFontAwesomeIcon, NxH2, NxTextLink, NxThreatIndicator, NxTile } from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle, faExternalLink } from '@fortawesome/pro-solid-svg-icons';
import './ComponentSummary.scss';
import * as PropTypes from 'prop-types';

export default function ComponentSummary(props) {
  const { componentDetails } = props;
  const {
    highestCvssScore,
    verifiedVulnerabilitiesCount,
    unverifiedVulnerabilitiesCount,
    category,
    website,
  } = componentDetails.componentSummary;

  ComponentSummary.propTypes = {
    componentDetails: PropTypes.object,
  };

  return (
    <NxTile id="sbom-manager-component-detail-tile" className="sbom-manager-component-detail-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Component Summary</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content className="sbom-manager-component-detail-tile__content">
        <div className="sbom-manager-component-detail-tile__highest-cvss-score">
          <div>
            <b>Highest CVSS Score</b>
          </div>
          <div>
            <NxThreatIndicator policyThreatLevel={highestCvssScore} presentational className="threat-indicator-icon" />

            <span>{highestCvssScore}</span>
          </div>
        </div>

        <div className="sbom-manager-component-detail-tile__vulnerabilities-verified">
          <div>
            <b>Vulnerabilities Verified</b>
          </div>
          <div>
            <NxFontAwesomeIcon className={'sbom-verified-icon'} icon={faCheckCircle} />
            <span data-testid="verified">
              <b>{verifiedVulnerabilitiesCount}</b> Sonatype Verified
            </span>
          </div>
          <div>
            <NxFontAwesomeIcon className={'sbom-unverified-icon'} icon={faExclamationTriangle} />
            <span data-testid="unverified">
              <b>{unverifiedVulnerabilitiesCount}</b> Unverified
            </span>
          </div>
        </div>

        <div className="sbom-manager-component-detail-tile__category">
          <div>
            <b>Category</b>
          </div>
          <div data-testid="category">{category && <span>{category}</span>}</div>
        </div>

        <div className="sbom-manager-component-detail-tile__website">
          <div>
            <b>Website</b>
          </div>
          <div data-testid="website">
            {website && (
              <NxTextLink id="sbom-component-details-link" href="#">
                <span>{website}</span>
                <NxFontAwesomeIcon className={'sbom-external-link-icon'} icon={faExternalLink} />
              </NxTextLink>
            )}
          </div>
        </div>
      </NxTile.Content>
    </NxTile>
  );
}
