/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';

import { NxDrawer, NxFooter, NxButtonBar, NxPolicyViolationIndicator } from '@sonatype/react-shared-components';

import PortalDrawer from 'MainRoot/react/PortalDrawer';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import AddOrRequestWaiverButton from 'MainRoot/waivers/AddOrRequestWaiverButton';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/componentDetails/ViolationsTableTile/policyViolationsSlice';
import {
  selectSelectedPolicyViolation,
  selectIsViolationsDetailPopoverOpen,
} from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { selectApplicableWaivers } from 'MainRoot/violation/violationSelectors';
import {
  selectAddWaiverFromFirewallRedirectionProps,
  selectFirewallPolicyViolations,
  selectFirewallIsLoading,
  selectHasPermissionToAddWaivers,
  selectHasFirewallWaiverCreatePermission,
} from 'MainRoot/firewall/firewallSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsWaiverRequestWorkflowEnabled,
  selectHasWaiverRequestWorkflow,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRepositoryComponents } from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/repositoryResultsSummaryPageSelectors';
import FirewallViolationPageContainer from './FirewallViolationPageContainer';

export default function FirewallPolicyViolationDetailsPopover() {
  const dispatch = useDispatch();

  const toggleDrawer = () => dispatch(actions.toggleShowViolationsDetailPopover());
  const unsetRowClick = () => dispatch(actions.unsetViolationsDetailRowClicked());
  const unsetShowViolationsDetailPopover = () => dispatch(actions.unsetShowViolationsDetailPopover());
  const isViolationsDetailPopoverOpen = useSelector(selectIsViolationsDetailPopoverOpen);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const selectedPolicyViolation = useSelector(selectSelectedPolicyViolation);
  const redirectionProps = useSelector(selectAddWaiverFromFirewallRedirectionProps);
  const { activeWaivers } = useSelector(selectApplicableWaivers);
  const componentDetailsPolicyViolations = useSelector(selectFirewallPolicyViolations);
  const bulkWaivePolicyViolations = useSelector(selectRepositoryComponents);
  const hasPermissionForAddWaivers = useSelector(selectHasPermissionToAddWaivers);
  const hasFirewallOnlyCreatePermission = useSelector(selectHasFirewallWaiverCreatePermission);
  const loading = useSelector(selectFirewallIsLoading);
  const isWaiverRequestWorkflowEnabled = useSelector(selectIsWaiverRequestWorkflowEnabled);
  const hasWaiverRequestWorkflow = useSelector(selectHasWaiverRequestWorkflow);

  // For Standalone Firewall the waiver-request-workflow-enabled system config is a Lifecycle-only concern;
  // show the button based on the entitlement alone. For non-standalone contexts (e.g. repository results
  // summary page) retain the combined check so the admin toggle is still respected.
  const effectiveWaiverRequestEnabled = isStandaloneFirewall
    ? hasWaiverRequestWorkflow
    : isWaiverRequestWorkflowEnabled && hasWaiverRequestWorkflow;

  // Try to find the violation in component details violations first, then fall back to bulk waive violations, or use selectedPolicyViolation directly
  const policyDetail = selectedPolicyViolation
    ? componentDetailsPolicyViolations?.find((item) => item.policyViolationId === selectedPolicyViolation.policyViolationId) ||
      bulkWaivePolicyViolations?.find((item) => item.policyViolationId === selectedPolicyViolation.policyViolationId) ||
      selectedPolicyViolation
    : null;
  const addWaiverRedirectionProps = {
    ...redirectionProps,
    repositoryId: redirectionProps?.repositoryId || selectedPolicyViolation?.repositoryId,
    componentIdentifier: redirectionProps?.componentIdentifier || selectedPolicyViolation?.componentIdentifier,
    componentHash: redirectionProps?.componentHash || selectedPolicyViolation?.componentHash || selectedPolicyViolation?.hash,
    matchState: redirectionProps?.matchState || selectedPolicyViolation?.matchState || selectedPolicyViolation?.matchStateId,
    tabId: redirectionProps?.tabId || 'violations',
    pathname: redirectionProps?.pathname || selectedPolicyViolation?.pathname,
    componentDisplayName:
      redirectionProps?.componentDisplayName ||
      selectedPolicyViolation?.componentDisplayName ||
      selectedPolicyViolation?.componentDisplayText,
  };

  const redirectToAddWaiverPage = () => {
    if (!policyDetail) return;
    dispatch(
      stateGo(`${isStandaloneFirewall ? 'firewall' : 'repository'}.addWaiver`, {
        ...addWaiverRedirectionProps,
        violationId: policyDetail.policyViolationId,
      })
    );
  };

  const navigateToRequestWaiverPage = () => {
    if (!policyDetail) return;
    dispatch(
      stateGo(`${isStandaloneFirewall ? 'firewall' : 'repository'}.requestWaiver`, {
        ...addWaiverRedirectionProps,
        violationId: policyDetail.policyViolationId,
      })
    );
  };

  useEffect(() => {
    return () => {
      unsetShowViolationsDetailPopover();
      unsetRowClick();
    };
  }, []);

  return (
    <PortalDrawer
      id="component-details-policy-violations-popover"
      aria-labelledby="policy-violation-details-popover-title"
      open={isViolationsDetailPopoverOpen}
      onClose={() => {
        toggleDrawer();
        unsetRowClick();
      }}
      className="policy-violation-details-popover"
    >
      <NxDrawer.Header id="component-details-popover-scroll">
        <NxDrawer.HeaderTitle id="policy-violation-details-popover-title" className="nx-h2">
          Violation of <em>{selectedPolicyViolation?.policyName}</em>
        </NxDrawer.HeaderTitle>
        <NxPolicyViolationIndicator policyThreatLevel={selectedPolicyViolation?.policyThreatLevel} />
      </NxDrawer.Header>
      <NxDrawer.Content tabIndex={0}>
        {isViolationsDetailPopoverOpen && (
          <FirewallViolationPageContainer selectPolicyId={selectedPolicyViolation?.policyViolationId} isFromPolicyViolations />
        )}
      </NxDrawer.Content>
      <NxFooter>
        {!loading ? (
          <NxButtonBar>
            {activeWaivers?.length ? (
              <ActiveWaiversIndicator
                activeWaiverCount={activeWaivers.length}
                waived={policyDetail?.waived}
                showUnapplied
              />
            ) : null}
            <AddOrRequestWaiverButton
              variant={activeWaivers?.length ? 'secondary' : 'primary'}
              hasPermissionForAppWaivers={hasPermissionForAddWaivers}
              hasFirewallOnlyCreatePermission={hasFirewallOnlyCreatePermission}
              isFirewallOrRepository
              isWaiverRequestWorkflowEnabled={effectiveWaiverRequestEnabled}
              onClickAddWaiver={redirectToAddWaiverPage}
              onClickRequestWaiver={navigateToRequestWaiverPage}
            />
          </NxButtonBar>
        ) : null}
      </NxFooter>
    </PortalDrawer>
  );
}
