/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { selectIsApplication, selectIsOrganization, selectIsSbomManager, selectIsFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsArtifactoryRepositorySupported,
  selectIsDataRetentionEnabled,
  selectIsInnerSourceRepositorySupported,
  selectIsInnerSourceRepositoriesEnabled,
  selectIsLegacyViolationSupported,
  selectIsMonitoringSupported,
  selectIsOrgsAndAppsEnabled,
  selectIsProprietaryComponentsEnabled,
  selectIsSourceControlForSourceTileSupported,
  selectTenantMode,
  selectIsScmEnabled,
  selectIsSbomContinuousMonitoringUiEnabled,
  selectIsSbomPoliciesSupported,
  selectIsAutoWaiversEnabled,
  selectIsDeveloperDashboardEnabled,
  selectIsCpeMatchingSupported,
  selectIsWaiverExpirationNotificationEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

import NavPills from 'MainRoot/navPills/NavPills';
import { selectIsSbomManagerOnlyLicense } from 'MainRoot/productFeatures/productLicenseSelectors';

export default function OwnerSummaryPills() {
  const isApp = useSelector(selectIsApplication);
  const isFirewall = useSelector(selectIsFirewall);
  const isArtifactoryRepositorySupported = useSelector(selectIsArtifactoryRepositorySupported);
  const isDataRetentionEnabled = useSelector(selectIsDataRetentionEnabled);
  const isInnerSourceRepositoriesEnabled = useSelector(selectIsInnerSourceRepositoriesEnabled);
  const isInnerSourceRepositorySupported = useSelector(selectIsInnerSourceRepositorySupported);
  const isLegacyViolationSupported = useSelector(selectIsLegacyViolationSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isOrg = useSelector(selectIsOrganization);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isProprietaryComponentsEnabled = useSelector(selectIsProprietaryComponentsEnabled);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isScmEnabled = useSelector(selectIsScmEnabled);
  const isSourceControlForSourceTileSupported = useSelector(selectIsSourceControlForSourceTileSupported);
  const isSbomContinuousMonitoringUiEnabled = useSelector(selectIsSbomContinuousMonitoringUiEnabled);
  const isSbomPoliciesSupported = useSelector(selectIsSbomPoliciesSupported);
  const isAutoWaiversEnabled = useSelector(selectIsAutoWaiversEnabled);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const isPublicDataEnabled = useSelector(selectIsCpeMatchingSupported);
  const isWaiverExpirationNotificationEnabled = useSelector(selectIsWaiverExpirationNotificationEnabled);
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';
  const isDataRetentionConfigEnabled = isDataRetentionEnabled && !isMultiTenant;
  const hasSbomManagerLicenseOnly = useSelector(selectIsSbomManagerOnlyLicense);

  const navList = useMemo(() => {
    return [
      {
        label: 'SBOMs',
        target: 'owner-pill-sboms',
        isDisplayed: isSbomManager && isApp,
      },
      {
        label: 'App Categories',
        target: 'owner-pill-app-categories',
        isDisplayed: isOrgsAndAppsEnabled && !isSbomManager,
      },
      {
        label: 'Policies',
        target: 'owner-pill-policy',
        isDisplayed: !isSbomManager || isSbomPoliciesSupported,
      },
      {
        label: 'Legacy Violations',
        target: 'owner-pill-legacy-violations',
        isDisplayed: isLegacyViolationSupported && !isSbomManager,
      },
      {
        label: 'Continuous monitoring',
        target: 'owner-pill-continuous-monitoring',
        isDisplayed: isMonitoringSupported && (!isSbomManager || isSbomContinuousMonitoringUiEnabled),
      },
      {
        label: 'Proprietary Components',
        target: 'owner-pill-component-configuration',
        isDisplayed: isProprietaryComponentsEnabled && !isSbomManager,
      },
      {
        label: 'Component labels',
        target: 'owner-pill-comp-labels',
        isDisplayed: !isSbomManager,
      },
      {
        label: 'License threat groups',
        target: 'owner-pill-ltgs',
        isDisplayed: !isSbomManager,
      },
      {
        label: 'Data retention',
        target: 'owner-pill-retention',
        isDisplayed: isOrg && isDataRetentionConfigEnabled && !isSbomManager,
      },
      {
        label: 'Waiver Expiration Notifications',
        target: 'owner-pill-waiver-expiration-notification',
        isDisplayed: isFirewall && !isSbomManager && isWaiverExpirationNotificationEnabled,
      },
      {
        label: 'Source control',
        target: 'owner-pill-source-control',
        isDisplayed: (isOrg || isApp) && isSourceControlForSourceTileSupported && isScmEnabled && !isSbomManager,
      },
      {
        label: 'InnerSource repository',
        target: 'owner-pill-innersource-repository',
        isDisplayed:
          isInnerSourceRepositorySupported && (isOrg || isApp) && isInnerSourceRepositoriesEnabled && !isSbomManager,
      },
      {
        label: 'Artifactory repository',
        target: 'owner-pill-artifactory-repository',
        isDisplayed: isArtifactoryRepositorySupported && (isOrg || isApp) && !isSbomManager,
      },
      {
        label: 'Auto-Waivers',
        target: 'owner-pill-auto-waivers-configuration',
        isDisplayed: isAutoWaiversEnabled && !isSbomManager && isDeveloperDashboardEnabled,
      },
      {
        label: 'Access',
        target: 'access-tile-pill-access',
        isDisplayed: true,
      },
      {
        label: 'Public Data Sources',
        target: 'owner-pill-public-data-sources',
        isDisplayed: isPublicDataEnabled && !hasSbomManagerLicenseOnly,
      },
    ];
  }, [
    isApp,
    isFirewall,
    isArtifactoryRepositorySupported,
    isDataRetentionConfigEnabled,
    isInnerSourceRepositoriesEnabled,
    isInnerSourceRepositorySupported,
    isLegacyViolationSupported,
    isMonitoringSupported,
    isOrg,
    isOrgsAndAppsEnabled,
    isProprietaryComponentsEnabled,
    isSbomManager,
    isSourceControlForSourceTileSupported,
    isSbomContinuousMonitoringUiEnabled,
    isWaiverExpirationNotificationEnabled,
  ]);
  return <NavPills list={navList} root="#owner-summary-sections" />;
}
