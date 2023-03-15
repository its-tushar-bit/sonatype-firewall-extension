/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector, useDispatch } from 'react-redux';
import LicenseDetectionsTile from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/LicenseDetectionsTile';
import FirewallPolicyViolationsTile from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/policyViolationsTile/FirewallPolicyViolationsTile';
import { selectLegalPolicyViolations } from 'MainRoot/firewall/firewallComponentDetailsPage/policyViolations/firewallPolicyViolationsSelectors.js';
import { selectFirewallComponentDetailsPage } from 'MainRoot/firewall/firewallSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { loadComponentDetails, loadComponentLicenses } from 'MainRoot/firewall/firewallActions';
import { selectFirewallLicenseDetectionsTileDataSlice } from 'MainRoot/firewall/firewallComponentDetailsPage/legal/firewallLegalSelectors';
import { actions as licenseDetectionTileActions } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LicenseDetectionsTile/licenseDetectionsTileSlice';
import EditLicensesPopoverContainer from 'MainRoot/componentDetails/ComponentDetailsLegalTab/EditLicensesPopover/EditLicensesPopoverContainer';

export default function ComponentDetailsLegalTab() {
  const violations = useSelector(selectLegalPolicyViolations);
  const { isLoadingComponentDetails, componentDetailsLoadError } = useSelector(selectFirewallComponentDetailsPage);
  const routeParams = useSelector(selectRouterCurrentParams);
  const { stageId, componentHash, repositoryId, componentIdentifier } = routeParams;
  const dispatch = useDispatch();
  const data = {
    ...useSelector(selectFirewallLicenseDetectionsTileDataSlice),
    isLoadingComponentDetails,
    componentDetailsLoadError,
    stageId,
    componentHash,
  };
  const reviewObligationsClickHandler = () => {
    dispatch(
      stateGo('legal.componentOverviewByComponentIdentifier', {
        componentIdentifier,
        repositoryId,
      })
    );
  };

  return (
    <>
      <LicenseDetectionsTile
        {...{
          ...data,
          loadLicenses: () => dispatch(loadComponentLicenses(repositoryId, componentIdentifier)),
          toggleShowEditLicensesPopover: () => dispatch(licenseDetectionTileActions.toggleShowEditLicensesPopover()),
          reviewObligationsClickHandler,
          loadComponentDetails: () => dispatch(loadComponentDetails(routeParams)),
        }}
      />
      <FirewallPolicyViolationsTile violations={violations} showProxyState title="Legal Policy Violations" />
      <EditLicensesPopoverContainer />
    </>
  );
}
