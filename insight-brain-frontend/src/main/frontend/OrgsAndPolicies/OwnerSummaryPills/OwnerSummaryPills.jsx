/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { selectIsApplication, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsInnerSourceRepositorySupported,
  selectIsArtifactoryRepositorySupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectIsFirewallOnlyLicense } from 'MainRoot/configuration/license/licenseSelectors';

import NavPills from 'MainRoot/navPills/NavPills';

export default function OwnerSummaryPills() {
  const isOrg = useSelector(selectIsOrganization);
  const isApp = useSelector(selectIsApplication);
  const isInnerSourceRepositorySupported = useSelector(selectIsInnerSourceRepositorySupported);
  const isArtifactoryRepositorySupported = useSelector(selectIsArtifactoryRepositorySupported);
  const isFirewallOnlyLicense = useSelector(selectIsFirewallOnlyLicense);
  const isFeatureEnabledForLicense = !isFirewallOnlyLicense;

  const navList = useMemo(
    () => [
      {
        label: 'App Categories',
        target: 'owner-pill-app-categories',
        isDisplayed: isFeatureEnabledForLicense,
      },
      {
        label: 'Policies',
        target: 'owner-pill-policy',
        isDisplayed: true,
      },
      {
        label: 'Grandfathering',
        target: 'owner-pill-grandfathering',
        isDisplayed: isFeatureEnabledForLicense,
      },
      {
        label: 'Continuous monitoring',
        target: 'owner-pill-continuous-monitoring',
        isDisplayed: isFeatureEnabledForLicense,
      },
      {
        label: 'Proprietary Components',
        target: 'owner-pill-component-configuration',
        isDisplayed: isFeatureEnabledForLicense,
      },
      {
        label: 'Component labels',
        target: 'owner-pill-comp-labels',
        isDisplayed: true,
      },
      {
        label: 'License threat groups',
        target: 'owner-pill-ltgs',
        isDisplayed: true,
      },
      {
        label: 'Data retention',
        target: 'owner-pill-retention',
        isDisplayed: isOrg && isFeatureEnabledForLicense,
      },
      {
        label: 'Source control',
        target: 'owner-pill-source-control',
        isDisplayed: (isOrg || isApp) && isFeatureEnabledForLicense,
      },
      {
        label: 'InnerSource repository',
        target: 'owner-pill-innersource-repository',
        isDisplayed: isInnerSourceRepositorySupported && (isOrg || isApp) && isFeatureEnabledForLicense,
      },
      {
        label: 'Artifactory repository',
        target: 'owner-pill-artifactory-repository',
        isDisplayed: isArtifactoryRepositorySupported && (isOrg || isApp),
      },
      {
        label: 'Access',
        target: 'owner-pill-access',
        isDisplayed: true,
      },
    ],
    [isOrg, isApp, isArtifactoryRepositorySupported, isInnerSourceRepositorySupported]
  );
  return <NavPills list={navList} root="#owner-summary-sections" />;
}
