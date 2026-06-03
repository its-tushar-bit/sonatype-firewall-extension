/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */

import React, { useEffect, useMemo, useRef } from 'react';
import {
  NxFontAwesomeIcon,
  NxLoadingSpinner,
  NxOverflowTooltip,
  NxSmallTag,
  NxTableCell,
  NxTableRow,
  NxTextLink,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faStar } from '@fortawesome/sharp-solid-svg-icons';
import { faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { faQuestionCircle } from '@fortawesome/pro-regular-svg-icons';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './slices/prioritiesPageSlice';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import { stringifyComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { selectIsDeveloperBulkRecommendationsEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  getRecommendationsPrioritiesPage,
  NEXT_NO_VIOLATIONS,
  NEXT_NO_VIOLATIONS_DEPENDENCIES,
  NEXT_NON_FAILING,
  NEXT_NON_FAILING_DEPENDENCIES,
  RECOMMENDED_NON_BREAKING,
  RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
  INNER_SOURCE_LATEST_NON_BREAKING,
  INNER_SOURCE_LATEST,
} from '../../componentDetails/overview/riskRemediation/recommendedVersionsUtils';
import { selectReportStageId } from 'MainRoot/applicationReport/applicationReportSelectors';
import PolicyActionTag from 'MainRoot/react/PolicyActionTag';
import PRStatus from 'MainRoot/components/prStatus/PRStatus';
import Reachability from 'MainRoot/components/reachability/Reachability';
import CreatePRModal from 'MainRoot/manualPullRequest/CreatePRModal';

export const dependencyTypeMap = {
  Direct: 'direct',
  Transitive: 'transitive',
  'Inner Source': 'inner-source',
  'Inner Source Direct': 'direct',
  'Inner Source Transitive': 'transitive',
  Unknown: 'unknown',
};

export const isInnerSourceDependencyType = (dependencyType) => {
  return (
    dependencyType &&
    (dependencyType === 'Inner Source' ||
      dependencyType === 'Inner Source Direct' ||
      dependencyType === 'Inner Source Transitive')
  );
};

export const recommendationTypeMap = {
  'next-no-violations': 'Next non-violating version',
  'next-no-violations-with-dependencies': 'Next non-violating with dependencies version',
  'next-non-failing': 'Next non-failing version',
  'next-non-failing-with-dependencies': 'Next non-failing with dependencies version',
  'recommended-non-breaking': 'Recommended non-breaking version',
  'recommended-non-breaking-with-dependencies': 'Recommended non-breaking with dependencies version',
  'inner-source-latest-non-breaking': 'Latest non-breaking inner source version',
  'inner-source-latest': 'Latest inner source version',
};

export default function PrioritiesPageRow({
  component,
  componentHref,
  violationsHref,
  latestBuildPrioritiesHref,
  hasAutoWaiversConfigured,
}) {
  const dispatch = useDispatch();

  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { recommendations, visibleCreatePRModalComponentHash } = useSelector(selectPrioritiesPageSlice);
  const stageId = useSelector(selectReportStageId);

  const isDeveloperBulkRecommendationsEnabled = useSelector(selectIsDeveloperBulkRecommendationsEnabled);

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
    hasExpiredWaiver,
    hasSoonToExpireWaiver,
    isAllViolationsWaived,
    waiverExpirationDetails,
    waivedViolationsCount,
    hasAutoWaiver,
    hasSameViolationsOnMain,
  } = component;

  const policyAction = action === 'none' ? null : action;
  const formattedDependencyType = dependencyTypeMap[dependencyType];
  const actualVersion = componentIdentifier?.coordinates?.version;
  const isUnknown = formattedDependencyType === dependencyTypeMap.Unknown && componentIdentifier === null;

  const loading = isDeveloperBulkRecommendationsEnabled ? false : recommendations[componentHash]?.loading;
  const error = isDeveloperBulkRecommendationsEnabled ? null : recommendations[componentHash]?.error;
  const automatedRemediationStatus = recommendations[componentHash]?.automatedRemediationStatus;

  const isModalOpenForComponent = visibleCreatePRModalComponentHash === componentHash;

  const remediationObj = recommendations[componentHash]?.remediation;

  const bulkRecommendation = useMemo(
    () => getRecommendationsPrioritiesPage(remediationType, remediationVersion, actualVersion, stageId),
    [remediationType, remediationVersion, actualVersion, stageId]
  );

  let recommendation;
  if (isDeveloperBulkRecommendationsEnabled) {
    recommendation = bulkRecommendation;
  } else {
    recommendation = remediationObj?.type ? remediationObj : null;
  }

  const remediationStatusPollingRef = useRef(null);

  function startPRStatusPollingAndSaveReference(id, componentHash) {
    if (id == null || componentHash == null) return;

    remediationStatusPollingRef.current?.abort?.();
    remediationStatusPollingRef.current = dispatch(actions.startPRStatusPolling({ id, componentHash }));
  }

  const doLoad = async () => {
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
      actualVersion,
    };

    const response = await dispatch(actions.checkIfLoadRecommendationsNeeded(requestData));
    if (response?.payload?.[componentHash]?.automatedRemediationStatus) {
      const { payload } = response;
      const remediationStatus = payload[componentHash].automatedRemediationStatus;

      if (remediationStatus?.status === 'PULL_REQUEST_CREATION_PENDING' && remediationStatus?.id) {
        const { id } = remediationStatus;
        startPRStatusPollingAndSaveReference(id, componentHash);
      }
    }
  };

  useEffect(() => {
    if (!isDeveloperBulkRecommendationsEnabled) {
      doLoad();
    }

    return () => {
      remediationStatusPollingRef.current?.abort?.();
    };
  }, [isDeveloperBulkRecommendationsEnabled]);

  const openCreatePRModal = () => {
    dispatch(
      actions.openCreatePRModal({
        componentHash: componentHash,
        targetVersion: recommendation.version,
        isDirectDependency:
          formattedDependencyType === dependencyTypeMap.Direct ||
          formattedDependencyType === dependencyTypeMap['Inner Source Direct'],
      })
    );
  };

  const onPRCreated = (result) => {
    startPRStatusPollingAndSaveReference(result?.id, componentHash);
  };

  const onRetryCreatePR = async () => {
    const { payload } = await dispatch(
      actions.createPR({
        componentHash: componentHash,
        targetVersion: recommendation.version,
        isDirectDependency:
          formattedDependencyType === dependencyTypeMap.Direct ||
          formattedDependencyType === dependencyTypeMap['Inner Source Direct'],
      })
    );
    startPRStatusPollingAndSaveReference(payload?.data?.id, componentHash);
  };

  const getNextStep = () => {
    if (hasSameViolationsOnMain) {
      return (
        <NxTextLink href={latestBuildPrioritiesHref} className="iq-pr-status__view-violations-link">
          Go to Build stage
        </NxTextLink>
      );
    }

    return (
      <PRStatus
        automatedRemediationStatus={automatedRemediationStatus}
        onCreatePR={openCreatePRModal}
        onRetry={onRetryCreatePR}
        defaultContent={
          loading ? (
            <NxLoadingSpinner />
          ) : (
            <NxTextLink href={violationsHref} className="iq-pr-status__view-violations-link">
              View Violations
            </NxTextLink>
          )
        }
      />
    );
  };

  return (
    <NxTableRow data-analytics-id="sonatype-developer-priorities-page-component-row">
      <NxTableCell className="nx-cell--num iq-priorities-table__priority">{priority}</NxTableCell>
      <NxTableCell className="iq-priorities-table__component">
        <NxOverflowTooltip>
          <div className="nx-truncate-ellipsis">
            {formattedDependencyType === dependencyTypeMap.Unknown ? null : (
              <>
                <DependencyIndicator type={formattedDependencyType} />
                {isInnerSourceDependencyType(dependencyType) && <DependencyIndicator type="inner-source" />}
              </>
            )}
            <NxTextLink href={componentHref}>{displayName}</NxTextLink>
          </div>
        </NxOverflowTooltip>
      </NxTableCell>
      <NxTableCell>
        <BuildAction
          action={policyAction}
          hasExpiredWaiver={hasExpiredWaiver}
          hasSoonToExpireWaiver={hasSoonToExpireWaiver}
          isAllViolationsWaived={isAllViolationsWaived}
          waiverExpirationDetails={waiverExpirationDetails}
        />
      </NxTableCell>
      <NxTableCell className="iq-priorities-table__reachability">
        <Reachability reachable={securityReachable} />
      </NxTableCell>
      <NxTableCell>
        <Recommendation
          loading={loading}
          error={error}
          hasAutoWaiversConfigured={hasAutoWaiversConfigured}
          isUnknown={isUnknown}
          actualVersion={actualVersion}
          recommendation={recommendation}
          reachable={securityReachable}
          isAllViolationsWaived={isAllViolationsWaived}
          waivedViolationsCount={waivedViolationsCount}
          hasAutoWaiver={hasAutoWaiver}
          hasSameViolationsOnMain={hasSameViolationsOnMain}
        />
      </NxTableCell>
      <NxTableCell>
        {getNextStep()}
        <RowCreatePRModal visible={isModalOpenForComponent} onPRCreated={onPRCreated} />
      </NxTableCell>
    </NxTableRow>
  );
}

function RowCreatePRModal({ visible, onPRCreated }) {
  return visible ? <CreatePRModal onSuccess={onPRCreated} /> : null;
}

function BuildAction({
  action,
  hasExpiredWaiver,
  hasSoonToExpireWaiver,
  isAllViolationsWaived,
  waiverExpirationDetails,
}) {
  return (
    <div className="iq-priorities-table__build-action">
      {isAllViolationsWaived === true ? <span>Waived</span> : <PolicyActionTag action={action} />}
      {waiverExpirationDetails && (
        <NxTooltip title={waiverExpirationDetails}>
          {action && hasExpiredWaiver ? (
            <NxFontAwesomeIcon className="iq-expired-waiver-icon" icon={faQuestionCircle} />
          ) : isAllViolationsWaived && hasSoonToExpireWaiver ? (
            <NxFontAwesomeIcon className="iq-soon-to-expire-waiver-icon" icon={faExclamationTriangle} />
          ) : (
            <div></div>
          )}
        </NxTooltip>
      )}
    </div>
  );
}

function Recommendation({
  loading,
  error,
  hasAutoWaiversConfigured,
  isUnknown,
  actualVersion,
  recommendation,
  reachable,
  isAllViolationsWaived,
  waivedViolationsCount,
  hasAutoWaiver,
  hasSameViolationsOnMain,
}) {
  const nudgeAutoWaiverText = 'Ask an administrator to configure Automated Waivers';
  const waivedViolationsCountText =
    waivedViolationsCount === 1
      ? `${waivedViolationsCount} waived violation`
      : `${waivedViolationsCount} waived violations`;
  const hasUpgradePath = recommendation?.version && actualVersion !== recommendation?.version;
  const shouldWaiveViolations = reachable !== true && !hasUpgradePath && !error && !isUnknown;
  const shouldInvestigate = error || isUnknown || !hasUpgradePath;

  if (loading) {
    return <NxLoadingSpinner />;
  }

  if (isAllViolationsWaived) {
    return (
      <div>
        <span>{waivedViolationsCountText}</span>
        {hasAutoWaiver && (
          <NxSmallTag color="green" className="iq-waiver-indicator-auto-tag">
            Auto
          </NxSmallTag>
        )}
      </div>
    );
  }

  if (hasSameViolationsOnMain) {
    return (
      <div className="iq-priorities-table__recommendation">
        <span>Resolve on default branch</span>
      </div>
    );
  }

  if (shouldWaiveViolations) {
    return hasAutoWaiversConfigured ? (
      <div className="iq-priorities-table__recommendation">
        <span>Waive violations</span>
      </div>
    ) : (
      <NxTooltip title={nudgeAutoWaiverText}>
        <div className="iq-priorities-table__recommendation">
          <span>Waive violations</span>
        </div>
      </NxTooltip>
    );
  }

  if (shouldInvestigate) {
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
  hasAutoWaiversConfigured: PropTypes.bool,
  isUnknown: PropTypes.bool,
  actualVersion: PropTypes.string,
  recommendation: PropTypes.shape({
    type: PropTypes.oneOf([
      NEXT_NO_VIOLATIONS,
      NEXT_NO_VIOLATIONS_DEPENDENCIES,
      NEXT_NON_FAILING,
      NEXT_NON_FAILING_DEPENDENCIES,
      RECOMMENDED_NON_BREAKING,
      RECOMMENDED_NON_BREAKING_WITH_DEPENDENCIES,
      INNER_SOURCE_LATEST_NON_BREAKING,
      INNER_SOURCE_LATEST,
    ]),
    version: PropTypes.string,
    isGolden: PropTypes.bool,
  }),
  reachable: PropTypes.bool,
  isAllViolationsWaived: PropTypes.bool,
  waivedViolationsCount: PropTypes.number,
  hasAutoWaiver: PropTypes.bool,
  hasSameViolationsOnMain: PropTypes.bool,
};

PrioritiesPageRow.propTypes = {
  component: PropTypes.shape({
    displayName: PropTypes.string.isRequired,
    dependencyType: PropTypes.string.isRequired,
    action: PropTypes.string.isRequired,
    priority: PropTypes.number.isRequired,
    hasSameViolationsOnMain: PropTypes.bool,
  }).isRequired,
  componentHref: PropTypes.string.isRequired,
  violationsHref: PropTypes.string.isRequired,
  latestBuildPrioritiesHref: PropTypes.string.isRequired,
  hasAutoWaiversConfigured: PropTypes.bool.isRequired,
};
