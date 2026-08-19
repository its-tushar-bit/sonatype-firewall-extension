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
  NxReadOnly,
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
    <NxTile className="sbom-manager-component-detail-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Component Summary</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxReadOnly className="nx-read-only--grid sbom-manager-component-detail-tile__content">
          <NxReadOnly.Item className="sbom-manager-component-detail-tile__highest-cvss-score">
            <NxReadOnly.Label>Highest CVSS Score</NxReadOnly.Label>
            <NxReadOnly.Data>
              <NxThreatIndicator
                policyThreatLevel={allThreatLevelNumbers.find((n) => n === Math.floor(highestCvssScore))}
                presentational
              />
              <span data-testid="highestCvssScore">{highestCvssScore}</span>
            </NxReadOnly.Data>
          </NxReadOnly.Item>
          {(verifiedVulnerabilitiesCount !== undefined || unverifiedVulnerabilitiesCount !== undefined) && (
            <NxReadOnly.Item className="sbom-manager-component-detail-tile__vulnerabilities-verified">
              <NxReadOnly.Label>Vulnerabilities Verified</NxReadOnly.Label>
              {verifiedVulnerabilitiesCount !== undefined && (
                <NxReadOnly.Data>
                  <NxFontAwesomeIcon className="sbom-verified-icon" icon={faCheckCircle} />
                  <span data-testid="verified">
                    <b>{verifiedVulnerabilitiesCount}</b> Sonatype Verified
                  </span>
                </NxReadOnly.Data>
              )}
              {unverifiedVulnerabilitiesCount !== undefined && (
                <NxReadOnly.Data>
                  <NxFontAwesomeIcon className="sbom-unverified-icon" icon={faExclamationTriangle} />
                  <span data-testid="unverified">
                    <b>{unverifiedVulnerabilitiesCount}</b> Unverified
                  </span>
                </NxReadOnly.Data>
              )}
            </NxReadOnly.Item>
          )}
          {isSbomPoliciesSupported && (
            <NxReadOnly.Item className="sbom-manager-component-detail-tile__policy-violations">
              <NxReadOnly.Label>Policy Violations</NxReadOnly.Label>
              <NxReadOnly.Data className="policy-violations-threat-counter">
                <NxSmallThreatCounter
                  data-testid="critical-threat-counter"
                  criticalCount={criticalPolicyViolations || 0}
                />
                <span className="critical-threat-category">Critical</span>
              </NxReadOnly.Data>
              <NxReadOnly.Data className="policy-violations-threat-counter">
                <NxSmallThreatCounter data-testid="severe-threat-counter" severeCount={severePolicyViolations || 0} />
                <span>Severe</span>
              </NxReadOnly.Data>
            </NxReadOnly.Item>
          )}
        </NxReadOnly>
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
