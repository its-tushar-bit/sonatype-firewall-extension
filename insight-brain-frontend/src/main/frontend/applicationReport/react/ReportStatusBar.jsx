/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import NxFontAwesomeIcon from '@sonatype/react-shared-components/components/NxFontAwesomeIcon/NxFontAwesomeIcon';
import { faHistory } from '@fortawesome/pro-solid-svg-icons';
import { propOr } from 'ramda';
import LoadWrapper from '../../react/LoadWrapper';

export default function ReportStatusBar(props) {
  const { selectedReport, loadError } = props;

  const getReportProp = propName => propOr(0, propName, selectedReport);
  const criticalViolationCount = getReportProp('criticalViolationCount');
  const severeViolationCount = getReportProp('severeViolationCount');
  const moderateViolationCount = getReportProp('moderateViolationCount');
  const nonLowViolationCount = getReportProp('nonLowViolationCount');
  const policyComponentCount = getReportProp('policyComponentCount');
  const totalArtifactCount = getReportProp('totalArtifactCount');
  const knownArtifactCount = getReportProp('knownArtifactCount');
  const grandfatheredPolicyViolationCount = getReportProp('grandfatheredPolicyViolationCount');

  const coveragePercent = () => {
    if (knownArtifactCount !== 0 && totalArtifactCount !== 0) {
      return Math.round(100 * knownArtifactCount / totalArtifactCount);
    }
    return 0;
  };

  return (
    <LoadWrapper error={loadError}>
      <div className="nx-tile-content">
        <div className="iq-tile iq-tile--indicators">
          <div className="iq-tile-content iq-tile-content--indicators">
            <div className="iq-indicator-row">
              <div className="iq-threat-indicators">
                <div className="iq-threat-indicator nx-threat-bar--critical">{criticalViolationCount}</div>
                <div className="iq-threat-indicator nx-threat-bar--severe">{severeViolationCount}</div>
                <div className="iq-threat-indicator nx-threat-bar--moderate">{moderateViolationCount}</div>
                <div className="iq-caption">
                  <h3 className="iq-caption__text">{nonLowViolationCount} VIOLATION
                    {nonLowViolationCount === 1 ? '' : 'S'}
                  </h3>
                  <p className="iq-caption__sub-text">Affecting {policyComponentCount} component
                    {policyComponentCount === 1 ? '' : 's'}
                  </p>
                </div>
              </div>
              <div className="iq-coverage-indicator">
                <div className="iq-caption">
                  <h3 className="iq-caption__text">{totalArtifactCount} COMPONENTS</h3>
                  <p className="iq-caption__sub-text">{coveragePercent()}% of all components identified</p>
                </div>
              </div>
              <div className="iq-grandfathering-indicator">
                <NxFontAwesomeIcon icon={faHistory}/>
                <div className="iq-caption">
                  <h3 className="iq-caption__text">{grandfatheredPolicyViolationCount} Grandfathered</h3>
                  <p className="iq-caption__sub-text">violations</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </LoadWrapper>
  );
}

ReportStatusBar.propTypes = {
  selectedReport: PropTypes.shape({
    knownArtifactCount: PropTypes.number.isRequired,
    totalArtifactCount: PropTypes.number.isRequired,
    policyComponentCount: PropTypes.number.isRequired,
    grandfatheredPolicyViolationCount: PropTypes.number.isRequired,
    criticalViolationCount: PropTypes.number.isRequired,
    severeViolationCount: PropTypes.number.isRequired,
    moderateViolationCount: PropTypes.number.isRequired,
    nonLowViolationCount: PropTypes.number.isRequired
  }),
  loadError: PropTypes.object
};
