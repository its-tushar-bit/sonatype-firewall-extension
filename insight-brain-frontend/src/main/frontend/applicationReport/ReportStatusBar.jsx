/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import NxFontAwesomeIcon from '@sonatype/react-shared-components/components/NxFontAwesomeIcon/NxFontAwesomeIcon';
import { faHistory } from '@fortawesome/pro-solid-svg-icons';
import { isNil, propOr, toUpper } from 'ramda';
import {
  NxBinaryDonutChart,
  NxButton,
  NxH2,
  NxModal,
  NxSmallThreatCounter,
  NxTile,
  useToggle,
} from '@sonatype/react-shared-components';
export default function ReportStatusBar(props) {
  const getReportProp = (propName) => propOr(0, propName, props);

  const criticalViolationCount = getReportProp('criticalViolationCount');
  const severeViolationCount = getReportProp('severeViolationCount');
  const moderateViolationCount = getReportProp('moderateViolationCount');
  const nonLowViolationCount = getReportProp('nonLowViolationCount');
  const policyComponentCount = getReportProp('policyComponentCount');
  const totalArtifactCount = getReportProp('totalArtifactCount');
  const knownArtifactCount = getReportProp('knownArtifactCount');
  const legacyPolicyViolationsCount =
    getReportProp('legacyViolationCount') || getReportProp('grandfatheredPolicyViolationCount');
  const quarantinedComponentCount = getReportProp('quarantinedComponentCount');
  const { totalApplicationRisk, isDeveloperDashboardEnabled } = props;
  const risk = !isNil(totalApplicationRisk) && totalApplicationRisk >= 0 ? totalApplicationRisk : 'N/A';

  const showSectionDefault = (propName) => propOr(true, propName, props);
  const hideSectionDefault = (propName) => propOr(false, propName, props);
  const showLegacyViolationsSection = showSectionDefault('showLegacyViolationsSection');
  const showQuarantinedSection = hideSectionDefault('showQuarantinedSection');

  const coveragePercent = () => {
    if (knownArtifactCount !== 0 && totalArtifactCount !== 0) {
      return Math.round((100 * knownArtifactCount) / totalArtifactCount);
    }
    return 0;
  };

  const pluralTermination = (components) => (components === 1 ? '' : 's');
  const [showApplicationRiskScoreModal, toggleShowApplicationRiskScoreModal] = useToggle(false);
  return (
    <NxTile>
      <NxTile.Content>
        <div className="iq-indicator-row">
          <div className="iq-threat-indicators">
            <NxSmallThreatCounter
              criticalCount={criticalViolationCount || null}
              severeCount={severeViolationCount || null}
              moderateCount={moderateViolationCount || null}
            />
            <div className="iq-caption">
              <h3 className="iq-caption__text">
                {nonLowViolationCount} VIOLATION
                {toUpper(pluralTermination(nonLowViolationCount))}
              </h3>
              <p className="iq-caption__sub-text">
                Affecting {policyComponentCount} component
                {pluralTermination(policyComponentCount)}
              </p>
            </div>
          </div>
          <div className="iq-coverage-indicator">
            <NxBinaryDonutChart
              className="iq-report-status-bar__coverage-indicator-chart"
              percent={coveragePercent()}
              role="presentation"
            />
            <div className="iq-caption">
              <h3 className="iq-caption__text">
                {totalArtifactCount} COMPONENT{toUpper(pluralTermination(totalArtifactCount))}
              </h3>
              <p className="iq-caption__sub-text">{coveragePercent()}% of all components identified</p>
            </div>
          </div>
          {showLegacyViolationsSection && (
            <div className="iq-legacy-violations-indicator">
              <NxFontAwesomeIcon icon={faHistory} />
              <div className="iq-caption">
                <h3 className="iq-caption__text">
                  {`${legacyPolicyViolationsCount} `}
                  {legacyPolicyViolationsCount === 1 ? 'Legacy Violation' : 'Legacy Violations'}
                </h3>
              </div>
            </div>
          )}
          {showQuarantinedSection && (
            <div className="iq-quarantine-indicator">
              <div className="iq-caption">
                <h3 className="iq-caption__text">{quarantinedComponentCount} QUARANTINED</h3>
                <p className="iq-caption__sub-text">component{pluralTermination(quarantinedComponentCount)}</p>
              </div>
            </div>
          )}
          {isDeveloperDashboardEnabled && (
            <div className="iq-application-risk-score--container">
              <div className="iq-application-risk-score--content">
                <div
                  className="iq-application-risk-score--risk nx-small-threat-counter"
                  data-testid="iq-app-risk-score"
                >
                  {risk}
                </div>
                <div className="iq-application-risk-score--desc">
                  <div className="iq-application-risk-score--desc-title">APP RISK SCORE</div>
                  <button className="nx-text-link" onClick={toggleShowApplicationRiskScoreModal}>
                    Learn more
                  </button>
                </div>
              </div>
              {showApplicationRiskScoreModal && (
                <NxModal onCancel={toggleShowApplicationRiskScoreModal}>
                  <NxModal.Header>
                    <NxH2>Application Risk Score</NxH2>
                  </NxModal.Header>
                  <NxModal.Content>
                    Application risk score is the aggregate threat scores of your application's policy violations. It
                    indicates the total risk found in the latest scan. Sonatype integrations can help lower your
                    application risk score by providing insights based on your application security.
                  </NxModal.Content>
                  <footer className="nx-footer">
                    <div className="nx-btn-bar">
                      <NxButton onClick={toggleShowApplicationRiskScoreModal}>Close</NxButton>
                    </div>
                  </footer>
                </NxModal>
              )}
            </div>
          )}
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

ReportStatusBar.propTypes = {
  knownArtifactCount: PropTypes.number,
  totalArtifactCount: PropTypes.number,
  policyComponentCount: PropTypes.number,
  legacyViolationCount: PropTypes.number,
  grandfatheredPolicyViolationCount: PropTypes.number,
  criticalViolationCount: PropTypes.number,
  severeViolationCount: PropTypes.number,
  moderateViolationCount: PropTypes.number,
  nonLowViolationCount: PropTypes.number,
  showLegacyViolationsSection: PropTypes.bool,
  showQuarantinedSection: PropTypes.bool,
  totalApplicationRisk: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  isDeveloperDashboardEnabled: PropTypes.bool,
};
