/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxSmallTag,
  NxTableCell,
  NxTableRow,
  NxThreatIndicator,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faCheck, faHistory } from '@fortawesome/pro-solid-svg-icons';

import ComponentDisplay from 'MainRoot/ComponentDisplay/ReactComponentDisplay';
import { DependencyIndicators } from './DependencyIndicators';
import { selectIsAggregated, selectSelectedReport } from './applicationReportSelectors';
import { allPass, filter, includes, length, not, compose, pathOr, prop, is } from 'ramda';

import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import { selectIsAutoWaiversEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function ReportTableRow({ onClick, component }) {
  const selectedReport = useSelector(selectSelectedReport);
  const isAggregated = useSelector(selectIsAggregated);
  const getTransitiveViolationsCount = () => {
    const transitiveComponentViolations = compose(
      length,
      filter(
        allPass([
          prop('policyThreatLevel'),
          compose(not, prop('waived')),
          compose(not, prop('legacyViolation')),
          compose(includes(component.serializedComponentIdentifier), pathOr('', ['dependencyInfo', 'rootAncestors'])),
        ])
      )
    )(selectedReport.allEntries);
    return `${transitiveComponentViolations} transitive violation${transitiveComponentViolations === 1 ? '' : 's'}`;
  };
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  return (
    <NxTableRow isClickable onClick={onClick}>
      <NxTableCell className="iq-app-report__threat-cell">
        <NxThreatIndicator policyThreatLevel={component.policyThreatLevel} />
        <span className="nx-threat-number">{component.policyThreatLevel}</span>
      </NxTableCell>
      <NxTableCell className="iq-app-report__policy-name-cell">
        <NxOverflowTooltip>
          <div className="iq-app-report__policy-name-text">{component.policyName}</div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-app-report__component-name-cell">
        {isAggregated && component.innerSource && (
          <span className="iq-transitive-violations-count iq-text-indicator iq-pull-right">
            {getTransitiveViolationsCount()}
          </span>
        )}
        {component.waived && !is(Number, component.waivedViolations) && (
          <span className="iq-text-indicator iq-text-indicator--waived iq-pull-right">
            <span>Waived</span>
            <NxFontAwesomeIcon icon={faCheck} />
          </span>
        )}
        {component.waivedViolations > 0 && (
          <NxTooltip title="Toggle off aggregate view to see all violations">
            <span className="iq-pull-right">
              <ActiveWaiversIndicator
                activeWaiverCount={component.waivedViolations}
                isFromAggregatedView={isAggregated}
              />
            </span>
          </NxTooltip>
        )}
        {isAutoWaiversEnabled && component.waivedWithAutoWaiver && (
          <span className="iq-app-report__auto-waiver-tag iq-text-indicator iq-pull-right">
            <NxSmallTag color="green">Auto</NxSmallTag>
          </span>
        )}
        {component.legacyViolation && (
          <span className="iq-text-indicator iq-text-indicator--legacy-violation iq-pull-right">
            <span>Legacy</span>
            <NxFontAwesomeIcon icon={faHistory} />
          </span>
        )}
        <div className="iq-app-report__truncate-wrapper">
          <DependencyIndicators component={component} />
          <ComponentDisplay component={component} truncate />
        </div>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

ReportTableRow.propTypes = {
  component: PropTypes.shape({
    derivedComponentName: PropTypes.string,
    policyName: PropTypes.string,
    hash: PropTypes.string,
    derivedDependencyType: PropTypes.string,
    waived: PropTypes.bool,
    legacyViolation: PropTypes.bool,
    policyThreatLevel: PropTypes.number,
    componentIdentifier: PropTypes.object,
    isOnlyInnerSourceTransitiveDependency: PropTypes.bool,
    innerSource: PropTypes.bool,
    waivedViolations: PropTypes.number,
    waivedWithAutoWaiver: PropTypes.bool,
    serializedComponentIdentifier: PropTypes.string,
  }),
  onClick: PropTypes.func.isRequired,
};
