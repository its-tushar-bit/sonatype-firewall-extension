/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { selectRouterCurrentParams, selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import WaiverDetails from './WaiverDetails';
import AutoWaiverDetails from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverDetails';

export default function WaiverDetailsContainer() {
  const currentRouteName = useSelector(selectCurrentRouteName);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);

  if (currentRouteName === 'waiver.details' && routerCurrentParams?.type === 'autoWaiver') {
    return <AutoWaiverDetails />;
  }

  return <WaiverDetails />;
}
