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
  NxSmallThreatCounter,
  NxThreatIndicator,
  NxTile,
} from '@sonatype/react-shared-components';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import './ComponentSummary.scss';
import * as PropTypes from 'prop-types';

export default function ComponentSummary({ vulnerabilitySummary, policyViolationSummary, isSbomPoliciesSupported }) {
  const { highestCvssScore, verifiedVulnerabilitiesCount, unverifiedVulnerabilitiesCount } = vulnerabilitySummary;

  const { severe: severePolicyViolations, critical: criticalPolicyViolations } = policyViolationSummary;

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
        {isSbomPoliciesSupported && (
          <div className="sbom-manager-component-detail-tile__policy-violations">
            <div>
              <b>Policy Violations</b>
            </div>
            <div className="sbom-manager-component-detail-tile__policy-violations__content">
              <div className="policy-violations-threat-counter">
                <NxSmallThreatCounter data-testid="severe-threat-counter" severeCount={severePolicyViolations || 0} />
                <p>Severe</p>
              </div>
              <div className="policy-violations-threat-counter">
                <NxSmallThreatCounter
                  data-testid="critical-threat-counter"
                  criticalCount={criticalPolicyViolations || 0}
                />
                <p>Critical</p>
              </div>
            </div>
          </div>
        )}
      </NxTile.Content>
    </NxTile>
  );
}

ComponentSummary.propTypes = {
  vulnerabilitySummary: PropTypes.shape({
    highestCvssScore: PropTypes.number,
    verifiedVulnerabilitiesCount: PropTypes.number,
    unverifiedVulnerabilitiesCount: PropTypes.number,
  }).isRequired,
  policyViolationSummary: PropTypes.shape({
    severe: PropTypes.number,
    critical: PropTypes.number,
  }).isRequired,
  isSbomPoliciesSupported: PropTypes.bool,
};
