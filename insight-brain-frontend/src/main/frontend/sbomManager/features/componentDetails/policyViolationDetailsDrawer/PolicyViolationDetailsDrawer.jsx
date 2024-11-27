/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  categoryByPolicyThreatLevel,
  NxDrawer,
  NxH2,
  NxLoadWrapper,
  NxPolicyViolationIndicator,
} from '@sonatype/react-shared-components';
import { pathOr } from 'ramda';

import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import PolicyViolationConstraintInfo from 'MainRoot/violation/PolicyViolationConstraintInfo';
import SbomVulnerabilityDetails from '../vulnerabilitiesDrawer/SbomVulnerabilityDetails';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { actions } from '../componentDetailsSlice';
import { selectSbomComponentDetails, selectPolicyViolationDetailsDrawer } from '../componentDetailsSelector';
import SbomManagerViolationDetailsTile from './SbomManagerViolationDetailsTile';

import './PolicyViolationDetailsDrawer.scss';

export default function PolicyViolationDetailsDrawer() {
  const dispatch = useDispatch();
  const {
    policyViolationDetailsDrawer: { showDrawer, policyViolationId },
    sbomPolicyViolations: { policy },
    componentDetails,
    vulnerabilityDetails,
    loadingVulnerabilityDetail,
    loadVulnerabilityDetailError,
  } = useSelector(selectSbomComponentDetails);
  const routerParams = useSelector(selectRouterCurrentParams);

  const hideDrawer = () => dispatch(actions.hidePolicyViolationDetailsDrawer());

  const { applicationPublicId } = routerParams;

  const violation = (policy?.allViolations || policy?.activeViolations || []).find(
    (violation) => violation.policyViolationId === policyViolationId
  );

  const titleThreatLevelCategory = categoryByPolicyThreatLevel[violation?.policyThreatLevel];

  const vulnerabilityRefId = pathOr(
    null,
    ['constraints', 0, 'conditions', 0, 'conditionTriggerReference', 'value'],
    violation
  );

  const loadSbomComponentVulnerabilities = () =>
    dispatch(
      actions.loadVulnerabilityDetails({
        vulnerability: { refId: vulnerabilityRefId },
        componentIdentifier: componentDetails?.componentIdentifier,
        extraParams: {
          ownerId: applicationPublicId,
          hash: componentDetails?.hash,
          isRepository: false,
        },
      })
    );

  useEffect(() => {
    if (componentDetails && vulnerabilityRefId) {
      loadSbomComponentVulnerabilities();
    }
  }, [componentDetails, vulnerabilityRefId]);

  const drawerContent = violation ? (
    <>
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle id="policy-violation-details-drawer-title">
          <span>Violation of {violation.policyName}</span>
          <NxPolicyViolationIndicator threatLevelCategory={titleThreatLevelCategory} />
        </NxDrawer.HeaderTitle>
      </NxDrawer.Header>
      <NxDrawer.Content tabIndex={0}>
        <SbomManagerViolationDetailsTile />
        <NxLoadWrapper
          retryHandler={loadSbomComponentVulnerabilities}
          loading={loadingVulnerabilityDetail}
          error={loadVulnerabilityDetailError}
        >
          {violation.constraints && (
            <PolicyViolationConstraintInfo
              constraintViolations={violation.constraints}
              isFirewallContext={true}
              isFromPolicyViolations={true}
            />
          )}
          {!isNilOrEmpty(vulnerabilityDetails) && (
            <>
              <NxH2>Vulnerability Details</NxH2>
              <SbomVulnerabilityDetails
                componentName={componentDetails?.packageUrl}
                componentIdentifier={componentDetails?.componentIdentifier}
                vulnerabilityDetails={vulnerabilityDetails}
              />
            </>
          )}
        </NxLoadWrapper>
      </NxDrawer.Content>
    </>
  ) : null;

  return (
    <NxDrawer
      id="sbom-manager-policy-violation-details-drawer"
      className="sbom-manager-policy-violation-details-drawer"
      aria-labelledby="policy-violation-details-drawer-title"
      open={showDrawer}
      onClose={() => hideDrawer()}
    >
      {drawerContent}
    </NxDrawer>
  );
}
