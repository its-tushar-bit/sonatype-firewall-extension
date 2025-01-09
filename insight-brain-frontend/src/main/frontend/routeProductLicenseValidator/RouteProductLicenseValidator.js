/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nameStartsWithSbomManager, isSbomManagerOnlyLicenseProduct } from 'MainRoot/sbomManager/sbomManagerUtil';
import {
  isFirewallOnlyLicenseProduct,
  isNotFirewallLicenseProduct,
} from 'MainRoot/productFeatures/productLicenseSelectors';
import { loadIfNotYetLoaded } from 'MainRoot/utility/services/ProductLicense';
import { nameStartsWithFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

const NON_PROTECTED_PATHS = ['home', 'root', 'productlicense', 'gettingStarted', 'proxyConfig'];
const PERMITTED_SBOM_MANAGER_PATHS = [
  'addRole',
  'addWebhook',
  'administrators',
  'administratorsConfig',
  'administratorsEdit',
  'advancedSearchConfig',
  'baseUrlConfiguration',
  'create-ldap',
  'edit-ldap-connection',
  'edit-ldap-usermapping',
  'editRole',
  'editWebhook',
  'ldap-list',
  'listWebhooks',
  'mailConfig',
  'proxyConfig',
  'rolesList',
  'saml',
  'systemNoticeConfiguration',
  'users',
  'createUser',
  'editUser',
];
const FALLBACK_TARGET_STATE_NAME = 'home';

async function isPathPermitted(state) {
  if (NON_PROTECTED_PATHS.includes(state?.name)) {
    return true;
  }

  const productLicense = await loadIfNotYetLoaded();
  if (productLicense?.products == null) {
    console.warn('Product license products are null, cannot check path permission: ' + state?.name);
    return false;
  }

  const isNotFirewall = isNotFirewallLicenseProduct(productLicense?.products);
  const isFirewallOnly = isFirewallOnlyLicenseProduct(productLicense?.products);
  const hasFirewallPrefix = nameStartsWithFirewall(state?.name);

  if ((isNotFirewall && hasFirewallPrefix) || (isFirewallOnly && !hasFirewallPrefix)) {
    return false;
  }

  if (isSbomManagerOnlyLicenseProduct(productLicense.products)) {
    return checkSbomManagerOnlyLicensePaths(state.name);
  }

  return true;
}

function checkSbomManagerOnlyLicensePaths(name) {
  if (nameStartsWithSbomManager(name)) {
    return true;
  }
  return PERMITTED_SBOM_MANAGER_PATHS.includes(name);
}

export default async function handleOnEnterPermissions(stateServiceTargetFn, state) {
  if (!(await isPathPermitted(state))) {
    return stateServiceTargetFn(FALLBACK_TARGET_STATE_NAME);
  }
  return true;
}
