/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import {
  selectRouterCurrentParams,
  selectCurrentRouteName,
  selectIsStandaloneFirewall,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import WaiverDetails from './WaiverDetails';
import AutoWaiverDetails from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverDetails';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { FIREWALL_FIREWALLPAGE_WAIVERS, FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';

export default function WaiverDetailsContainer() {
  const currentRouteName = useSelector(selectCurrentRouteName);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const isStandaloneFirewall = useSelector(selectIsStandaloneFirewall);

  const waiversBackButtonStateName = isStandaloneFirewall
    ? FIREWALL_FIREWALLPAGE_WAIVERS
    : 'dashboard.overview.waivers';

  const showAutoWaiverDetails =
    (currentRouteName === 'waiver.details' || currentRouteName === FIREWALL_WAIVER_DETAILS) &&
    routerCurrentParams?.type === 'autoWaiver';

  return (
    <>
      <MenuBarBackButton stateName={waiversBackButtonStateName} />
      {showAutoWaiverDetails ? <AutoWaiverDetails /> : <WaiverDetails />}
    </>
  );
}
