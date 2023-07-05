/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';

import LoadWrapper from '../react/LoadWrapper';
import ViolationDetailsTile, { violationDetailsPropTypes } from './ViolationDetailsTile';
import PolicyViolationConstraintInfoTile, { constraintViolationsPropType } from './PolicyViolationConstraintInfoTile';
import SecurityVulnerabilityDetailsTile from './SecurityVulnerabilityDetailsTile';
import { capitalizeFirstLetter } from '../util/jsUtil';
import { getComponentName } from 'MainRoot/util/componentNameUtils';

export default function ViolationPage(props) {
  const {
    $state,
    loadViolation,
    loadVulnerabilityDetails,
    stateGo,
    fetchStageTypes,
    loading,
    violationDetails,
    stageTypes,
    vulnerabilityDetailsLoading,
    vulnerabilityDetails,
    vulnerabilityDetailsError,
    activeWaivers,
    selectedViolationId,
    goToWaivers,
    isFromPolicyViolations,
    isFirewallContext,
    policyViolations,
    selectPolicyId,
    loadFirewallPolicyVulnerabilityDetails,
    onGoToRepositoryComponentWaiversPage,
    loadFirewallViolationDetails,
    hasPermissionForAppWaivers,
    hasEditIqPermission,
    loadApplicableWaivers,
    componentHash,
    tabId,
    repositoryId,
    matchState,
    pathname,
    isFirewall,
  } = props;

  const error = props.violationDetailsError || props.stageTypesError;

  const policyDetail = selectPolicyId
    ? policyViolations.find((item) => item.policyViolationId === selectPolicyId)
    : null;

  const detailViolations = violationDetails ? violationDetails.constraintViolations : [];

  const constraintViolations = isFirewallContext ? policyDetail.constraints : detailViolations;

  const violationLoading = isFirewallContext
    ? loading || !policyViolations
    : loading || !(violationDetails && stageTypes);

  const conditionTriggerReference = isFirewallContext
    ? policyDetail.constraints[0].conditions[0].conditionTriggerReference
    : null;

  const isSecurityVulnerability =
    capitalizeFirstLetter(
      isFirewallContext ? policyDetail.policyThreatCategory : violationDetails && violationDetails.policyThreatCategory
    ) === 'Security';

  useEffect(() => {
    load();
  }, [selectedViolationId, conditionTriggerReference, selectPolicyId]);

  function load() {
    if (!isFirewallContext) {
      if (selectedViolationId && violationDetails?.policyViolationId !== selectedViolationId) {
        loadViolation(selectedViolationId);
      }
    } else {
      loadFirewallViolationDetails(selectPolicyId);
      loadApplicableWaivers(selectPolicyId);
      if (conditionTriggerReference) {
        loadFirewallPolicyVulnerabilityDetails(conditionTriggerReference.value);
      }
    }
    fetchStageTypes('dashboard');
  }

  return (
    <div id="violation-page">
      <LoadWrapper error={error} loading={violationLoading} retryHandler={load}>
        <ViolationDetailsTile
          {...{
            $state,
            stageTypes,
            violationDetails,
            stateGo,
            activeWaivers,
            goToWaivers,
            selectedViolationId,
            isFromPolicyViolations,
            isFirewallContext,
            policyViolations,
            selectPolicyId,
            policyDetail,
            onGoToRepositoryComponentWaiversPage,
            hasPermissionForAppWaivers,
          }}
        />
        <PolicyViolationConstraintInfoTile
          isFirewallContext={isFirewallContext}
          constraintViolations={constraintViolations}
        />
        {isSecurityVulnerability && (
          <SecurityVulnerabilityDetailsTile
            showTitle={!isFromPolicyViolations}
            vulnerabilityDetails={vulnerabilityDetails}
            error={vulnerabilityDetailsError}
            loading={vulnerabilityDetailsLoading}
            retryLoad={loadVulnerabilityDetails}
            componentName={violationDetails ? getComponentName(violationDetails) : null}
            componentIdentifier={violationDetails?.componentIdentifier}
            ownerType={isFirewallContext ? 'organization' : 'application'}
            ownerId={isFirewallContext ? 'ROOT_ORGANIZATION_ID' : violationDetails?.applicationPublicId}
            hasEditIqPermission={hasEditIqPermission}
            componentHash={componentHash}
            tabId={tabId}
            repositoryId={repositoryId}
            matchState={matchState}
            pathname={pathname}
            isFirewall={isFirewall}
          />
        )}
      </LoadWrapper>
    </div>
  );
}

export const violationPageTypes = {
  $state: PropTypes.shape({
    get: PropTypes.func.isRequired,
    href: PropTypes.func.isRequired,
  }).isRequired,
  selectedViolationId: PropTypes.string,
  loadViolation: PropTypes.func.isRequired,
  loadVulnerabilityDetails: PropTypes.func.isRequired,
  fetchStageTypes: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  violationDetailsError: LoadWrapper.propTypes.error,
  stageTypesError: LoadWrapper.propTypes.error,
  violationDetails: PropTypes.shape({
    ...violationDetailsPropTypes,
    constraintViolations: constraintViolationsPropType.isRequired,
  }),
  stageTypes: ViolationDetailsTile.propTypes.stageTypes,
  vulnerabilityDetailsLoading: PropTypes.bool.isRequired,
  vulnerabilityDetails: PropTypes.object,
  vulnerabilityDetailsError: LoadWrapper.propTypes.error,
  activeWaivers: ViolationDetailsTile.propTypes.activeWaivers,
  goToWaivers: PropTypes.func.isRequired,
  onGoToRepositoryComponentWaiversPage: PropTypes.func.isRequired,
  loadFirewallViolationDetails: PropTypes.func.isRequired,
  loadApplicableWaivers: PropTypes.func.isRequired,
  isFromPolicyViolations: PropTypes.bool,
  isFirewallContext: PropTypes.bool,
  policyViolations: PropTypes.object,
  selectPolicyId: PropTypes.string,
  loadFirewallPolicyVulnerabilityDetails: PropTypes.func,
  hasPermissionForAppWaivers: PropTypes.bool,
  componentHash: PropTypes.string,
  tabId: PropTypes.string,
  repositoryId: PropTypes.string,
  matchState: PropTypes.string,
  pathname: PropTypes.string,
  isFirewall: PropTypes.bool,
};

ViolationPage.propTypes = violationPageTypes;
