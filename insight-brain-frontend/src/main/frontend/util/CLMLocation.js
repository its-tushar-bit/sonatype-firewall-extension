/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, isNil, not, pick } from 'ramda';

import commonServicesModule from '../utilAngular/CommonServices';
import { toURIParams, uriTemplate } from './urlUtil';

/**
 * Generates the url to fetch the vulnerability details of a given refId.
 *
 * @param {string} refId refId of the vulnerability whose details are wanted
 * @param {object} componentIdentifier the coordinates of the component where the vulnerability was found.
 * This parameter is _optional_ but providing it will yield results in the scope of the given component.
 * @param {object} extraQueryParameters optional. A set of query parameters to add.
 * It is an object of shape `{identificationSource, ownerId, ownerType, scanId}`. Depending on the
 * value for `identificationSource` it will save one request to HDS and instead will search directly in
 * the third-party vulnerabilities table.
 */
export function getVulnerabilityJsonDetailUrl(refId, componentIdentifier, extraQueryParameters = {}) {
  const urlWithPath = uriTemplate`/api/v2/vulnerabilities/${refId}`;

  const params = toURIParams({
    componentIdentifier: componentIdentifier && JSON.stringify(componentIdentifier),
    ...extraQueryParameters,
  });

  if (params.length > 0) {
    return `${urlWithPath}?${params}`;
  }

  return urlWithPath;
}

export function getRoleForNewUrl() {
  return uriTemplate`/rest/security/roles/new`;
}

export function getAutomaticSourceControlConfigurationUrl() {
  return uriTemplate`/rest/config/automaticScmConfiguration`;
}

export function getMailConfigUrl() {
  return uriTemplate`/api/v2/config/mail`;
}

export function getTestMailUrl(mailRecipient) {
  return uriTemplate`/api/v2/config/mail/test/${mailRecipient}`;
}

export function getViolationDetailsUrl(constituentViolationId) {
  return uriTemplate`/api/v2/policyViolations/crossStage/?constituentId=${constituentViolationId}`;
}

export function getProxyConfigUrl() {
  return uriTemplate`/api/v2/config/httpProxyServer`;
}

export function getDashboardSavedFilters() {
  return uriTemplate`/rest/dashboard/filters/named`;
}

export function getLegalDashboardSavedFilters() {
  return uriTemplate`/rest/userFilter/named?type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getNewestRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/newestRisks`;
}

export function getNewestRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/newestRisks`;
}

/**
 * Retrieve the list of application risk in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getApplicationRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/applicationRisks`;
}

export function getApplicationRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/applicationRisks`;
}

/**
 * Retrieve the list of components with violations in the most recent stage.  Supports filters
 * @since 1.11
 */
export function getComponentRisksUrl() {
  return uriTemplate`/rest/dashboard/policy/componentRisks`;
}

export function getComponentRisksExportUrl() {
  return uriTemplate`/rest/dashboard/export/componentRisks`;
}

/**
 * Retrieve the list of waivers.  Supports filters
 * @since 1.45
 */
export function getWaiversUrl() {
  return uriTemplate`/rest/dashboard/policy/policyWaivers`;
}

export function getWaiversExportUrl() {
  return uriTemplate`/rest/dashboard/export/policyWaivers`;
}

export function getApplicationsUrl() {
  return uriTemplate`/rest/application`;
}

export function getApplicationCategoriesUrl(applicationPublicId) {
  return uriTemplate`/rest/appliedTag/application/${applicationPublicId}`;
}

export function getApplicableOrganizationCategories(applicationPublicId) {
  return uriTemplate`/api/v2/applicationCategories/application/${applicationPublicId}/applicable`;
}

export function getApplicationUrl(applicationPublicId) {
  return uriTemplate`/rest/application/${applicationPublicId}`;
}

export function getDashboardStageUrl() {
  return uriTemplate`/rest/policy/stages?context=dashboard`;
}

export function getOrganizationsUrl() {
  return uriTemplate`/rest/organization`;
}

export function getNLevelOrgUrl() {
  return uriTemplate`/api/v2/organizations`;
}

export function getOrganizationUrl(id) {
  return uriTemplate`/rest/organization/${id}`;
}

export function getAllLicensesUrl() {
  return uriTemplate`/rest/license`;
}

export function getLicenseGroupsUrl(ownerType, ownerId) {
  return uriTemplate`/rest/licenseThreatGroup/${ownerType}/${ownerId}`;
}

export function getApplicableLicenseGroupsUrl(ownerType, ownerId) {
  return uriTemplate`/rest/licenseThreatGroup/${ownerType}/${ownerId}/applicable`;
}

export function getDeleteLicenseGroupUrl(ownerType, ownerId, licenseThreatGroupId) {
  return uriTemplate`/rest/licenseThreatGroup/${ownerType}/${ownerId}/${licenseThreatGroupId}`;
}

export function getLicenseGroupLicensesUrl(ownerType, ownerId, licenseThreatGroupId) {
  return uriTemplate`/rest/licenseThreatGroupLicense/${ownerType}/${ownerId}/${licenseThreatGroupId}`;
}

export function getIsHdsReachable() {
  return uriTemplate`/rest/hdsPing`;
}

export function getTelemetryUrl() {
  return uriTemplate`/rest/environment/stats`;
}

export function getScmOrganizationsUrl() {
  return uriTemplate`/rest/onboarding/organizations`;
}

function getSummaryUrl() {
  return uriTemplate`/rest/application/services/summary`;
}

export function getApplicationSummaryUrl(applicationPublicId) {
  return `${getSummaryUrl()}/${encodeURIComponent(applicationPublicId)}`;
}

export function getApplicationTagsUrl() {
  return uriTemplate`/api/v2/applicationCategories/application`;
}

export function getDashboardFilters() {
  return uriTemplate`/rest/dashboard/filters/active`;
}

export function getLegalDashboardFilters() {
  return uriTemplate`/rest/userFilter/active?type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getActionStageUrl() {
  return uriTemplate`/rest/policy/stages?context=all`;
}

export function getCliStageUrl() {
  return uriTemplate`/rest/policy/stages`;
}

export function getAdvancedSearchConfigUrl() {
  return uriTemplate`/rest/search/advanced/status`;
}

export function getAdvancedSearchIndexUrl() {
  return uriTemplate`/api/v2/search/advanced/index`;
}

export function getAdvancedSearchUrl(query, page, isShowAllComponents) {
  return uriTemplate`/api/v2/search/advanced?query=${query}&page=${page}&allComponents=${isShowAllComponents}`;
}

export function getAdvancedSearchCsvExportUrl(query, isShowAllComponents) {
  return uriTemplate`/api/v2/search/advanced/export/csv?query=${query}&allComponents=${isShowAllComponents}`;
}

export function getScmRepositoriesUrl(organizationId, defaultHostUrl) {
  return uriTemplate`/rest/onboarding/loadRepositories?\
orgId=${organizationId}&defaultHostUrl=${defaultHostUrl}`;
}

export function getScmDefaultHostUrl(organizationId, provider) {
  return uriTemplate`/rest/onboarding/defaultHostUrl?orgId=${organizationId}&provider=${provider}`;
}

export function getImportRepositoriesUrl(organizationId) {
  return uriTemplate`/rest/onboarding/importRepositories/${organizationId}`;
}

export function getValidateScmConfigUrl(scmProvider, scmHostUrl) {
  return uriTemplate`/rest/onboarding/validate/${scmProvider}?scmHostUrl=${scmHostUrl}`;
}

export function getCompositeSourceControlUrl(ownerType, ownerId) {
  return uriTemplate`/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
}

export const getRepositoryInfoUrl = (repositoryId) => uriTemplate`/rest/repositories/${repositoryId}`;

export const getRepositoryEvaluateUrl = (repositoryId) => uriTemplate`/rest/repositories/${repositoryId}/evaluate`;

export const getRepositoryComponentsUrl = (repositoryId) =>
  uriTemplate`/api/experimental/repositories/${repositoryId}/results/details`;

export function getDashboardDeleteFilterUrl(filterName) {
  return uriTemplate`/rest/dashboard/filters/named/delete?filterName=${filterName}`;
}

export function getLegalDashboardDeleteFilterUrl(filterName) {
  return uriTemplate`/rest/userFilter/?name=${filterName}&type=ADVANCED_LEGAL_PACK_DASHBOARD`;
}

export function getApplicableWaiversUrl(policyViolationId) {
  return uriTemplate`/api/v2/policyViolations/${policyViolationId}/applicableWaivers`;
}

export function getApplicationReportsUrl(applicationId) {
  return uriTemplate`/api/v2/reports/applications/${applicationId}`;
}

export function getValidateLicenseUrl() {
  return uriTemplate`/rest/product/license/validate`;
}

function getBaseReportUrl(applicationPublicId, scanId) {
  return uriTemplate`/rest/report/${applicationPublicId}/${scanId}`;
}

const getBrowseReportUrl = (fileName) => (applicationPublicId, scanId) =>
  `${getBaseReportUrl(applicationPublicId, scanId)}/browseReport/${fileName}`;

export function getReportMetadataUrl(applicationPublicId, scanId) {
  return `${getBaseReportUrl(applicationPublicId, scanId)}/metadata`;
}

export function getFirewallConfigurationUrl() {
  return uriTemplate`/api/v2/firewall/releaseQuarantine/configuration`;
}

export function getFirewallReleaseQuarantineSummaryUrl() {
  return uriTemplate`/api/v2/firewall/releaseQuarantine/summary`;
}

export function getRetentionPoliciesUrl(orgId) {
  return uriTemplate`/api/v2/dataRetentionPolicies/organizations/${encodeURIComponent(orgId)}`;
}

export function getReevaluateComponentUrl(repositoryId, hash) {
  return uriTemplate`/rest/repositories/${repositoryId}/evaluate/${hash}`;
}

export const getComponentLicensesUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  identificationSource,
  scanId,
}) => {
  const params = toURIParams({
    componentIdentifier,
    identificationSource,
    scanId,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${ownerId}/licenses?` + params;
};

export const getComponentMultiLicensesUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  identificationSource,
  scanId,
}) => {
  const params = toURIParams({
    componentIdentifier,
    identificationSource,
    scanId,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${ownerId}/multiLicenses?` + params;
};

export function getSuccessMetricsConfigUrl() {
  return uriTemplate`/rest/successMetrics`;
}

export function getSystemNoticeUrl() {
  return uriTemplate`/rest/config/systemNotice`;
}

export function getSystemNoticeFetchUrl() {
  return `${getSystemNoticeUrl()}/fetch`;
}

export function getRoleListUrl() {
  return uriTemplate`/rest/security/roles`;
}

export function getSamlSsoLoginUrl(hash) {
  return hash ? uriTemplate`/saml/login?hash=${hash}` : uriTemplate`/saml/login`;
}

export function getAutomaticApplicationsConfigurationUrl() {
  return uriTemplate`/rest/config/automaticApplications`;
}

export function getLdapConfigUrl(ldapId) {
  return ldapId ? uriTemplate`/rest/config/ldap/${ldapId}` : uriTemplate`/rest/config/ldap`;
}

export function getLdapConnectionConfig(ldapId) {
  return `${getLdapConfigUrl(ldapId)}/connection`;
}

export function getLdapConnectionTest(ldapId) {
  return `${getLdapConfigUrl(ldapId)}/testConnection`;
}

export function getLdapLoginTest(ldapId) {
  return `${getLdapConfigUrl(ldapId)}/testLogin`;
}

export function getLdapUserMappingConfig(ldapId) {
  return `${getLdapConfigUrl(ldapId)}/userMapping`;
}

export function getLdapUserMappingTest(ldapId) {
  return `${getLdapConfigUrl(ldapId)}/testUserMapping`;
}

export function getLdapPriority() {
  return uriTemplate`/rest/config/ldap/priority`;
}

/**
 * @since 1.20.0
 */
export function getOwnerListUrl() {
  return uriTemplate`/rest/sidebar`;
}

export function getRepositoriesUrl() {
  return uriTemplate`/rest/repositories`;
}

export function getFirewallReleaseQuarantineListUrl(page, pageSize, sortBy, sortAsc) {
  let params = toURIParams({
    page: page,
    pageSize: pageSize,
    sortBy: sortBy,
    asc: sortAsc,
  });

  params = params.length === 0 ? '' : '?' + params;

  return uriTemplate`/api/v2/firewall/components/autoReleasedFromQuarantine` + params;
}

export function getFirewallQuarantineListUrl(page, pageSize, sortBy, sortAsc, policyId, componentName) {
  let params = toURIParams({
    page: page,
    pageSize: pageSize,
    sortBy: sortBy,
    asc: sortAsc,
    policyId: policyId,
    componentName: componentName,
  });

  params = params.length === 0 ? '' : '?' + params;

  return uriTemplate`/api/v2/firewall/components/quarantined` + params;
}

export function getFirewallQuarantineSummaryUrl() {
  return uriTemplate`/api/v2/firewall/quarantine/summary`;
}

export function getProductFeaturesUrl() {
  return uriTemplate`/rest/product/features`;
}

export function getEnableUnauthenticatedPages() {
  return uriTemplate`/rest/product/features/enableUnauthenticatedPages`;
}

export function getEnableSsoOnly() {
  return uriTemplate`/rest/product/features/enableSsoOnly`;
}

export function getQuarantinedComponentViewAnonymousAccessEnabledState() {
  return uriTemplate`/api/v2/firewall/quarantinedComponentView/configuration/anonymousAccess/`;
}

/*
 * @since 1.18.0
 */
export function getPermissionContextTestUrl(ownerType, ownerId) {
  if (ownerType === 'repository_container') {
    return uriTemplate`/rest/user/permissions/repository_container`;
  }
  return uriTemplate`/rest/user/permissions/${ownerType}` + (ownerId ? `/${ownerId}` : '');
}

export const getReportBomUrl = getBrowseReportUrl('bom.json');

export const getReportUnknownJsUrl = getBrowseReportUrl('unknownjs.json');

export const getExpandedCoverageEmbeddableUrl = getBrowseReportUrl('index.html');

export const getReportPolicyThreatsUrl = getBrowseReportUrl('policythreats.json');

export const getReportDataUrl = getBrowseReportUrl('data.json');

export const getReportPartialMatchedUrl = getBrowseReportUrl('partialmatched.json');

export const getDependenciesUrl = getBrowseReportUrl('dependencies.json');

export const getReportSecurityUrl = getBrowseReportUrl('security.json');

export const getReportLicenseUrl = getBrowseReportUrl('licenses.json');

export function getReportReevaluateUrl(applicationPublicId, scanId) {
  return `${getBaseReportUrl(applicationPublicId, scanId)}/reevaluatePolicy`;
}

/**
 * @param waiverScope {string} application|organization
 * @param ownerId {string}
 * @param policyViolationId {string}
 */
export function deleteWaiverUrl(waiverScope, ownerId, waiverId) {
  return uriTemplate`/api/v2/policyWaivers/${waiverScope}/${ownerId}/${waiverId}/`;
}

export function redirectTo(url) {
  window.location = url;
}

export function assign(url) {
  window.location.assign(url);
}

export function getDownloadPdfUrl(applicationPublicId, scanId) {
  return uriTemplate`/rest/report/${applicationPublicId}/${scanId}/printReport`;
}

export function getExportCycloneDxUrl(applicationId, scanId) {
  return uriTemplate`/ui/links/cycloneDx/${applicationId}/reports/${scanId}`;
}

export function getExportSpdxUrl(applicationId, scanId) {
  return uriTemplate`/ui/links/spdx/${applicationId}/reports/${scanId}`;
}

/**
 * @param waiverScope {string} application|organization
 * @param ownerId {string}
 * @param policyViolationId {string}
 */
export function getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId) {
  return uriTemplate`/api/v2/policyWaivers/${waiverScope}/${ownerId}/${policyViolationId}`;
}

/**
 * @param {string} ownerType
 * @param {string} ownerId
 * @param {string} policyId
 * @returns {string}
 */
export function getOwnerContextHierarchyUrl(ownerType, ownerId, policyId) {
  return uriTemplate`/rest/policyWaiver/${ownerType}/${ownerId}/applicable/context/${policyId}`;
}

/**
 * Get detailed information for a single waiver
 * @param ownerType {string} application|organization
 * @param ownerId {string}
 * @param waiverId {string}
 * @returns {object}
 */
export function getWaiverDetailsUrl(ownerType, ownerId, policyWaiverId) {
  return uriTemplate`/api/v2/policyWaivers/${ownerType}/${ownerId}/${policyWaiverId}`;
}

export function userTokenUrl() {
  return uriTemplate`/api/v2/userTokens/currentUser`;
}

export function checkUserTokenExistenceUrl() {
  return `${userTokenUrl()}/hasToken`;
}

export function getLicenseLegalApplicationReportUrl(applicationId) {
  return uriTemplate`/api/v2/licenseLegalMetadata/application/${applicationId}`;
}

export function getLicenseLegalComponentUrl(orgOrApp, ownerId, hash) {
  return uriTemplate`/api/v2/licenseLegalMetadata/${orgOrApp}/${ownerId}/component?hash=${hash}`;
}

export function getLicenseLegalComponentByComponentIdentifierUrl(componentIdentifier) {
  return uriTemplate`/api/v2/licenseLegalMetadata/organization/ROOT_ORGANIZATION_ID/component?componentIdentifier=${componentIdentifier}`;
}

export function getLegalDashboardApplicationsUrl() {
  return uriTemplate`/api/experimental/licenseLegalMetadata/dashboard/applications`;
}

export function getLegalDashboardComponentsUrl() {
  return uriTemplate`/api/experimental/licenseLegalMetadata/dashboard/components`;
}

export function getLegalDashboardApplicationUrl(applicationPublicId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/dashboard/application/${applicationPublicId}`;
}

export function getOwnerHierarchyUrl(ownerType, ownerId) {
  return uriTemplate`/rest/owner/${ownerType}/${ownerId}/hierarchy`;
}

export function getOwnerDetailsUrl(ownerType, ownerId, isRepositories) {
  return isRepositories
    ? uriTemplate`/rest/sidebar/repository_container/details`
    : uriTemplate`/rest/sidebar/${ownerType}/${ownerId}/details`;
}

export function getComponentDisplayNameByIdentifierUrl(componentIdentifier) {
  return uriTemplate`/rest/componentDetails/nameByIdentifier?componentIdentifier=${componentIdentifier}`;
}

export function getSaveComponentObligationAttributionUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution`;
}

export function getComponentObligationAttributionUrl(orgOrApp, ownerId, componentIdentifier, obligationName) {
  if (obligationName) {
    return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution
      ?componentIdentifier=${JSON.stringify(componentIdentifier)}&obligationName=${obligationName}`;
  }
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation/attribution
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getDeleteComponentObligationAttributionUrl(componentObligationAttributionId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/component/obligation/attribution/\
${componentObligationAttributionId}`;
}

export function getSaveComponentOriginalSourcesOverrideUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/sourceLink`;
}

export function getSaveComponentCopyrightOverrideUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/copyright`;
}

export function getComponentCopyrightOverrideUrl(orgOrApp, ownerId, componentIdentifier) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/copyright\
?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getSaveComponentObligationUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation`;
}

export function getSaveComponentObligationsUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligations`;
}

export function getComponentObligationUrl(orgOrApp, ownerId, componentIdentifier, obligationName) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/obligation
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}&obligationName=${obligationName}`;
}

export function getDeleteComponentObligationsUrl(componentObligationIds) {
  const queryParams = componentObligationIds.join('&componentObligationId=');
  return uriTemplate`/api/experimental/licenseLegalMetadata/component/obligation?componentObligationId=` + queryParams;
}

export function getSaveLegalFileUrl(orgOrApp, ownerId) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/legalFile`;
}

export function getLegalFileUrl(orgOrApp, ownerId, componentIdentifier, legalFileType) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/legalFile
    ?componentIdentifier=${JSON.stringify(componentIdentifier)}&legalFileType=${legalFileType}`;
}

function getVulnerabilityCustomDataUrl(ownerType, ownerId) {
  return uriTemplate`/api/experimental/vulnerability/customData/${ownerType}/${ownerId}`;
}

function getVulnerabilityCustomDataFieldUrl(field, ownerType, ownerId) {
  return `${getVulnerabilityCustomDataUrl(ownerType, ownerId)}/${field}`;
}

function getVulnerabilityCustomDataFieldIdUrl(field, ownerType, ownerId, id) {
  return `${getVulnerabilityCustomDataUrl(ownerType, ownerId)}/${field}/${id}`;
}

function getVulnerabilityCustomDataFieldRefIdUrl(field, ownerType, ownerId, refId, componentIdentifier) {
  const componentIdentifierParam = componentIdentifier ? `?componentIdentifier=${componentIdentifier}` : '';
  return `${getVulnerabilityCustomDataFieldUrl(field, ownerType, ownerId)}/refId/${refId}${componentIdentifierParam}`;
}

export function getVulnerabilityCustomRemediationRefIdUrl(ownerType, ownerId, refId, componentIdentifier) {
  return getVulnerabilityCustomDataFieldRefIdUrl('remediation', ownerType, ownerId, refId, componentIdentifier);
}

export function getVulnerabilityCustomRemediationUrl(ownerType, ownerId) {
  return getVulnerabilityCustomDataFieldUrl('remediation', ownerType, ownerId);
}

export function getVulnerabilityCustomRemediationIdUrl(ownerType, ownerId, id) {
  return `${getVulnerabilityCustomDataFieldIdUrl('remediation', ownerType, ownerId, id)}`;
}

export function getVulnerabilityCustomCweRefIdUrl(ownerType, ownerId, refId, componentIdentifier) {
  return getVulnerabilityCustomDataFieldRefIdUrl('cwe', ownerType, ownerId, refId, componentIdentifier);
}

export function getVulnerabilityCustomCweUrl(ownerType, ownerId) {
  return getVulnerabilityCustomDataFieldUrl('cwe', ownerType, ownerId);
}

export function getVulnerabilityCustomCweIdUrl(ownerType, ownerId, id) {
  return `${getVulnerabilityCustomDataFieldIdUrl('cwe', ownerType, ownerId, id)}`;
}

export function getVulnerabilityCustomCvssVectorRefIdUrl(ownerType, ownerId, refId, componentIdentifier) {
  return getVulnerabilityCustomDataFieldRefIdUrl('cvss/vector', ownerType, ownerId, refId, componentIdentifier);
}

export function getVulnerabilityCustomCvssVectorUrl(ownerType, ownerId) {
  return getVulnerabilityCustomDataFieldUrl('cvss/vector', ownerType, ownerId);
}

export function getVulnerabilityCustomCvssVectorIdUrl(ownerType, ownerId, id) {
  return `${getVulnerabilityCustomDataFieldIdUrl('cvss/vector', ownerType, ownerId, id)}`;
}

export function getVulnerabilityCustomCvssSeverityRefIdUrl(ownerType, ownerId, refId, componentIdentifier) {
  return getVulnerabilityCustomDataFieldRefIdUrl('cvss/severity', ownerType, ownerId, refId, componentIdentifier);
}

export function getVulnerabilityCustomCvssSeverityUrl(ownerType, ownerId) {
  return getVulnerabilityCustomDataFieldUrl('cvss/severity', ownerType, ownerId);
}

export function getVulnerabilityCustomCvssSeverityIdUrl(ownerType, ownerId, id) {
  return `${getVulnerabilityCustomDataFieldIdUrl('cvss/severity', ownerType, ownerId, id)}`;
}

export function getPoliciesUrl() {
  return uriTemplate`/api/v2/policies`;
}

export function getPolicyMonitoringUrl(ownerType, ownerId) {
  return uriTemplate`/rest/policyMonitoring/${ownerType}/${ownerId}`;
}

export function getApplicablePolicyMonitoringUrl(ownerType, ownerId) {
  return getPolicyMonitoringUrl(ownerType, ownerId) + '/applicable';
}

export function getCopyrightFilePathsUrl(
  orgOrApp,
  ownerId,
  componentHash,
  componentIdentifier,
  copyrightHash,
  pageStart,
  pageLength
) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/${copyrightHash}/filePaths?\
componentIdentifier=${JSON.stringify(componentIdentifier)}&pageStart=${pageStart}&pageLength=${pageLength}`;
}

export function getCopyrightContextUrl(orgOrApp, ownerId, componentHash, componentIdentifier, copyrightHash, filePath) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/${copyrightHash}/context?componentIdentifier=${JSON.stringify(componentIdentifier)}&filePath=${filePath}`;
}

export function getCopyrightFileCountUrl(orgOrApp, ownerId, componentHash, componentIdentifier) {
  return uriTemplate`/api/experimental/licenseLegalMetadata/${orgOrApp}/${ownerId}/component/${componentHash}/copyright\
/fileCount?componentIdentifier=${JSON.stringify(componentIdentifier)}`;
}

export function getNotificationUrl() {
  return uriTemplate`/rest/product/notifications`;
}

export function getNotificationViewedUrl() {
  return uriTemplate`/rest/product/notifications/viewed`;
}

export function getReportAuditLogUrl(appPublicId, reportId, component) {
  const keyJson = JSON.stringify(pick(['hash', 'componentIdentifier'], component));

  return uriTemplate`/rest/report/${appPublicId}/${reportId}/auditLog/licenses.json+security.json
      ?key=${keyJson}`;
}

export function getWebhookEventTypesUrl() {
  return uriTemplate`/rest/config/webhook/eventTypes`;
}

export function saveRequestWaiverUrl(policyViolation) {
  return uriTemplate`/api/v2/policyWaivers/waiverRequests/${policyViolation}`;
}

export function getIsJiraEnabledUrl() {
  return uriTemplate`/rest/jira/enabled`;
}

export function getJiraProjectsUrl() {
  return uriTemplate`/rest/jira/project`;
}

export function getWebhooksUrl() {
  return uriTemplate`/rest/config/webhook`;
}

export function deleteWebhooksUrl(webhookId) {
  return uriTemplate`/rest/config/webhook/${webhookId}`;
}

export function getTransitiveViolationsUrl(ownerType, ownerId, scanId, hash) {
  return uriTemplate`/api/v2/policyViolations/transitive/${ownerType}/${ownerId}/${scanId}?hash=${hash}`;
}

export function getWaiveTransitiveViolationsUrl(ownerId, scanId, hash) {
  return uriTemplate`/api/v2/policyWaivers/transitive/application/${ownerId}/${scanId}?hash=${hash}`;
}

export function getLatestReportUrl(applicationId, stageTypeId) {
  return uriTemplate`/ui/links/application/${applicationId}/latestReport/${stageTypeId}`;
}

export function getRoleByIdUrl(roleId) {
  return uriTemplate`/rest/security/roles/${roleId}`;
}

export function getUserUrl() {
  return uriTemplate`/rest/user`;
}

export function getMultiTenantUserUrl() {
  return uriTemplate`/rest/mtiqUser`;
}

export function getSessionUrl() {
  return uriTemplate`/rest/user/session`;
}

export function getComponentLabels(ownerId, componentHash, ownerType = 'application') {
  return uriTemplate`/rest/label/component/${ownerType}/${ownerId}/${componentHash}`;
}

export function removeLabel(ownerType, ownerId, componentHash, labelId) {
  return uriTemplate`/rest/label/component/${ownerType}/${ownerId}/${componentHash}/${labelId}`;
}

export function setProprietaryMatchers(ownerId) {
  return uriTemplate`/rest/proprietary/application/${ownerId}/add`;
}

export function getApplicableLabelsUrl(ownerType, ownerId) {
  return uriTemplate`/api/v2/labels/${ownerType}/${ownerId}/applicable`;
}

export function getLabelsUrl(ownerType, ownerId) {
  return uriTemplate`/api/v2/labels/${ownerType}/${ownerId}`;
}

export function getDeleteLabelsUrl(ownerType, ownerId, labelId) {
  return uriTemplate`/api/v2/labels/${ownerType}/${ownerId}/${encodeURIComponent(labelId)}`;
}

export function getProprietaryConfigUrl(ownerType, ownerId) {
  return uriTemplate`/rest/proprietary/${ownerType}/${ownerId}`;
}

export function getApplicableLabelScopesUrl(ownerType, ownerId, labelId) {
  return uriTemplate`/api/v2/labels/${ownerType}/${ownerId}/applicable/context/${labelId}`;
}

export function getSaveLabelScopeUrl(scopeType, scopeId, componentHash) {
  return uriTemplate`/rest/label/component/${scopeType}/${scopeId}/${componentHash}`;
}

export function getUserByIdUrl(userId) {
  return uriTemplate`/rest/user/${userId}`;
}

export function getMultiTenantUserByIdUrl(userId) {
  return uriTemplate`/rest/mtiqUser/${userId}`;
}

export function getFindUsersUrl(query) {
  return uriTemplate`/rest/user/global/global/query?q=${query}`;
}

export function getRoleMappingUrl(roleId) {
  return uriTemplate`/rest/membershipMapping/global/global/role/${roleId}`;
}

export function getRoleMappingsForRepositories() {
  return uriTemplate`/rest/membershipMapping/repository_container`;
}

export function getRoleMappingForCurrentOwnerUrl(ownerType, ownerId) {
  return uriTemplate`/rest/membershipMapping/${ownerType}/${ownerId ?? ''}`;
}

export function getAccessPageRolesUrl(ownerType, ownerId) {
  return uriTemplate`/rest/membershipMapping/${ownerType}/${ownerId}`;
}

export function getRepositoryRoleMappingUrl() {
  return uriTemplate`/rest/membershipMapping/repository_container`;
}

export function getUsersRepositoryRoleMappingUrl(query) {
  return uriTemplate`/rest/user/repository_container/query?q=${query}`;
}

export function getUsersRoleMappingUrl(ownerType, ownerId, query, groups = true) {
  const params = { q: query, groups };

  return uriTemplate`/rest/user/${ownerType}/${ownerId}/query?` + toURIParams(params);
}

export function getCreateOrDeleteAccessUrl(ownerType, ownerId, roleId) {
  return uriTemplate`/rest/membershipMapping/${ownerType}/${ownerId}/role/${roleId}`;
}

export function getCreateOrDeleteAccessRepositoryUrl(roleId) {
  return uriTemplate`/rest/membershipMapping/repository_container/role/${roleId}`;
}

export function getSuccessMetricsReportsUrl() {
  return uriTemplate`/rest/successMetrics/report`;
}

export function getRequestWaiverUrl(policyViolationId) {
  return uriTemplate`/api/v2/policyWaiver/${policyViolationId}/application`;
}

export function getLicenseOverrideUrl(ownerType, ownerId, componentIdentifier) {
  if (componentIdentifier) {
    /**
     * `componentIdentifier` is already a stringified json, but it still needs encoding
     * `uriTemplate` handles that encoding for us.
     */
    return uriTemplate`/rest/licenseOverride/${ownerType}/${ownerId}?componentIdentifier=${componentIdentifier}`;
  }

  return uriTemplate`/rest/licenseOverride/${ownerType}/${ownerId}`;
}

export function getBaseLicenseOverrideUrl(ownerType, ownerId) {
  return uriTemplate`/rest/licenseOverride/${ownerType}/${ownerId}`;
}

export function getDeleteLicenseOverrideUrl(ownerType, ownerId, licenseOverrideId) {
  return uriTemplate`/rest/licenseOverride/${ownerType}/${ownerId}/${licenseOverrideId}`;
}

export function getLicensesWithSyntheticFilterUrl() {
  return uriTemplate`/rest/license?filterSynthetic=true`;
}

export function getUserResetPasswordByIdUrl(userId) {
  return uriTemplate`/rest/user/${userId}/reset`;
}

export function getComponentWaivers(ownerType, ownerId, hash) {
  return uriTemplate`/rest/policyWaiver/${ownerType}/${ownerId}/component/${hash}`;
}

export function getRobotUrl(isApp, hashcode) {
  return uriTemplate`/rest/${isApp ? 'application' : 'organization'}/services/generateIcon/${hashcode}`;
}

export function getAddIconUrl(isApp, ownerId) {
  return uriTemplate`/rest/${isApp ? 'application' : 'organization'}/icon/${encodeURIComponent(ownerId)}`;
}

export const getVersionGraphUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  hash,
  matchState,
  proprietary,
  pathname,
  identificationSource,
  scanId,
  stageId,
  dependencyType,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname,
    identificationSource,
    scanId,
    stageId,
    dependencyType,
  });
  return (
    uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${encodeURIComponent(ownerId)}/allVersions?` + params
  );
};

export const getComponentDetailsUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  hash,
  matchState,
  proprietary,
  pathname,
  identificationSource,
  scanId,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    matchState,
    proprietary,
    pathname,
    identificationSource,
    scanId,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${encodeURIComponent(ownerId)}?` + params;
};

export const getPolicyEvaluationTimestampUrl = (repositoryId, componentIdentifier) =>
  uriTemplate`/rest/repositories/${repositoryId}/policyEvaluationTimestamps?componentIdentifier=${componentIdentifier}`;

export const getVulnerabilitiesUrl = ({
  clientType,
  ownerType,
  ownerId,
  componentIdentifier,
  hash,
  identificationSource,
  scanId,
}) => {
  const params = toURIParams({
    componentIdentifier,
    hash,
    identificationSource,
    scanId,
  });
  return uriTemplate`/rest/${clientType}/componentDetails/${ownerType}/${ownerId}/vulnerabilities?` + params;
};

export const getComponentPolicyViolationsUrl = (pathname, repositoryId) =>
  uriTemplate`/rest/repositories/${repositoryId}/policyViolations/${pathname}`;

export function getAttributionReportUrl(applicationPublicId, stageTypeId) {
  return uriTemplate`/api/v2/licenseLegalMetadata/application/${applicationPublicId}/stage/${stageTypeId}/report`;
}

export function getAttributionReportTemplatesUrl() {
  return uriTemplate`/api/v2/licenseLegalMetadata/report-template`;
}

export function getAttributionReportTemplateUrl(templateId) {
  return uriTemplate`/api/v2/licenseLegalMetadata/report-template/${templateId}`;
}

export function getAttributionReportMultiApplicationUrl() {
  return uriTemplate`/rest/legal/attribution/multiApplication/activeUserFilter/report`;
}

export const getSuccessMetricsChartDataUrl = (successMetricsReportId) =>
  uriTemplate`/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/chartData`;

export const getSuccessMetricsComponentCountsUrl = (successMetricsReportId) =>
  uriTemplate`/rest/successMetrics/report/${encodeURIComponent(successMetricsReportId)}/componentCounts`;

export const getSuccessMetricsReportUrl = (successMetricsId) =>
  uriTemplate`/rest/successMetrics/report/${successMetricsId}`;

export function getLicenseSummaryUrl() {
  return uriTemplate`/rest/product/license`;
}

export function getLicenseUploadUrl() {
  return uriTemplate`/api/v2/product/license`;
}

export const getInnerSourceComponentLatestVersionUrl = (componentIdentifier) =>
  uriTemplate`/rest/innerSource/component/latestVersion?componentIdentifier=${JSON.stringify(componentIdentifier)}`;

export function getClaimComponentUrl(hash) {
  const base = uriTemplate`/rest/component/identified`;

  return hash ? `${base}/${encodeURIComponent(hash)}` : base;
}

export function getQuarantinedComponentOverviewUrl(token) {
  return uriTemplate`/rest/repositories/quarantinedComponent/${token}/overview`;
}

export function getQuarantinedComponentPolicyViolationsUrl(token) {
  return uriTemplate`/rest/repositories/quarantinedComponent/${token}/policyViolations`;
}

export function getQuarantinedComponentOtherVersionsUrl(token, page, pageSize, sortAsc) {
  let params = toURIParams({
    page: page,
    pageSize: pageSize,
    asc: sortAsc,
  });

  params = params.length === 0 ? '' : '?' + params;

  return uriTemplate`/rest/repositories/quarantinedComponent/${token}/otherVersions` + params;
}

export function getQuarantinedComponentRemediationUrl(token) {
  return uriTemplate`/rest/repositories/quarantinedComponent/${token}/remediation`;
}

export function getQuarantinedComponentDetailsUrl(token, version) {
  const params = toURIParams({ version });

  return uriTemplate`/rest/repositories/quarantinedComponent/${token}/details?` + params;
}

export const getVulnerabilityOverrideUrl = (ownerType, ownerId, hash, vulnerability) => {
  if (hash && vulnerability) {
    const { source, refId } = vulnerability;
    return uriTemplate`/rest/securityVulnerabilityOverride/${ownerType}/${ownerId}/${hash}/${source}/${refId}`;
  }
  return uriTemplate`/rest/securityVulnerabilityOverride/${ownerType}/${ownerId}`;
};

export const getRepositoryConnectionUrl = (ownerType, ownerId, repositoryConnectionId, inherit) => {
  if (repositoryConnectionId) {
    return uriTemplate`/api/v2/config/repositoryConnection/${ownerType}/${ownerId}/${repositoryConnectionId}`;
  }
  if (compose(not, isNil)(inherit)) {
    return uriTemplate`/api/v2/config/repositoryConnection/${ownerType}/${ownerId}?inherit=${inherit}`;
  }
  return uriTemplate`/api/v2/config/repositoryConnection/${ownerType}/${ownerId}`;
};

export const getTestRepositoryConnectionUrl = (ownerType, ownerId, repositoryConnectionId) => {
  return getRepositoryConnectionUrl(ownerType, ownerId, repositoryConnectionId) + '/test';
};

export const getArtifactoryConnectionUrl = (ownerType, ownerId, artifactoryConnectionId, inherit) => {
  if (artifactoryConnectionId) {
    return uriTemplate`/api/v2/config/artifactoryConnection/${ownerType}/${ownerId}/${artifactoryConnectionId}`;
  }
  if (compose(not, isNil)(inherit)) {
    return uriTemplate`/api/v2/config/artifactoryConnection/${ownerType}/${ownerId}?inherit=${inherit}`;
  }
  return uriTemplate`/api/v2/config/artifactoryConnection/${ownerType}/${ownerId}`;
};

export const getTestArtifactoryConnectionUrl = (ownerType, ownerId, artifactoryConnectionId) => {
  return getArtifactoryConnectionUrl(ownerType, ownerId, artifactoryConnectionId) + '/test';
};

export const getSamlConfigurationUrl = () => {
  return uriTemplate`/api/v2/config/saml`;
};

export const getSamlMetadataUrl = () => {
  return uriTemplate`/api/v2/config/saml/metadata`;
};

export const getOrganizationAppliedTagUrl = (organizationId) => {
  return getCategoriesUrl('organization', organizationId) + '/applied';
};

export const getCategoriesUrl = (ownerType, ownerId) => {
  return uriTemplate`/api/v2/applicationCategories/${ownerType}/${ownerId}`;
};

export const getApplicableCategoriesUrl = (ownerType, ownerId) => {
  const getApplicableParam = ownerType === 'organization' ? '/applicable' : '';

  return getCategoriesUrl(ownerType, ownerId) + getApplicableParam;
};

export const getDeleteCategoriesUrl = (ownerType, ownerId, categoryId) => {
  return getCategoriesUrl(ownerType, ownerId) + `/${categoryId}`;
};

export const getOrganizationPolicyTagUrl = (organizationId) => {
  return getCategoriesUrl('organization', organizationId) + '/policy';
};

export const getApplicationSummariesUrl = (nameFilter, order, page, pageSize) => {
  const params = toURIParams({
    nameFilter,
    order,
    page,
    pageSize,
  });

  return `${getSummaryUrl()}?` + params;
};

export const getConditionTypeUrl = () => uriTemplate`/rest/policy/conditionType`;
export const getConditionValueTypeUrl = (ownerType, ownerId) =>
  uriTemplate`/rest/conditionValueType/${ownerType}/${ownerId}`;

export const getPolicyUrl = (ownerType, ownerId) => {
  return uriTemplate`/rest/policy/${ownerType}/${ownerId}`;
};

export const getPolicyCRUDUrl = (ownerType, ownerId, policyId) => {
  return getPolicyUrl(ownerType, ownerId) + `/${policyId}`;
};

export const getPolicyOverridesUrl = (ownerType, ownerId, policyId) => {
  return getPolicyCRUDUrl(ownerType, ownerId, policyId) + '/overrides';
};

export const getApplicablePolicies = (ownerType, ownerId) => {
  return getPolicyUrl(ownerType, ownerId) + '/applicable';
};

export const getPolicyTagUrl = (policyId, ownerType, ownerId) => {
  return uriTemplate`/rest/appliedTag/policy/${policyId}/${ownerType}/${ownerId}`;
};

export const getCrowdConfigurationUrl = () => {
  return uriTemplate`/api/v2/config/crowd`;
};

export const getCrowdConfigurationTestUrl = () => {
  return uriTemplate`/api/v2/config/crowd/test`;
};

export const getDestinationOrganizationsUrl = (ownerId, isApp) => {
  return uriTemplate`/rest/move/${isApp ? 'application' : 'organization'}/${ownerId}/destinations`;
};

export const getBundleUploadUrl = (applicationPublicId, stageId, sendNotifications) => {
  return uriTemplate`/rest/scan/${applicationPublicId}?stageId=${stageId}&sendNotifications=${sendNotifications}${
    !window.FormData ? '&noFormData=true' : ''
  }`;
};

export const getEvaluationStatusUrl = (applicationPublicId, ticketId) =>
  uriTemplate`/rest/scan/${applicationPublicId}/${ticketId}`;

export const getApplicationReportUrl = (applicationPublicId, scanId) =>
  `#/applicationReport/${applicationPublicId}/${scanId}/policy`;

export const getMoveApplicationUrl = (applicationId, organizationId) =>
  uriTemplate`/api/v2/applications/${applicationId}/move/organization/${organizationId}`;

export const getMoveOrganizationUrl = (organizationId, destinationId) =>
  uriTemplate`/api/v2/organizations/${organizationId}/move/destination/${destinationId}?failEarlyOnError=true`;

export const getMoveOrganizationCSVErrorsUrl = (organizationId, destinationId) =>
  uriTemplate`/rest/organization/move/export/${organizationId}?destinationId=${destinationId}`;

export const getComponentRiskDetailsUrl = (hash) => {
  return uriTemplate`/rest/componentDetails/applications?hash=${hash}`;
};

export const getComponentNameUrl = (hash) => {
  return uriTemplate`/rest/componentDetails/name?hash=${hash}`;
};

export const getAuditReportSummary = function (repositoryId) {
  return uriTemplate`/rest/repositories/${encodeURIComponent(repositoryId)}/report/summary`;
};

export const getGrandfatheringUrl = (ownerType, ownerId) =>
  uriTemplate`/rest/policyViolationGrandfathering/${ownerType}/${ownerId}`;

export const getNotificationWebhooksUrl = (ownerType, ownerId) => {
  return uriTemplate`/rest/config/webhook/policy/${ownerType}/${ownerId ? `${ownerId}` : ''}`;
};

export const getRevokeGrandfatheringUrl = (applicationPublicId) =>
  uriTemplate`/rest/policyViolationGrandfathering/revoke/${applicationPublicId}`;

export const getEndpointsUrl = (apiType) => uriTemplate`/api/v2/endpoints/${apiType}`;

export const getGrandfatheringModalUrl = (appId) =>
  uriTemplate`/rest/policyViolationGrandfathering/grandfather/${appId}`;

export const getImportPoliciesUrl = (appId) => uriTemplate`/rest/policy/organization/${appId}/import`;

export const getRepositoryPolicyViolationUrl = (repositoryId, repositoryPolicyId) =>
  uriTemplate`/rest/repositories/${repositoryId}/policyViolation/${repositoryPolicyId}`;

export const getUnconfiguredRepositoriesManager = () => uriTemplate`/rest/repositories/repositoryManager/unconfigured`;

export const getRepositoryListUrl = (repositoryManagerId) =>
  uriTemplate`/rest/repositories/repositoryManager/${repositoryManagerId}/repositories`;

export const getSupportedRepositoriesFormat = () => uriTemplate`/rest/integration/repositories/evaluate/ignorePatterns`;

export const getConfigureRepositoriesUrl = (repositoryManagerId) =>
  uriTemplate`/rest/repositories/repositoryManager/${repositoryManagerId}/configureRepositories`;

export const getConfigureFirewallOnboardingUrl = () => uriTemplate`/rest/repositories/configureFirewallOnboarding`;

export const getRepositoryComponentNameUrl = () => uriTemplate`/rest/repositories/proprietaryComponentNamePatterns`;

export const getAppsWithoutCiIntegrations = () => uriTemplate`/rest/plugin/apps/ci`;

export const getAppsWithoutScmIntegrations = () => uriTemplate`/rest/sourceControl/application`;

export const getConfigurationUrl = () => {
  return uriTemplate`/api/v2/config`;
};

export const getRepositoryComponentNamePatternUpdateUrl = () =>
  uriTemplate`/rest/repositories/proprietaryComponentNamePatterns/update`;

export const getSourceControlRateLimitsUrl = (ownerType, ownerId) =>
  uriTemplate`/api/experimental/sourceControl/${ownerType}/${ownerId}/rateLimits`;

export const getCiUsageUrl = () => uriTemplate`/rest/plugin/stat/ci`;

export const getIdeIntegratedUserCount = () => uriTemplate`/api/v2/scan/applications/ideUser/overview`;

export const getAppsWithoutRecentCiUsageUrl = () => uriTemplate`/rest/plugin/apps/ci`;

export const getPolicyViolationUiLink = (policyViolationHref) => {
  // policyViolationHref should not be set within the template to avoid undesired character escaping
  return uriTemplate`/assets/` + policyViolationHref;
};

export const getAddWaiverUiLink = (addWaiverHref, comments) => {
  let commentParam = '';
  if (comments) {
    commentParam = `?comments=${encodeURIComponent(comments)}`;
  }
  // addWaiverHref should not be set within the template to avoid undesired character escaping
  // and since the href is in the way the comment can't be encoded by uriTemplate
  return uriTemplate`/assets/` + addWaiverHref + commentParam;
};

export default angular.module('CLMLocation', [commonServicesModule.name]).factory('CLMLocations', [
  'BaseUrl',
  '$window',
  function (baseUrl, $window) {
    function getUserTelemetryPrefix() {
      const isRM = $window.clmEndpoint && $window.clmEndpoint.type === 'rm';

      // use the RM proxy endpoint if we are in RM.  The normal one will get blocked
      return baseUrl.get() + (isRM ? '/rest/rm/user-telemetry' : '/rest/user-telemetry');
    }

    return {
      getLicensesUrl: function () {
        return baseUrl.get() + '/rest/license';
      },

      getConditionTypeUrl,
      getActionStageUrl,
      getDashboardStageUrl,
      getCliStageUrl,
      getApplicationsUrl,

      getApplicationUrl: function (applicationPublicId) {
        return baseUrl.get() + '/rest/application/' + encodeURIComponent(applicationPublicId);
      },

      getApplicationSummariesUrl,

      getApplicationSummaryUrl,

      getOrganizationsUrl,

      getApplicationReportsUrl,

      getValidateLicenseUrl,

      getLicenseSummaryUrl,

      getLicenseUploadUrl,

      evaluatePolicyUrl: function (applicationPublicId, scanId) {
        return baseUrl.get() + '/rest/policy/' + encodeURIComponent(applicationPublicId) + '/evaluate?scanId=' + scanId;
      },

      getUserTelemetryConfig: () => `${getUserTelemetryPrefix()}/config`,

      getUserTelemetryJavascript: () => `${getUserTelemetryPrefix()}/javascript`,

      getUserTelemetryProxy: () => `${getUserTelemetryPrefix()}/events`,

      getProprietaryConfig: function () {
        return baseUrl.get() + '/rest/config/proprietary';
      },

      getLdapPriority,

      getReportUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/browseReport/index.html',

      getSessionUrl,

      getSessionLogoutUrl: function () {
        return baseUrl.get() + '/rest/user/session/logout';
      },

      getUserUrl,

      getRoleByIdUrl,

      getRoleForNewUrl,

      getRoleListUrl,

      getPermissionUrl: function () {
        return baseUrl.get() + '/rest/user/permissions';
      },

      getChangeMyPasswordUrl: function () {
        return baseUrl.get() + '/rest/user/password';
      },

      getChangePasswordUrl: function (userId) {
        return baseUrl.get() + '/rest/user/' + userId + '/password';
      },

      getReportMetadataUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/metadata',

      getBundleUploadUrl: function (applicationPublicId, stageId, sendNotifications) {
        return (
          baseUrl.get() +
          '/rest/scan/' +
          encodeURIComponent(applicationPublicId) +
          '?stageId=' +
          stageId +
          '&sendNotifications=' +
          sendNotifications +
          (!$window.FormData ? '&noFormData=true' : '')
        );
      },

      getEvaluationStatusUrl: function (applicationPublicId, ticketId) {
        return baseUrl.get() + '/rest/scan/' + encodeURIComponent(applicationPublicId) + '/' + ticketId;
      },

      getOrganizationAppliedTagUrl: function (organizationId) {
        return this.getCategoriesUrl('organization', organizationId) + '/applied';
      },
      getCategoriesUrl: function (ownerType, ownerId) {
        return baseUrl.get() + '/api/v2/applicationCategories/' + ownerType + '/' + encodeURIComponent(ownerId);
      },

      getApplicationTagUrl: function (applicationPublicId) {
        return baseUrl.get() + '/rest/appliedTag/application/' + encodeURIComponent(applicationPublicId);
      },
      getApplicableOrganizationTags: function (applicationPublicId) {
        return (
          baseUrl.get() +
          '/api/v2/applicationCategories/application/' +
          encodeURIComponent(applicationPublicId) +
          '/applicable'
        );
      },

      getProductFeaturesUrl,

      getEnableUnauthenticatedPages,

      getEnableSsoOnly,

      getQuarantinedComponentViewAnonymousAccessEnabledState,

      getComponentRisksUrl,

      getComponentRisksExportUrl,

      getApplicationRisksUrl,

      getApplicationRisksExportUrl,

      getNewestRisksUrl,

      getNewestRisksExportUrl,

      getApplicationTagsUrl,

      getDashboardFilters,

      getDashboardSavedFilters,

      getDashboardComponentMatchSummaryUrl: function () {
        return baseUrl.get() + '/rest/dashboard/components/summary';
      },

      getComponentDetailsUrl: getComponentRiskDetailsUrl,

      getComponentNameUrl,

      getNotificationUrl,

      getNotificationViewedUrl,

      /**
       * @Since 1.17
       */
      getAuditReportSummary: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + encodeURIComponent(repositoryId) + '/report/summary';
      },

      /**
       * @Since 1.18
       */
      getRootOrganizationConfigMigrationUrl: function (organizationId) {
        return baseUrl.get() + '/rest/migrate/root' + (organizationId ? '/' + organizationId : '');
      },

      /**
       * @since 1.18
       */
      getRepositoryReportUrl: function (repositoryId) {
        return baseUrl.get() + '/assets/audit-report/index.html?repositoryId=' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryInfoUrl: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId;
      },

      /**
       * @since 1.18
       */
      getRepositoryEvaluateUrl: function (repositoryId) {
        return baseUrl.get() + '/rest/repositories/' + repositoryId + '/evaluate';
      },

      /**
       * @since 1.19.0
       */
      getRepositoriesUrl,

      /**
       * @since 1.20.0
       */
      getOwnerListUrl: function () {
        return baseUrl.get() + '/rest/sidebar';
      },

      getRequestWaiverUrl,
      getWebhooksUrl,

      getWebhookEventTypesUrl,

      getSystemNoticeUrl,

      getSystemNoticeFetchUrl,

      getGrandfatherUrl: function (applicationPublicId) {
        const appId = encodeURIComponent(applicationPublicId);
        return `${baseUrl.get()}/rest/policyViolationGrandfathering/grandfather/${appId}`;
      },

      getSuccessMetricsConfigUrl,

      getSuccessMetricsChartDataUrl,

      getSuccessMetricsComponentCountsUrl,

      getSuccessMetricsReportsUrl,

      getSuccessMetricsReportUrl,

      getAutomaticApplicationsConfigurationUrl,
      getAdvancedSearchConfigUrl: () => `${baseUrl.get()}/rest/search/advanced/status`,

      getShouldDisplayDefaultPasswordWarning: () => `${baseUrl.get()}/rest/user/shouldDisplayDefaultPasswordWarning`,

      getIsHdsReachable,

      getTelemetryUrl,

      getReportPolicyThreatsUrl: getBrowseReportUrl('policythreats.json'),

      getReportAuditLogUrl,

      getReportReevaluateUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/reevaluatePolicy',

      getReportPdfDownloadUrl: (applicationPublicId, scanId) =>
        getBaseReportUrl(applicationPublicId, scanId) + '/printReport',

      getExportCycloneDxUrl,

      getExportSpdxUrl,

      getClaimComponentUrl,

      getVulnerabilityJsonDetailUrl,

      /**
       * @since 1.79.0
       */
      getSourceControlUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControl/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.79.0
       */
      getCompositeSourceControlUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControl/${ownerType}/${ownerId}`;
      },

      getValidateScmConfigUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/compositeSourceControlConfigValidator/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.97.0
       */
      getSourceControlMetricsUrl: function (ownerType, ownerId) {
        return baseUrl.get() + `/api/v2/sourceControlMetrics/${ownerType}/${ownerId}`;
      },

      /**
       * @since 1.102.0
       */
      getAbsoluteUrl: function (url) {
        return baseUrl.get() + `/${url}`;
      },

      getInnerSourceComponentLatestVersionUrl,

      /**
       * @since 1.128.0
       */
    };
  },
]);
