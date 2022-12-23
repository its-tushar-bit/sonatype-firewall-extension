/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import NxFontAwesomeIcon from '@sonatype/react-shared-components/components/NxFontAwesomeIcon/NxFontAwesomeIcon';
import { faHistory } from '@fortawesome/pro-solid-svg-icons';
import { propOr, toUpper } from 'ramda';
import { NxBinaryDonutChart, NxSmallThreatCounter } from '@sonatype/react-shared-components';
export default function ReportStatusBar(props) {
  const getReportProp = (propName) => propOr(0, propName, props);

  const criticalViolationCount = getReportProp('criticalViolationCount');
  const severeViolationCount = getReportProp('severeViolationCount');
  const moderateViolationCount = getReportProp('moderateViolationCount');
  const nonLowViolationCount = getReportProp('nonLowViolationCount');
  const policyComponentCount = getReportProp('policyComponentCount');
  const totalArtifactCount = getReportProp('totalArtifactCount');
  const knownArtifactCount = getReportProp('knownArtifactCount');
  const grandfatheredPolicyViolationCount = getReportProp('grandfatheredPolicyViolationCount');
  const quarantinedComponentCount = getReportProp('quarantinedComponentCount');

  const showSectionDefault = (propName) => propOr(true, propName, props);
  const hideSectionDefault = (propName) => propOr(false, propName, props);
  const showGrandfatheredSection = showSectionDefault('showGrandfatheredSection');
  const showQuarantinedSection = hideSectionDefault('showQuarantinedSection');

  const coveragePercent = () => {
    if (knownArtifactCount !== 0 && totalArtifactCount !== 0) {
      return Math.round((100 * knownArtifactCount) / totalArtifactCount);
    }
    return 0;
  };

  const pluralTermination = (components) => (components === 1 ? '' : 's');

  return (
    <section className="nx-tile">
      <div className="nx-tile-content">
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
          {showGrandfatheredSection && (
            <div className="iq-grandfathering-indicator">
              <NxFontAwesomeIcon icon={faHistory} />
              <div className="iq-caption">
                <h3 className="iq-caption__text">{grandfatheredPolicyViolationCount} Grandfathered</h3>
                <p className="iq-caption__sub-text">violations</p>
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
      </div>
    </section>
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
};
