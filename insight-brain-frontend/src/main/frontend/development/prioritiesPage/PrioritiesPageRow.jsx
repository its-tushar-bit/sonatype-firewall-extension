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
import { getRemediationsPrioritiesPage } from '../../componentDetails/overview/riskRemediation/recommendedVersionsUtils';
import PropTypes from 'prop-types';
import { isNilOrEmpty } from '../../util/jsUtil';

const dependencyTypeMap = {
  Direct: 'direct',
  Transitive: 'transitive',
  'Inner Source': 'inner-source',
  Unknown: 'unknown',
};

export default function PrioritiesPageRow({ component, onClick }) {
  const dispatch = useDispatch();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const {
    recommendations,
    metadata: { stageId },
  } = useSelector(selectPrioritiesPageSlice);

  const {
    displayName,
    dependencyType,
    action,
    highestThreat,
    priority,
    highestThreatPolicyName,
    highestThreatPolicyConstraintName,
    securityReachable,
    componentIdentifier,
    componentHash,
    pathName,
  } = component;

  const policyAction = action === 'none' ? null : action;
  const formattedDependencyType = dependencyTypeMap[dependencyType];
  const actualVersion = componentIdentifier?.coordinates?.version;
  const isUnknown = formattedDependencyType === dependencyTypeMap.Unknown && componentIdentifier === null;

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

  const { loading, error, remediation } = recommendations[componentHash] || {};

  const recommendation = useMemo(() => getRemediationsPrioritiesPage(remediation, actualVersion, stageId), [
    remediation,
    actualVersion,
  ]);

  const recommendationText =
    !recommendation?.version || actualVersion === recommendation?.version
      ? null
      : `Upgrade to ${recommendation.version}`;
  const recommendationSubtext = !recommendation?.text ? '' : recommendation.text;

  const doLoad = () => {
    dispatch(actions.checkIfLoadRecommendationsNeeded(requestData));
  };

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxTable.Row isClickable onClick={onClick}>
      <NxTable.Cell className="iq-priorities-page-priority">{priority}</NxTable.Cell>
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
            recommendationText={recommendationText}
            recommendationSubtext={recommendationSubtext}
          />
        </div>
      </NxTable.Cell>
      <NxTable.Cell chevron />
    </NxTable.Row>
  );
}

function Recommendation({ loading, error, recommendationText, recommendationSubtext, isUnknown }) {
  if (loading) {
    return <NxLoadingSpinner />;
  }

  if (error || isUnknown) {
    return <div className="iq-priorities-page-remediation__upgrade">No recommendation available</div>;
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
  recommendationText: PropTypes.string,
  recommendationSubtext: PropTypes.string,
  isUnknown: PropTypes.bool,
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
};
