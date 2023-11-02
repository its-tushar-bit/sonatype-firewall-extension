/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { componentPropType } from './advancedLegalPropTypes';
import { terseAgo, timeAgo } from '../utilAngular/CommonServices';
import { EFFECTIVELY_UNSPECIFIED_LICENSES, STAGE_NAME_TO_DISPLAY, STAGE_NAME_TO_ID } from './advancedLegalConstants';
import { NxPolicyViolationIndicator, NxTextLink } from '@sonatype/react-shared-components';
import { inc, prop, reduceBy } from 'ramda';

export default function ComponentOverviewTile(props) {
  const { applicationPublicId, component, $state } = props;

  const licenseLegalData = component.licenseLegalData;
  const obligations = component.licenseLegalData.obligations;
  const attributions = component.licenseLegalData.attributions;

  const obligationCounts = {
    OPEN: 0,
    FLAGGED: 0,
    IGNORED: 0,
    FULFILLED: 0,
    ...reduceBy(inc, 0, prop('originalStatus'), obligations),
  };

  const reviewedCount = obligationCounts.FULFILLED + obligationCounts.IGNORED;

  const reviewStatus = () => {
    if (obligationCounts.FLAGGED > 0) {
      return 'Flagged';
    }
    if (obligations.length === 0) {
      return licenseLegalData.effectiveLicenses.length === 0 ||
        licenseLegalData.effectiveLicenses.every((license) => EFFECTIVELY_UNSPECIFIED_LICENSES.indexOf(license) !== -1)
        ? 'Unreviewed'
        : 'Complete';
    }
    if (reviewedCount === 0) {
      return 'Unreviewed';
    }
    if (reviewedCount === obligations.length) {
      return 'Complete';
    }
    return 'In Progress';
  };

  const lastUpdatedObligation =
    obligations.length === 0
      ? null
      : obligations.reduce((prev, current) =>
          (prev.lastUpdatedAt || 0) > (current.lastUpdatedAt || 0) ? prev : current
        );

  const lastUpdatedAttribution =
    attributions.length === 0
      ? null
      : attributions.reduce((prev, current) =>
          (prev.lastUpdatedAt || 0) > (current.lastUpdatedAt || 0) ? prev : current
        );

  const lastUpdated = (() => {
    let lastUpdatedByUsername = undefined;
    let lastUpdatedAt = 0;
    if (lastUpdatedObligation && lastUpdatedObligation.lastUpdatedAt > lastUpdatedAt) {
      lastUpdatedByUsername = lastUpdatedObligation.lastUpdatedByUsername;
      lastUpdatedAt = lastUpdatedObligation.lastUpdatedAt;
    }
    if (lastUpdatedAttribution && lastUpdatedAttribution.lastUpdatedAt > lastUpdatedAt) {
      lastUpdatedByUsername = lastUpdatedAttribution.lastUpdatedByUsername;
      lastUpdatedAt = lastUpdatedAttribution.lastUpdatedAt;
    }
    if (
      licenseLegalData.componentCopyrightLastUpdatedAt &&
      licenseLegalData.componentCopyrightLastUpdatedAt > lastUpdatedAt
    ) {
      lastUpdatedByUsername = licenseLegalData.componentCopyrightLastUpdatedByUsername;
      lastUpdatedAt = licenseLegalData.componentCopyrightLastUpdatedAt;
    }
    if (
      licenseLegalData.componentNoticesLastUpdatedAt &&
      licenseLegalData.componentNoticesLastUpdatedAt > lastUpdatedAt
    ) {
      lastUpdatedByUsername = licenseLegalData.componentNoticesLastUpdatedByUsername;
      lastUpdatedAt = licenseLegalData.componentNoticesLastUpdatedAt;
    }
    if (
      licenseLegalData.componentLicenseFilesLastUpdatedAt &&
      licenseLegalData.componentLicenseFilesLastUpdatedAt > lastUpdatedAt
    ) {
      lastUpdatedByUsername = licenseLegalData.componentLicenseFilesLastUpdatedByUsername;
      lastUpdatedAt = licenseLegalData.componentLicenseFilesLastUpdatedAt;
    }
    const lastUpdatedAtTimeAgo = timeAgo(lastUpdatedAt);
    return {
      lastUpdatedByUsername: lastUpdatedByUsername === undefined ? 'N/A' : lastUpdatedByUsername,
      lastUpdatedAt:
        lastUpdatedByUsername === undefined ? 'Never' : lastUpdatedAtTimeAgo.age + ' ' + lastUpdatedAtTimeAgo.qualifier,
    };
  })();

  const getStageScanHref = (stageScan) =>
    $state.href('applicationReport.policy', {
      publicId: applicationPublicId,
      scanId: stageScan.scanId,
    });

  const createStageScan = (stageScan) => (
    <span
      id={'component-overview-tile-' + STAGE_NAME_TO_ID[stageScan.stageName]}
      className="stage-scan"
      key={stageScan.stageName}
    >
      {stageScan.scanId ? (
        <NxTextLink href={getStageScanHref(stageScan)}>
          {STAGE_NAME_TO_DISPLAY[stageScan.stageName]} {terseAgo(stageScan.scanDate)}
        </NxTextLink>
      ) : (
        <span className="no-stage-scan">{STAGE_NAME_TO_DISPLAY[stageScan.stageName]}</span>
      )}
    </span>
  );

  return (
    <section id="component-overview-tile" className="nx-tile">
      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <div className="license-component-overview__review-status">
                <dt className="nx-read-only__label">Review Status</dt>
                <dd id="component-overview-tile-review-status" className="nx-read-only__data">
                  {reviewStatus()}
                </dd>
              </div>
              <div className="license-component-overview__last-modified">
                <dt className="nx-read-only__label">Last Modified</dt>
                <dd id="component-overview-tile-last-modified" className="nx-read-only__data">
                  {lastUpdated.lastUpdatedAt}
                </dd>
              </div>
              <div className="license-component-overview__modified-by">
                <dt className="nx-read-only__label">Modified By</dt>
                <dd id="component-overview-tile-modified-by" className="nx-read-only__data">
                  {lastUpdated.lastUpdatedByUsername}
                </dd>
              </div>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <div className="license-component-overview__review-progress">
                <dt className="nx-read-only__label">Review Progress</dt>
                <dd id="component-overview-tile-review-progress" className="nx-read-only__data">
                  {reviewedCount}/{obligations.length} complete
                </dd>
              </div>
              <div className="license-component-overview__fulfilled">
                <dt className="nx-read-only__label">Fulfilled</dt>
                <dd id="component-overview-tile-fulfilled" className="nx-read-only__data">
                  {obligationCounts.FULFILLED}
                </dd>
              </div>
              <div className="license-component-overview__flagged">
                <dt className="nx-read-only__label">Flagged</dt>
                <dd id="component-overview-tile-flagged" className="nx-read-only__data">
                  {obligationCounts.FLAGGED}
                </dd>
              </div>
              <div className="license-component-overview__not-applicable">
                <dt className="nx-read-only__label">Not Applicable</dt>
                <dd id="component-overview-tile-not-applicable" className="nx-read-only__data">
                  {obligationCounts.IGNORED}
                </dd>
              </div>
            </dl>
          </div>
          <div className="nx-grid-col">
            <dl className="nx-read-only">
              <div className="license-component-overview__highest-threat">
                <dt className="nx-read-only__label">Highest License Threat</dt>
                <dd id="component-overview-tile-highest-license-threat-group" className="nx-read-only__data">
                  {licenseLegalData.highestEffectiveLicenseThreatGroup ? (
                    <NxPolicyViolationIndicator
                      policyThreatLevel={licenseLegalData.highestEffectiveLicenseThreatGroup.licenseThreatGroupLevel}
                    >
                      {licenseLegalData.highestEffectiveLicenseThreatGroup.licenseThreatGroupName}
                    </NxPolicyViolationIndicator>
                  ) : (
                    'N/A'
                  )}
                </dd>
              </div>
              {applicationPublicId && (
                <div className="license-component-overview__stages">
                  <dt className="nx-read-only__label">Stages</dt>
                  <dd id="component-overview-tile-stages" className="nx-read-only__data">
                    {component.stageScans ? component.stageScans.map(createStageScan) : 'N/A'}
                  </dd>
                </div>
              )}
            </dl>
          </div>
        </div>
      </div>
    </section>
  );
}

ComponentOverviewTile.propTypes = {
  applicationPublicId: PropTypes.string,
  component: componentPropType,
  licenses: PropTypes.arrayOf(
    PropTypes.shape({
      licenseId: PropTypes.string.isRequired,
      licenseName: PropTypes.string.isRequired,
    })
  ),
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }).isRequired,
};
