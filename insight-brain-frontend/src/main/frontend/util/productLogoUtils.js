/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const NEXUS_AUDITOR = 'AUDITOR';
const NEXUS_AUDITOR_LOGO = require('../img/nexus_auditor.svg');
const NEXUS_FIREWALL = 'REPOSITORY FIREWALL';
const NEXUS_FIREWALL_LOGO = require('../img/nexus_firewall.svg');
const NEXUS_LIFECYCLE = 'LIFECYCLE';
const NEXUS_LIFECYCLE_FOUNDATION = 'LIFECYCLE FOUNDATION';
const NEXUS_LIFECYCLE_LOGO = require('../img/nexus_lifecycle.svg');
const SONATYPE_LOGO = require('../img/sonatype.svg');
const SBOM_MANAGER_SAAS = 'SBOM MANAGER SAAS';
const SBOM_MANAGER = 'SBOM MANAGER';
const SBOM_MANAGER_LOGO = require('../sbomManager/assets/sbom-manager.svg');

const LOGO_MAP = {
  [NEXUS_AUDITOR]: NEXUS_AUDITOR_LOGO,
  [NEXUS_FIREWALL]: NEXUS_FIREWALL_LOGO,
  [NEXUS_LIFECYCLE]: NEXUS_LIFECYCLE_LOGO,
  [NEXUS_LIFECYCLE_FOUNDATION]: NEXUS_LIFECYCLE_LOGO,
  [SBOM_MANAGER_SAAS]: SBOM_MANAGER_LOGO,
  [SBOM_MANAGER]: SBOM_MANAGER_LOGO,
};

export function getProductLogo(productName = '') {
  return LOGO_MAP[productName.toUpperCase()] || SONATYPE_LOGO;
}
