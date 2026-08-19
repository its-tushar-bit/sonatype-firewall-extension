/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';

import {
  NxTable,
  NxFontAwesomeIcon,
  NxTag,
  NxLoadingSpinner,
  NxSmallThreatCounter,
} from '@sonatype/react-shared-components';
import { faTrophy, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import classnames from 'classnames';
import { versionComparisonInfoPropType } from '../../componentDetailsUtils';
import { formatTimeAgoUpToDay, formatDate } from 'MainRoot/util/dateUtils';
import { SECURITY, LICENSE, QUALITY, OTHER } from './policyThreatCategory';
import { isEmpty } from 'ramda';

export const CompareVersions = ({ currentVersion, selectedVersion, loading }) => {
  const showHygeneRating = currentVersion.hygieneRating || selectedVersion.hygieneRating;
  const showIntegrityRating = currentVersion.integrityRating || selectedVersion.integrityRating;

  const customDateFormat = (date) => (date ? formatDate(date, 'YYYY-MM-DD HH:mm:ss') : '-');
  const getUnquarantineStatusText = ({ unquarantineTime, autoUnquarantined }) => {
    if (unquarantineTime) {
      if (autoUnquarantined) {
        return `Automatically on ${customDateFormat(unquarantineTime)}`;
      } else {
        return `Manually on ${customDateFormat(unquarantineTime)}`;
      }
    }
    return '-';
  };

  return (
    <section className="iq-compare-versions nx-grid-col__section">
      <header id="compare-versions-header" className="nx-grid-header">
        <h3 className="nx-h3 nx-grid-header__title">Compare Versions</h3>
      </header>
      <NxTable id="compare-versions-table" className="nx-table--fixed-layout">
        <NxTable.Head>
          <NxTable.Row>
            <NxTable.Cell></NxTable.Cell>
            <NxTable.Cell>CURRENT</NxTable.Cell>
            <NxTable.Cell>SELECTED</NxTable.Cell>
          </NxTable.Row>
        </NxTable.Head>
        <NxTable.Body>
          <NxTable.Row id="version">
            <NxTable.Cell>Version</NxTable.Cell>
            <NxTable.Cell>{currentVersion.version}</NxTable.Cell>
            <NxTable.Cell>{loading ? <NxLoadingSpinner /> : selectedVersion.version || '-'}</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestPolicyThreat">
            <NxTable.Cell>Highest Policy Threat</NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreat(currentVersion.highestPolicyThreat, currentVersion.numberOfViolatedPolicies)}
            </NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreat(selectedVersion.highestPolicyThreat, selectedVersion.numberOfViolatedPolicies)}
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestSecurityThreat">
            <NxTable.Cell>Security Violation Threat</NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(currentVersion.policyMaxThreatLevelsByCategory, SECURITY)}
            </NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(selectedVersion.policyMaxThreatLevelsByCategory, SECURITY)}
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestCvssScore">
            <NxTable.Cell>Highest CVSS Score</NxTable.Cell>
            <NxTable.Cell>{currentVersion.highestCVSSScore}</NxTable.Cell>
            <NxTable.Cell>{selectedVersion.highestCVSSScore}</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestLicenseThreat">
            <NxTable.Cell>License Violation Threat</NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(currentVersion.policyMaxThreatLevelsByCategory, LICENSE)}
            </NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(selectedVersion.policyMaxThreatLevelsByCategory, LICENSE)}
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="effectiveLicense">
            <NxTable.Cell>Effective License</NxTable.Cell>
            <NxTable.Cell>{renderEffectiveLicenses(currentVersion)}</NxTable.Cell>
            <NxTable.Cell>{renderEffectiveLicenses(selectedVersion)}</NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestQualityThreat">
            <NxTable.Cell>Quality Violation Threat</NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(currentVersion.policyMaxThreatLevelsByCategory, QUALITY)}
            </NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(selectedVersion.policyMaxThreatLevelsByCategory, QUALITY)}
            </NxTable.Cell>
          </NxTable.Row>
          <NxTable.Row id="highestOtherThreat">
            <NxTable.Cell>Other Violation Threat</NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(currentVersion.policyMaxThreatLevelsByCategory, OTHER)}
            </NxTable.Cell>
            <NxTable.Cell>
              {renderHighestPolicyThreatByCategory(selectedVersion.policyMaxThreatLevelsByCategory, OTHER)}
            </NxTable.Cell>
          </NxTable.Row>
          {showHygeneRating && (
            <NxTable.Row id="hygieneRating">
              <NxTable.Cell>Hygiene Rating</NxTable.Cell>
              <NxTable.Cell>{renderHygieneRating(currentVersion.hygieneRating)}</NxTable.Cell>
              <NxTable.Cell>{renderHygieneRating(selectedVersion.hygieneRating)}</NxTable.Cell>
            </NxTable.Row>
          )}
          {showIntegrityRating && (
            <NxTable.Row id="integrityRating">
              <NxTable.Cell>Integrity Rating</NxTable.Cell>
              <NxTable.Cell>{renderIntegrityRating(currentVersion.integrityRating)}</NxTable.Cell>
              <NxTable.Cell>{renderIntegrityRating(selectedVersion.integrityRating)}</NxTable.Cell>
            </NxTable.Row>
          )}
          <NxTable.Row id="catalogDate" className="visual-testing-ignore">
            <NxTable.Cell>Cataloged</NxTable.Cell>
            <NxTable.Cell>
              {currentVersion.catalogDate ? formatTimeAgoUpToDay(currentVersion.catalogDate) : '-'}
            </NxTable.Cell>
            <NxTable.Cell>
              {!isEmpty(selectedVersion) &&
                (selectedVersion.catalogDate ? formatTimeAgoUpToDay(selectedVersion.catalogDate) : '-')}
            </NxTable.Cell>
          </NxTable.Row>

          {currentVersion.policyEvaluationTimestamps && (
            <>
              <NxTable.Row id="firstPolicyEvaluationTime">
                <NxTable.Cell>First Evaluation</NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {customDateFormat(currentVersion.policyEvaluationTimestamps.firstPolicyEvaluationTime)}
                </NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {!isEmpty(selectedVersion) &&
                    customDateFormat(selectedVersion.policyEvaluationTimestamps.firstPolicyEvaluationTime)}
                </NxTable.Cell>
              </NxTable.Row>
              <NxTable.Row id="latestPolicyEvaluationTime">
                <NxTable.Cell>Latest Evaluation</NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {customDateFormat(currentVersion.policyEvaluationTimestamps.latestPolicyEvaluationTime)}
                </NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {!isEmpty(selectedVersion) &&
                    customDateFormat(selectedVersion.policyEvaluationTimestamps.latestPolicyEvaluationTime)}
                </NxTable.Cell>
              </NxTable.Row>
              <NxTable.Row id="quarantineTime">
                <NxTable.Cell>Quarantined</NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {customDateFormat(currentVersion.policyEvaluationTimestamps.quarantineTime)}
                </NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {!isEmpty(selectedVersion) &&
                    customDateFormat(selectedVersion.policyEvaluationTimestamps.quarantineTime)}
                </NxTable.Cell>
              </NxTable.Row>
              <NxTable.Row id="unquarantineTime">
                <NxTable.Cell>Released from Quarantine</NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {getUnquarantineStatusText(currentVersion.policyEvaluationTimestamps)}
                </NxTable.Cell>
                <NxTable.Cell className="visual-testing-ignore">
                  {!isEmpty(selectedVersion) && getUnquarantineStatusText(selectedVersion.policyEvaluationTimestamps)}
                </NxTable.Cell>
              </NxTable.Row>
            </>
          )}
        </NxTable.Body>
      </NxTable>
    </section>
  );
};

CompareVersions.propTypes = {
  currentVersion: versionComparisonInfoPropType.isRequired,
  selectedVersion: versionComparisonInfoPropType.isRequired,
  loading: PropTypes.bool,
};

function renderIntegrityRating(integrityRating) {
  if (integrityRating == null) {
    return null;
  }

  const classes = classnames({ 'iq-integrity-rating-suspicious': integrityRating.id === 1 });
  return <span className={classes}>{integrityRating.label}</span>;
}

function renderHygieneRating(hygieneRating) {
  if (hygieneRating == null) {
    return null;
  }

  const { id, label } = hygieneRating;
  return (
    <Fragment>
      {id === 1 && <NxFontAwesomeIcon icon={faTrophy} className="iq-hygiene-rating-exemplar" />}
      {id === 4 && <NxFontAwesomeIcon icon={faExclamationTriangle} className="iq-hygiene-rating-laggard" />}
      <span>{label}</span>
    </Fragment>
  );
}

// this can be reused for Security, Legal, Quality and Other violation threat
function renderHighestPolicyThreat(highestPolicyThreat, numberOfViolatedPolicies) {
  if (highestPolicyThreat == null) {
    return null;
  }

  if (highestPolicyThreat === 'None') {
    return 'None';
  }

  const counterAttrs = {};

  if (highestPolicyThreat > 7) {
    counterAttrs.criticalCount = highestPolicyThreat;
  } else if (highestPolicyThreat <= 7 && highestPolicyThreat > 3) {
    counterAttrs.severeCount = highestPolicyThreat;
  } else if (highestPolicyThreat <= 3 && highestPolicyThreat > 1) {
    counterAttrs.moderateCount = highestPolicyThreat;
  } else if (highestPolicyThreat === 1) {
    counterAttrs.lowCount = highestPolicyThreat;
  } else if (highestPolicyThreat === 0) {
    counterAttrs.noneCount = highestPolicyThreat;
  }

  return (
    <Fragment>
      <NxSmallThreatCounter {...counterAttrs} />
      {numberOfViolatedPolicies > 1 && <div>within {numberOfViolatedPolicies} policies</div>}
    </Fragment>
  );
}

function renderHighestPolicyThreatByCategory(threatByCategory, category) {
  if (threatByCategory == null) {
    return null;
  }

  const maxThreat = threatByCategory[category];

  return renderHighestPolicyThreat(maxThreat ?? 'None');
}

function renderEffectiveLicenses({ effectiveLicenses, effectiveLicenseStatus }) {
  if (effectiveLicenses == null) {
    return null;
  }

  const tagColor = effectiveLicenseStatus === 'Overridden' ? 'purple' : 'indigo';
  return (
    <Fragment>
      <div>{effectiveLicenses}</div>
      {effectiveLicenseStatus && (
        <NxTag className="iq-compare-versions__license-status" color={tagColor}>
          {effectiveLicenseStatus}
        </NxTag>
      )}
    </Fragment>
  );
}
