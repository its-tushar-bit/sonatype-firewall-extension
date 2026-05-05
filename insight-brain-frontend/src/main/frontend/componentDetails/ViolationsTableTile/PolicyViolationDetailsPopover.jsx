/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useRouterState } from '../../react/RouterStateContext';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxDrawer,
  NxFooter,
  NxButtonBar,
  categoryByPolicyThreatLevel,
  NxH2,
  NxPolicyViolationIndicator,
} from '@sonatype/react-shared-components';

import PortalDrawer from 'MainRoot/react/PortalDrawer';
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
  selectApplicableAutoWaiver,
} from 'MainRoot/violation/violationSelectors';
import {
  selectIsAutoWaiversEnabled,
  selectIsWaiverRequestWorkflowEnabled,
  selectHasWaiverRequestWorkflow,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import ViolationName from './ViolationName';
import { selectIsStandaloneDeveloper } from '../../reduxUiRouter/routerSelectors';
import { loadReportAllData, loadReportIfNeeded } from 'MainRoot/applicationReport/applicationReportActions';
import { selectIsContainerImagesEvaluationEnabledAndProxyStage } from 'MainRoot/applicationReport/applicationReportSelectors';

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
  let { activeWaivers } = useSelector(selectApplicableWaivers);
  const activeAutoWaivers = useSelector(selectApplicableAutoWaiver);
  const violationDetails = useSelector(selectViolationDetails);
  const hasPermissionForAppWaivers = useSelector(selectHasPermissionForAppWaivers);
  const isStandaloneDeveloper = useSelector(selectIsStandaloneDeveloper);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isWaiverRequestWorkflowEnabled = useSelector(selectIsWaiverRequestWorkflowEnabled);
  const hasWaiverRequestWorkflow = useSelector(selectHasWaiverRequestWorkflow);

  const isContainerImagesEvaluationEnabled = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);

  if (isAutoWaiversEnabled && activeAutoWaivers.autoWaiver) {
    activeWaivers = activeWaivers.concat(activeAutoWaivers.autoWaiver);
  }

  const redirectToAddWaiver = () => {
    if (isStandaloneDeveloper) {
      return dispatch(stateGo('developer.addWaiver', { violationId: violationDetails.policyViolationId }));
    }
    return dispatch(stateGo('addWaiver', { violationId: violationDetails.policyViolationId }));
  };
  const redirectToRequestWaiver = () => {
    if (isStandaloneDeveloper) {
      return dispatch(stateGo('developer.requestWaiver', { violationId: violationDetails.policyViolationId }));
    }
    return dispatch(stateGo('requestWaiver', { violationId: violationDetails.policyViolationId }));
  };

  const titleThreatLevelCategory =
    categoryByPolicyThreatLevel[
      selectedPolicyViolation?.policyThreatLevel ||
        selectedPolicyViolation?.threatLevel ||
        violationDetails?.threatLevel
    ];
  const getPolicyExists = policyExists || violationIsLoading;

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
      <NxDrawer.Header>
        <NxDrawer.HeaderTitle className="nx-h2" id="policy-violation-details-popover-title">
          <ViolationName
            policyExists={getPolicyExists}
            policyName={selectedPolicyViolation?.policyName || violationDetails?.policyName}
          ></ViolationName>
        </NxDrawer.HeaderTitle>
        {getPolicyExists ? <NxPolicyViolationIndicator threatLevelCategory={titleThreatLevelCategory} /> : <div></div>}
        {!getPolicyExists && <NxH2>Policy no longer exists</NxH2>}
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
            {!isContainerImagesEvaluationEnabled && (
              <AddOrRequestWaiverButton
                variant={activeWaivers?.length ? 'secondary' : 'primary'}
                hasPermissionForAppWaivers={hasPermissionForAppWaivers}
                isWaiverRequestWorkflowEnabled={isWaiverRequestWorkflowEnabled}
                isRequestWaiverGated={!hasWaiverRequestWorkflow}
                onClickAddWaiver={redirectToAddWaiver}
                onClickRequestWaiver={redirectToRequestWaiver}
              />
            )}
          </NxButtonBar>
        ) : null}
      </NxFooter>
    </PortalDrawer>
  );
}
