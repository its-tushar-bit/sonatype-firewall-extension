/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';
import { selectIsApplication, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
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
  selectIsSbomManagerEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';

import NavPills from 'MainRoot/navPills/NavPills';

export default function OwnerSummaryPills() {
  const isOrg = useSelector(selectIsOrganization);
  const isApp = useSelector(selectIsApplication);
  const isInnerSourceRepositorySupported = useSelector(selectIsInnerSourceRepositorySupported);
  const isInnerSourceRepositoriesEnabled = useSelector(selectIsInnerSourceRepositoriesEnabled);
  const isArtifactoryRepositorySupported = useSelector(selectIsArtifactoryRepositorySupported);
  const isOrgsAndAppsEnabled = useSelector(selectIsOrgsAndAppsEnabled);
  const isLegacyViolationSupported = useSelector(selectIsLegacyViolationSupported);
  const isMonitoringSupported = useSelector(selectIsMonitoringSupported);
  const isProprietaryComponentsEnabled = useSelector(selectIsProprietaryComponentsEnabled);
  const isMultiTenant = useSelector(selectTenantMode) === 'multi-tenant';
  const isDataRetentionEnabled = useSelector(selectIsDataRetentionEnabled);
  const isDataRetentionConfigEnabled = isDataRetentionEnabled && !isMultiTenant;
  const isSourceControlForSourceTileSupported = useSelector(selectIsSourceControlForSourceTileSupported);
  const isScmEnabled = useSelector(selectIsScmEnabled);
  const isSbomManagerEnabled = useSelector(selectIsSbomManagerEnabled);

  const navList = useMemo(() => {
    if (isSbomManagerEnabled && isApp) {
      return [
        {
          label: 'SBOMs',
          target: 'owner-pill-sboms',
          isDisplayed: true,
        },
        {
          label: 'Access',
          target: 'access-tile-pill-access',
          isDisplayed: true,
        },
      ];
    }
    return [
      {
        label: 'App Categories',
        target: 'owner-pill-app-categories',
        isDisplayed: isOrgsAndAppsEnabled,
      },
      {
        label: 'Policies',
        target: 'owner-pill-policy',
        isDisplayed: true,
      },
      {
        label: 'Legacy Violations',
        target: 'owner-pill-legacy-violations',
        isDisplayed: isLegacyViolationSupported,
      },
      {
        label: 'Continuous monitoring',
        target: 'owner-pill-continuous-monitoring',
        isDisplayed: isMonitoringSupported,
      },
      {
        label: 'Proprietary Components',
        target: 'owner-pill-component-configuration',
        isDisplayed: isProprietaryComponentsEnabled,
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
        isDisplayed: isOrg && isDataRetentionConfigEnabled,
      },
      {
        label: 'Source control',
        target: 'owner-pill-source-control',
        isDisplayed: (isOrg || isApp) && isSourceControlForSourceTileSupported && isScmEnabled,
      },
      {
        label: 'InnerSource repository',
        target: 'owner-pill-innersource-repository',
        isDisplayed: isInnerSourceRepositorySupported && (isOrg || isApp) && isInnerSourceRepositoriesEnabled,
      },
      {
        label: 'Artifactory repository',
        target: 'owner-pill-artifactory-repository',
        isDisplayed: isArtifactoryRepositorySupported && (isOrg || isApp),
      },
      {
        label: 'Access',
        target: 'access-tile-pill-access',
        isDisplayed: true,
      },
    ];
  }, [
    isOrgsAndAppsEnabled,
    isLegacyViolationSupported,
    isMonitoringSupported,
    isProprietaryComponentsEnabled,
    isOrg,
    isDataRetentionConfigEnabled,
    isApp,
    isSourceControlForSourceTileSupported,
    isInnerSourceRepositorySupported,
    isInnerSourceRepositoriesEnabled,
    isArtifactoryRepositorySupported,
    isSbomManagerEnabled,
  ]);
  return <NavPills list={navList} root="#owner-summary-sections" />;
}
