/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useRouterState } from '../../react/RouterStateContext';
import ViolationDetailsTileHeaderMainTitle from 'MainRoot/violation/ViolationDetailsTileHeaderMainTitle';
import { useDispatch, useSelector } from 'react-redux';
import { NxDrawer, NxFooter, NxButtonBar, categoryByPolicyThreatLevel } from '@sonatype/react-shared-components';

import ViolationPageContainer from 'MainRoot/violation/ViolationPageContainer';
import ActiveWaiversIndicator from 'MainRoot/violation/ActiveWaiversIndicator';
import AddOrRequestWaiverButton from 'MainRoot/waivers/AddOrRequestWaiverButton';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from './policyViolationsSlice';
import {
  selectSelectedComponentPolicyViolation,
  selectIsViolationsDetailPopoverOpen,
} from './PolicyViolationsSelectors';
import {
  selectPolicyExists,
  selectViolationIsLoading,
  selectViolationDetails,
  selectApplicableWaivers,
  selectHasPermissionForAppWaivers,
} from 'MainRoot/violation/violationSelectors';

export default function PolicyViolationDetailsPopover() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const toggleDrawer = () => dispatch(actions.toggleShowViolationsDetailPopover());
  const unsetRowClick = () => dispatch(actions.unsetViolationsDetailRowClicked());
  const unsetShowViolationsDetailPopover = () => dispatch(actions.unsetShowViolationsDetailPopover());
  const isViolationsDetailPopoverOpen = useSelector(selectIsViolationsDetailPopoverOpen);
  const violationIsLoading = useSelector(selectViolationIsLoading);
  const policyExists = useSelector(selectPolicyExists);
  const selectedPolicyViolation = useSelector(selectSelectedComponentPolicyViolation);
  const { activeWaivers } = useSelector(selectApplicableWaivers);
  const violationDetails = useSelector(selectViolationDetails);
  const hasPermissionForAppWaivers = useSelector(selectHasPermissionForAppWaivers);

  const redirectToAddOrRequestWaiverPage = () =>
    hasPermissionForAppWaivers
      ? dispatch(stateGo('addWaiver', { violationId: selectedPolicyViolation.policyViolationId }))
      : dispatch(stateGo('requestWaiver', { violationId: selectedPolicyViolation.policyViolationId }));

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
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle id="policy-violation-details-popover-title">
          <ViolationDetailsTileHeaderMainTitle
            // This prevents the title to show as non existing while loading
            policyExists={policyExists || violationIsLoading}
            policyName={selectedPolicyViolation?.policyName}
            threatLevelCategory={
              categoryByPolicyThreatLevel[
                selectedPolicyViolation?.policyThreatLevel || selectedPolicyViolation?.threatLevel
              ]
            }
          />
        </NxDrawer.HeaderTitle>
      </NxDrawer.Header>
      <NxDrawer.Content tabIndex={0}>
        {isViolationsDetailPopoverOpen && <ViolationPageContainer $state={uiRouterState} isFromPolicyViolations />}
      </NxDrawer.Content>
      <NxFooter>
        {!violationIsLoading ? (
          <NxButtonBar>
            {activeWaivers?.length ? (
              <ActiveWaiversIndicator
                activeWaiverCount={activeWaivers.length}
                waived={violationDetails?.waived}
                showUnapplied
              />
            ) : null}
            <AddOrRequestWaiverButton
              variant={activeWaivers?.length ? 'secondary' : 'primary'}
              hasPermissionForAppWaivers={hasPermissionForAppWaivers}
              onClick={redirectToAddOrRequestWaiverPage}
            />
          </NxButtonBar>
        ) : null}
      </NxFooter>
    </NxDrawer>
  );
}
