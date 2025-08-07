/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const DEVELOPER = 'developer';
const FIREWALL = 'firewall';
const SBOM_MANAGER = 'sbomManager';
const LIFECYCLE = 'lifecycle';

export const PRODUCT_NAMES = {
  DEVELOPER,
  FIREWALL,
  SBOM_MANAGER,
  LIFECYCLE,
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
};

export function useProductInfo(product) {
  const uiRouterState = useRouterState();
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
    default:
      return {
        ...PRODUCT_ICONS.LIFECYCLE,
        href: uiRouterState.href('dashboard.overview.violations'),
      };
  }
}
