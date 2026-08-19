/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useSelector } from 'react-redux';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectIsPro, selectIsEnterprise } from 'MainRoot/productFeatures/productTierSelectors';

const DEVELOPER = 'developer';
const FIREWALL = 'firewall';
const SBOM_MANAGER = 'sbomManager';
const LIFECYCLE = 'lifecycle';
const SONATYPE = 'sonatype';
const SONATYPE_UNLICENSED = 'sonatypeUnlicensed';

export const PRODUCT_NAMES = {
  DEVELOPER,
  FIREWALL,
  SBOM_MANAGER,
  LIFECYCLE,
  SONATYPE,
  SONATYPE_UNLICENSED,
};

const PRODUCT_ICONS = {
  DEVELOPER: {
    lightPath: require('MainRoot/productIcons/sonatype-developer-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-developer-logo-nav-dark.svg'),
    altText: 'sonatype developer',
  },
  FIREWALL: {
    lightPath: require('MainRoot/productIcons/sonatype-firewall-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-firewall-logo-nav-dark.svg'),
    altText: 'sonatype firewall',
  },
  SBOM_MANAGER: {
    lightPath: require('MainRoot/productIcons/sonatype-sbom-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-sbom-logo-nav-dark.svg'),
    altText: 'sonatype sbom manager',
  },
  LIFECYCLE: {
    lightPath: require('MainRoot/productIcons/sonatype-lifecycle-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-lifecycle-logo-nav-dark.svg'),
    altText: 'Lifecycle',
  },
  LIFECYCLE_PRO: {
    lightPath: require('MainRoot/productIcons/sonatype-lifecycle-pro-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-lifecycle-pro-logo-nav-dark.svg'),
    altText: 'Lifecycle Pro',
  },
  LIFECYCLE_ENTERPRISE: {
    lightPath: require('MainRoot/productIcons/sonatype-lifecycle-enterprise-logo-nav.svg'),
    darkPath: require('MainRoot/productIcons/sonatype-lifecycle-enterprise-logo-nav-dark.svg'),
    altText: 'Lifecycle Enterprise',
  },
  SONATYPE: {
    lightPath: null,
    darkPath: null,
    altText: '',
  },
  SONATYPE_UNLICENSED: {
    lightPath: require('@sonatype/react-shared-components/assets/img/sonatype-header.svg'),
    darkPath: require('@sonatype/react-shared-components/assets/img/sonatype-header-dark-mode.svg'),
    altText: 'Sonatype',
  },
};

export function useProductInfo(product) {
  const uiRouterState = useRouterState();
  const isPro = useSelector(selectIsPro);
  const isEnterprise = useSelector(selectIsEnterprise);
  switch (product) {
    case DEVELOPER:
      return {
        ...PRODUCT_ICONS.DEVELOPER,
        href: uiRouterState.href('developer.dashboard'),
      };
    case FIREWALL:
      return {
        ...PRODUCT_ICONS.FIREWALL,
        href: uiRouterState.href('firewall.firewallPage'),
      };
    case SBOM_MANAGER:
      return {
        ...PRODUCT_ICONS.SBOM_MANAGER,
        href: uiRouterState.href('sbomManager.dashboard'),
      };
    case LIFECYCLE:
      return {
        ...(isPro
          ? PRODUCT_ICONS.LIFECYCLE_PRO
          : isEnterprise
          ? PRODUCT_ICONS.LIFECYCLE_ENTERPRISE
          : PRODUCT_ICONS.LIFECYCLE),
        href: uiRouterState.href('dashboard.overview.violations'),
      };
    case SONATYPE_UNLICENSED:
      return {
        ...PRODUCT_ICONS.SONATYPE_UNLICENSED,
        href: uiRouterState.href('dashboard.overview.violations'),
      };
    default:
      return {
        ...PRODUCT_ICONS.SONATYPE,
        href: uiRouterState.href('dashboard.overview.violations'),
      };
  }
}
