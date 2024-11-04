/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useMemo } from 'react';
import { NxLoadingSpinner, NxTable, NxTag, NxThreatIndicator } from '@sonatype/react-shared-components';
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
import { isNilOrEmpty } from '../../util/jsUtil';
import { selectReportStageId } from 'MainRoot/applicationReport/applicationReportSelectors';

export const dependencyTypeMap = {
  Direct: 'direct',
  Transitive: 'transitive',
  'Inner Source': 'inner-source',
  Unknown: 'unknown',
};

export default function PrioritiesPageRow({ component, onClick, index }) {
  const dispatch = useDispatch();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { recommendations } = useSelector(selectPrioritiesPageSlice);
  const stageId = useSelector(selectReportStageId);

  const isDeveloperBulkRecommendationsEnabled = useSelector(selectIsDeveloperBulkRecommendationsEnabled);

  const {
    displayName,
    dependencyType,
    action,
    highestThreat,
    highestThreatPolicyName,
    highestThreatPolicyConstraintName,
    securityReachable,
    componentIdentifier,
    componentHash,
    pathName,
    remediationType,
    remediationVersion,
  } = component;

  const policyAction = action === 'none' ? null : action;
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
    <NxTable.Row isClickable onClick={onClick} data-analytics-id="sonatype-developer-priorities-page-component-row">
      <NxTable.Cell className="iq-priorities-page-priority">{index}</NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-components">
          <div className="iq-priorities-page-components__component">{displayName}</div>
          <div className="iq-priorities-page-components__detail">
            <span data-testid="dependency-type">
              {formattedDependencyType === dependencyTypeMap.Unknown ? null : (
                <DependencyIndicator type={formattedDependencyType} />
              )}
            </span>
            {securityReachable ? (
              <NxTag className="iq-priorities-page-components__detail-tag">Security-Reachable</NxTag>
            ) : null}
          </div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-policy-details">
          <div className="iq-priorities-page-policy-details__desc">
            <NxThreatIndicator
              className="iq-priorities-page-policy-details__desc-threat-indicator"
              policyThreatLevel={highestThreat}
            />
            <span className="iq-priorities-page-policy-details__desc-threat">{highestThreat}</span>

            {policyAction && (
              <span className={`iq-priorities-page-policy-details__desc-policy-action ${policyAction}`}>
                {policyAction}
              </span>
            )}
          </div>
          <div className="iq-priorities-page-policy-details__constraint">{highestThreatPolicyConstraintName}</div>
          <div className="iq-priorities-page-policy-details__policy">{highestThreatPolicyName}</div>
        </div>
      </NxTable.Cell>
      <NxTable.Cell>
        <div className="iq-priorities-page-remediation">
          <Recommendation
            loading={loading}
            error={error}
            isUnknown={isUnknown}
            actualVersion={actualVersion}
            recommendation={recommendation}
          />
        </div>
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
}

function Recommendation({ loading, error, isUnknown, actualVersion, recommendation }) {
  if (loading) {
    return <NxLoadingSpinner />;
  }

  if (error || isUnknown) {
    return <div className="iq-priorities-page-remediation__upgrade">No recommendation available</div>;
  }

  const { version, text, type } = recommendation || {};

  const recommendationText = !version || actualVersion === version ? null : `Upgrade to ${version}`;
  let recommendationSubtext = !text ? '' : text;

  if (type === RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES) {
    recommendationSubtext = 'Non-breaking upgrade resolving issues for this component and its dependencies';
  } else if (type === RECOMMENDED_NON_BREAKING) {
    recommendationSubtext = 'Non-breaking upgrade resolving issues for this component';
  }

  if (isNilOrEmpty(recommendationText)) {
    return <div className="iq-priorities-page-remediation__upgrade">{recommendationSubtext}</div>;
  }

  return (
    <>
      <div className="iq-priorities-page-remediation__upgrade">{recommendationText}</div>
      <div className="iq-priorities-page-remediation__upgrade-desc">{recommendationSubtext}</div>
    </>
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
  index: PropTypes.number.isRequired,
};
