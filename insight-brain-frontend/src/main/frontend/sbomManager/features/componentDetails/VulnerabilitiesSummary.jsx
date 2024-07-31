/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  allThreatLevelNumbers,
  NxFontAwesomeIcon,
  NxH2,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
} from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle, faExternalLink } from '@fortawesome/pro-solid-svg-icons';
import './ComponentSummary.scss';
import * as PropTypes from 'prop-types';

export default function VulnerabilitiesSummary({ vulnerabilitySummary }) {
  const {
    highestCvssScore,
    verifiedVulnerabilitiesCount,
    unverifiedVulnerabilitiesCount,
    category,
    website,
  } = vulnerabilitySummary;

  return (
    <NxTile id="sbom-manager-component-detail-tile" className="sbom-manager-component-detail-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Component Summary</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content className="sbom-manager-component-detail-tile__content">
        {highestCvssScore !== undefined && (
          <div className="sbom-manager-component-detail-tile__highest-cvss-score">
            <div>
              <b>Highest CVSS Score</b>
            </div>
            <div>
              <NxThreatIndicator
                policyThreatLevel={allThreatLevelNumbers.find((n) => n === Math.floor(highestCvssScore))}
                presentational
                className="threat-indicator-icon"
              />
              <span data-testid="highestCvssScore">{highestCvssScore}</span>
            </div>
          </div>
        )}
        {(verifiedVulnerabilitiesCount !== undefined || unverifiedVulnerabilitiesCount !== undefined) && (
          <div className="sbom-manager-component-detail-tile__vulnerabilities-verified">
            <div>
              <b>Vulnerabilities Verified</b>
            </div>
            <div className="sbom-manager-component-detail-tile__vulnerabilities-verified__content">
              {verifiedVulnerabilitiesCount !== undefined && (
                <div>
                  <NxFontAwesomeIcon className="sbom-verified-icon" icon={faCheckCircle} />
                  <span data-testid="verified">
                    <b>{verifiedVulnerabilitiesCount}</b> Sonatype Verified
                  </span>
                </div>
              )}
              {unverifiedVulnerabilitiesCount !== undefined && (
                <div>
                  <NxFontAwesomeIcon className="sbom-unverified-icon" icon={faExclamationTriangle} />
                  <span data-testid="unverified">
                    <b>{unverifiedVulnerabilitiesCount}</b> Unverified
                  </span>
                </div>
              )}
            </div>
          </div>
        )}
        {category && (
          <div className="sbom-manager-component-detail-tile__category">
            <div>
              <b>Category</b>
            </div>
            <div data-testid="category">
              <span>{category}</span>
            </div>
          </div>
        )}
        {website && (
          <div className="sbom-manager-component-detail-tile__website">
            <div>
              <b>Website</b>
            </div>
            <div data-testid="website">
              <NxTextLink id="sbom-component-details-link" href={website} target="_blank" rel="noopener noreferrer">
                <span>{website}</span>
                <NxFontAwesomeIcon className="sbom-external-link-icon" icon={faExternalLink} />
              </NxTextLink>
            </div>
          </div>
        )}
      </NxTile.Content>
    </NxTile>
  );
}

VulnerabilitiesSummary.propTypes = {
  vulnerabilitySummary: PropTypes.shape({
    highestCvssScore: PropTypes.number,
    verifiedVulnerabilitiesCount: PropTypes.number,
    unverifiedVulnerabilitiesCount: PropTypes.number,
    category: PropTypes.string,
    website: PropTypes.string,
  }).isRequired,
};
