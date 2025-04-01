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
  NxTooltip,
  NxTextLink,
  NxFontAwesomeIcon,
  NxButton,
} from '@sonatype/react-shared-components';
import { faStar } from '@fortawesome/sharp-solid-svg-icons';
import classnames from 'classnames';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './slices/prioritiesPageSlice';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import {
  selectIsDeveloperBulkRecommendationsEnabled,
  selectIsManualPullRequestEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
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
import { selectReportStageId } from 'MainRoot/applicationReport/applicationReportSelectors';
import PolicyActionTag from 'MainRoot/react/PolicyActionTag';

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

const manualPullRequestHiddenReasons = [
  'UNSUPPORTED_STAGE',
  'UNSUPPORTED_DEPENDENCY_TYPE',
  'UNSUPPORTED_FORMAT',
  'REMEDIATION_EVENT_EXISTS',
  'NO_REMEDIATION_VERSION_AVAILABLE',
];

export default function PrioritiesPageRow({ component, href }) {
  const dispatch = useDispatch();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { recommendations } = useSelector(selectPrioritiesPageSlice);
  const stageId = useSelector(selectReportStageId);

  const isDeveloperBulkRecommendationsEnabled = useSelector(selectIsDeveloperBulkRecommendationsEnabled);
  const isManualPullRequestEnabled = useSelector(selectIsManualPullRequestEnabled);

  const {
    displayName,
    dependencyType,
    action,
    priority,
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
  const automatedRemediationStatus = recommendations[componentHash]?.automatedRemediationStatus;
  const isManualPullRequestVisible =
    automatedRemediationStatus && !manualPullRequestHiddenReasons.includes(automatedRemediationStatus.reason);
  const manualPullRequestDisabledTooltip = automatedRemediationStatus?.reason
    ? automatedRemediationStatus?.reason === 'SCM_NOT_CONFIGURED'
      ? 'Source Control is not configured'
      : 'Manual Pull Requests are disabled'
    : null;

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
    <NxTableRow data-analytics-id="sonatype-developer-priorities-page-component-row">
      <NxTableCell className="nx-cell--num iq-priorities-table__priority">{priority}</NxTableCell>
      <NxTableCell className="iq-priorities-table__component">
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">
            {formattedDependencyType === dependencyTypeMap.Unknown ? null : (
              <DependencyIndicator type={formattedDependencyType} />
            )}
            <NxTextLink href={href}>{displayName}</NxTextLink>
          </div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell className="iq-priorities-table__build-action">
        <PolicyActionTag action={policyAction} />
      </NxTableCell>
      <NxTableCell className="iq-priorities-table__reachability">
        {securityReachable ? 'Detected' : 'Not detected'}
      </NxTableCell>
      <NxTableCell>
        <Recommendation
          loading={loading}
          error={error}
          isUnknown={isUnknown}
          actualVersion={actualVersion}
          recommendation={recommendation}
        />
      </NxTableCell>
      {isManualPullRequestEnabled && (
        <NxTableCell>
          {isManualPullRequestVisible ? (
            <NxTooltip title={manualPullRequestDisabledTooltip} placement="top-end">
              <NxButton
                className={classnames('nx-btn--small', {
                  disabled: manualPullRequestDisabledTooltip,
                })}
              >
                Create PR
              </NxButton>
            </NxTooltip>
          ) : (
            '—'
          )}
        </NxTableCell>
      )}
    </NxTableRow>
  );
}

function Recommendation({ loading, error, isUnknown, actualVersion, recommendation }) {
  if (loading) {
    return <NxLoadingSpinner />;
  }

  if (error || isUnknown || !recommendation?.version || actualVersion === recommendation?.version) {
    return <span>Investigate</span>;
  }

  return (
    <NxTooltip title={recommendationTypeMap[recommendation?.type]}>
      <div className="iq-priorities-table__recommendation">
        <span>Upgrade to {recommendation?.version}</span>
        {recommendation?.isGolden && (
          <NxFontAwesomeIcon aria-hidden="false" aria-label="Golden Version" className="iq-golden-star" icon={faStar} />
        )}
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
    priority: PropTypes.number.isRequired,
  }).isRequired,
  href: PropTypes.string.isRequired,
};
