/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import NavPills from 'MainRoot/navPills/NavPills';
import { selectIsFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsWaiverExpirationNotificationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function RepositoriesPills() {
  const isFirewall = useSelector(selectIsFirewall);
  const isWaiverExpirationNotificationEnabled = useSelector(selectIsWaiverExpirationNotificationEnabled);

  const navList = useMemo(() => [
    {
      label: 'Configuration',
      target: 'repositories-pill-configuration',
      isDisplayed: true,
    },
    {
      label: 'Policies',
      target: 'owner-pill-policy',
      isDisplayed: true,
    },
    {
      label: 'Namespace Confusion Protection',
      target: 'namespace-confusion-protection-pill-configuration',
      isDisplayed: true,
    },
    {
      label: 'Waiver Expiration Notifications',
      target: 'owner-pill-waiver-expiration-notification',
      isDisplayed: isFirewall && isWaiverExpirationNotificationEnabled,
    },
    {
      label: 'Access',
      target: 'access-tile-pill-access',
      isDisplayed: true,
    },
  ], [isFirewall, isWaiverExpirationNotificationEnabled]);

  return <NavPills list={navList} root="#repositories-summary-sections" />;
}
