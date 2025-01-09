/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { NxDrawer, NxFooter, NxButtonBar, NxPolicyViolationIndicator } from '@sonatype/react-shared-components';

import ViolationPageContainer from 'MainRoot/violation/ViolationPageContainer';
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
} from 'MainRoot/firewall/firewallSelectors';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function FirewallPolicyViolationDetailsPopover() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const toggleDrawer = () => dispatch(actions.toggleShowViolationsDetailPopover());
  const unsetRowClick = () => dispatch(actions.unsetViolationsDetailRowClicked());
  const unsetShowViolationsDetailPopover = () => dispatch(actions.unsetShowViolationsDetailPopover());
  const isViolationsDetailPopoverOpen = useSelector(selectIsViolationsDetailPopoverOpen);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);
  const selectedPolicyViolation = useSelector(selectSelectedPolicyViolation);
  const redirectionProps = useSelector(selectAddWaiverFromFirewallRedirectionProps);
  const { activeWaivers } = useSelector(selectApplicableWaivers);
  const policyViolations = useSelector(selectFirewallPolicyViolations);
  const hasPermissionForAddWaivers = useSelector(selectHasPermissionToAddWaivers);
  const loading = useSelector(selectFirewallIsLoading);

  const policyDetail = selectedPolicyViolation
    ? policyViolations?.find((item) => item.policyViolationId === selectedPolicyViolation.policyViolationId)
    : null;

  const redirectToAddWaiverPage = () => {
    dispatch(
      stateGo(`${isStandaloneFirewall ? 'firewall' : 'repository'}.addWaiver`, {
        ...redirectionProps,
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
    <NxDrawer
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
          <ViolationPageContainer
            $state={uiRouterState}
            selectPolicyId={selectedPolicyViolation?.policyViolationId}
            isFromPolicyViolations
          />
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
              isFirewallOrRepository
              onClickAddWaiver={redirectToAddWaiverPage}
              onClickRequestWaiver={() => {}}
            />
          </NxButtonBar>
        ) : null}
      </NxFooter>
    </NxDrawer>
  );
}
