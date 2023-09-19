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
  NxH3,
  NxP,
  NxSmallThreatCounter,
  NxTextLink,
  NxTile,
} from '@sonatype/react-shared-components';
import useGetIntegrationsLink from 'MainRoot/integrations/useGetIntegrationsLink';
export default function ReportStatusBar(props) {
  const getReportProp = (propName) => propOr(0, propName, props);

  const criticalViolationCount = getReportProp('criticalViolationCount');
  const severeViolationCount = getReportProp('severeViolationCount');
  const moderateViolationCount = getReportProp('moderateViolationCount');
  const nonLowViolationCount = getReportProp('nonLowViolationCount');
  const policyComponentCount = getReportProp('policyComponentCount');
  const totalArtifactCount = getReportProp('totalArtifactCount');
  const knownArtifactCount = getReportProp('knownArtifactCount');
  const legacyPolicyViolationsCount = getReportProp('grandfatheredPolicyViolationCount');
  const quarantinedComponentCount = getReportProp('quarantinedComponentCount');
  const { totalApplicationRisk, isDeveloperDashboardEnabled } = props;
  const risk = !isNil(totalApplicationRisk) && totalApplicationRisk >= 0 ? totalApplicationRisk : 'N/A';

  const showSectionDefault = (propName) => propOr(true, propName, props);
  const hideSectionDefault = (propName) => propOr(false, propName, props);
  const showLegacyViolationsSection = showSectionDefault('showGrandfatheredSection');
  const showQuarantinedSection = hideSectionDefault('showQuarantinedSection');

  const coveragePercent = () => {
    if (knownArtifactCount !== 0 && totalArtifactCount !== 0) {
      return Math.round((100 * knownArtifactCount) / totalArtifactCount);
    }
    return 0;
  };

  const pluralTermination = (components) => (components === 1 ? '' : 's');
  const developerDashboardHref = useGetIntegrationsLink('overview');

  return (
    <NxTile>
      <NxTile.Content>
        {isDeveloperDashboardEnabled && (
          <div className="iq-app-risk-score-container">
            <NxH3>Application Total Risk Score</NxH3>
            <div className="iq-app-risk-score-row">
              <div className="iq-app-risk-score-row__risk" data-testid="iq-app-risk-score">
                {risk}
              </div>
              <NxP className="iq-app-risk-score-row__description">
                Application risk score is the aggregate threat score of your application&apos;s policy violations. It
                indicates the total risk found in the latest scan. Sonatype integrations can help to lower your
                application risk score by providing insights based on your application security.{' '}
                <NxTextLink href={developerDashboardHref}>Learn more.</NxTextLink>
              </NxP>
            </div>
          </div>
        )}
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
        </div>
      </NxTile.Content>
    </NxTile>
  );
}

ReportStatusBar.propTypes = {
  knownArtifactCount: PropTypes.number,
  totalArtifactCount: PropTypes.number,
  policyComponentCount: PropTypes.number,
  grandfatheredPolicyViolationCount: PropTypes.number,
  criticalViolationCount: PropTypes.number,
  severeViolationCount: PropTypes.number,
  moderateViolationCount: PropTypes.number,
  nonLowViolationCount: PropTypes.number,
  showGrandfatheredSection: PropTypes.bool,
  showQuarantinedSection: PropTypes.bool,
  totalApplicationRisk: PropTypes.oneOfType([PropTypes.number, PropTypes.string]),
  isDeveloperDashboardEnabled: PropTypes.bool,
};
