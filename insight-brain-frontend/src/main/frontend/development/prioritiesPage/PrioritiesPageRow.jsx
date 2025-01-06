/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useMemo } from 'react';
import {
  NxLoadingSpinner,
  NxOverflowTooltip,
  NxTableRow,
  NxTableCell,
  NxThreatIndicator,
  NxTooltip,
} from '@sonatype/react-shared-components';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './slices/prioritiesPageSlice';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { selectIsDeveloperBulkRecommendationsEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  getAsyncRecommendationsPrioritiesPage,
  getRecommendationsPrioritiesPage,
  NEXT_NO_VIOLATIONS,
  NEXT_NO_VIOLATIONS_DEPENDENCIES,
  NEXT_NON_FAILING,
  NEXT_NON_FAILING_DEPENDENCIES,
  RECOMMENDED_NON_BREAKING,
  RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
} from '../../componentDetails/overview/riskRemediation/recommendedVersionsUtils';
import PropTypes from 'prop-types';
import { selectReportStageId } from 'MainRoot/applicationReport/applicationReportSelectors';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';
import GoldenStar from 'MainRoot/img/golden-star.svg';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';

export const dependencyTypeMap = {
  Direct: 'direct',
  Transitive: 'transitive',
  'Inner Source': 'inner-source',
  Unknown: 'unknown',
};

export const recommendationTypeMap = {
  'next-no-violations': 'Next non-violating version',
  'next-no-violations-with-dependencies': 'Next non-violating with dependencies version',
  'next-non-failing': 'Next non-failing version',
  'next-non-failing-with-dependencies': 'Next non-failing with dependencies version',
  'recommended-non-breaking': 'Recommended non-breaking version',
  'recommended-non-breaking-with-dependencies': 'Recommended non-breaking with dependencies version',
};

export default function PrioritiesPageRow({ component, onClick, hasPolicyAction }) {
  const dispatch = useDispatch();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { recommendations } = useSelector(selectPrioritiesPageSlice);
  const stageId = useSelector(selectReportStageId);

  const isDeveloperBulkRecommendationsEnabled = useSelector(selectIsDeveloperBulkRecommendationsEnabled);

  const metadata = useSelector(selectApplicationReportMetaData);
  const { forMonitoring } = metadata || {};

  const {
    displayName,
    dependencyType,
    action,
    highestThreat,
    priority,
    highestThreatPolicyName,
    securityReachable,
    componentIdentifier,
    componentHash,
    pathName,
    remediationType,
    remediationVersion,
  } = component;

  const policyAction = action === 'none' ? '' : action;
  const formattedDependencyType = dependencyTypeMap[dependencyType];
  const actualVersion = componentIdentifier?.coordinates?.version;
  const isUnknown = formattedDependencyType === dependencyTypeMap.Unknown && componentIdentifier === null;

  const loading = isDeveloperBulkRecommendationsEnabled ? false : recommendations[componentHash]?.loading;
  const error = isDeveloperBulkRecommendationsEnabled ? null : recommendations[componentHash]?.error;
  const remediation = isDeveloperBulkRecommendationsEnabled ? null : recommendations[componentHash]?.remediation;

  const recommendation = isDeveloperBulkRecommendationsEnabled
    ? useMemo(() => getRecommendationsPrioritiesPage(remediationType, remediationVersion, actualVersion, stageId), [
        remediationType,
        remediationVersion,
        stageId,
      ])
    : useMemo(() => getAsyncRecommendationsPrioritiesPage(remediation, actualVersion, stageId), [
        remediation,
        actualVersion,
        stageId,
      ]);

  const doLoad = () => {
    const requestData = {
      clientType: 'ci',
      ownerType: 'application',
      ownerId: publicAppId,
      matchState: 'exact',
      proprietary: 'false',
      identificationSource: 'Sonatype',
      componentIdentifier: componentIdentifier ? stringifyComponentIdentifier(componentIdentifier, 'exact') : null,
      hash: componentHash,
      scanId,
      pathName,
      displayName,
      stageId,
      dependencyType: formattedDependencyType,
    };

    dispatch(actions.checkIfLoadRecommendationsNeeded(requestData));
  };

  useEffect(() => {
    if (!isDeveloperBulkRecommendationsEnabled) {
      doLoad();
    }
  }, [isDeveloperBulkRecommendationsEnabled]);

  return (
    <NxTableRow isClickable onClick={onClick} data-analytics-id="sonatype-developer-priorities-page-component-row">
      <NxTableCell className="iq-priorities-page-priority">{priority}</NxTableCell>
      <NxTableCell>
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">
            <span className="iq-priorities-page-dependency-indicator" data-testid="dependency-type">
              {formattedDependencyType === dependencyTypeMap.Unknown ? null : (
                <DependencyIndicator type={formattedDependencyType} />
              )}
            </span>
            <span className="iq-priorities-page-components__component">{displayName}</span>
          </div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell>
        <span className="iq-priorities-page-policy-details">
          <span
            className={
              forMonitoring || !hasPolicyAction
                ? 'iq-priorities-page-policy-details__desc-ignore-policy-action'
                : 'iq-priorities-page-policy-details__desc'
            }
          >
            <span className={`iq-priorities-page-policy-details__desc-policy-action ${policyAction}`}>
              {policyAction}
            </span>
            <NxThreatIndicator
              className="iq-priorities-page-policy-details__desc-threat-indicator"
              policyThreatLevel={highestThreat}
            />
            <span className="iq-priorities-page-policy-details__desc-threat">{highestThreat}</span>
            <NxOverflowTooltip>
              <span className="iq-priorities-page-policy-details__policy nx-truncate-ellipsis">
                {highestThreatPolicyName}
              </span>
            </NxOverflowTooltip>

            {securityReachable && <ReachabilityStatus reachabilityStatus={'REACHABLE'} />}
          </span>
        </span>
      </NxTableCell>
      <NxTableCell>
        <div className="iq-priorities-page-remediation">
          <Recommendation
            loading={loading}
            error={error}
            isUnknown={isUnknown}
            actualVersion={actualVersion}
            recommendation={recommendation}
          />
        </div>
      </NxTableCell>
      <NxTableCell chevron />
    </NxTableRow>
  );
}

function Recommendation({ loading, error, isUnknown, actualVersion, recommendation }) {
  if (loading) {
    return <NxLoadingSpinner />;
  }

  if (error || isUnknown || !recommendation?.version || actualVersion === recommendation?.version) {
    return <span>-</span>;
  }

  return (
    <NxTooltip title={recommendationTypeMap[recommendation?.type]}>
      <div className="iq-suggested-fix">
        <span>{recommendation?.version}</span>{' '}
        {recommendation?.isGolden && <img src={GoldenStar} className="iq-golden-star" />}
      </div>
    </NxTooltip>
  );
}

Recommendation.propTypes = {
  loading: PropTypes.bool,
  error: PropTypes.string,
  isUnknown: PropTypes.bool,
  actualVersion: PropTypes.string,
  recommendation: PropTypes.shape({
    id: PropTypes.string.isRequired,
    text: PropTypes.string.isRequired,
    type: PropTypes.oneOf([
      NEXT_NO_VIOLATIONS,
      NEXT_NO_VIOLATIONS_DEPENDENCIES,
      NEXT_NON_FAILING,
      NEXT_NON_FAILING_DEPENDENCIES,
      RECOMMENDED_NON_BREAKING,
      RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
    ]),
    version: PropTypes.string,
    linkId: PropTypes.string,
    linkText: PropTypes.string,
    isGolden: PropTypes.bool,
  }),
};

PrioritiesPageRow.propTypes = {
  component: PropTypes.shape({
    displayName: PropTypes.string.isRequired,
    dependencyType: PropTypes.string.isRequired,
    action: PropTypes.string.isRequired,
    highestThreat: PropTypes.number.isRequired,
    priority: PropTypes.number.isRequired,
    highestThreatPolicyName: PropTypes.string,
    highestThreatPolicyConstraintName: PropTypes.string,
  }).isRequired,
  onClick: PropTypes.func.isRequired,
  hasPolicyAction: PropTypes.bool,
};
