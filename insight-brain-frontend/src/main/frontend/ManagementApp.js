/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import configurationModule from './configuration/module';
import legacyConfigurationModule from './LegacyConfigurationModule';
import dashboardModule from './dashboard/dashboard.module';
import componentDetailsModule from './componentDetails/module';
import dependencyTreeModule from './DependencyTree/module';
import atlassianCrowdConfigurationModule from './configuration/crowd/module';
import reduxConfigModule from './reduxConfig/module';
import changeDefaultAdminPasswordModule from './changeDefaultAdminPasswordNotice/module';
import applicationReportModule from './applicationReport/module';
import ownerManagerModule from './OrgsAndPolicies/owner.manager.module';
import { MainModule } from './MainModule';
import { UserModule } from './security/users/UserModule';
import { SecurityModule } from './security/SecurityModule';
import RoleModule from './security/RoleModule';
import labsModule from './labs/module';
import vulnerabilitySearchModule from './vulnerabilitySearch/module';
import vulnerabilityCustomizeModule from './vulnerabilityCustomize/module';
import violationPageModule from './violation/module';
import waiversModule from './waivers/module';
import standaloneFirewallModule from './firewall/firewall.module';
import firewallOnboardingModule from './firewallOnboarding/module';
import quarantinedComponentReportModule from './quarantinedComponentReport/module';
import SystemNoticeContainer from './systemNotice/SystemNoticeContainer';
import innerSourceRepositoryConfigurationModule from './innerSourceRepositoryConfiguration/module';
import artifactoryRepositoryConfigurationModule from './artifactoryRepositoryConfiguration/module';
import apiModule from './api/module';
import baseUrlConfigurationModule from './configuration/baseUrl/module';
import baseUrlNotSetNoticeModule from 'MainRoot/configuration/baseUrl/baseUrlNotSetNotice/module';
import sourceControlRateLimitsModule from 'MainRoot/OrgsAndPolicies/sourceControlRateLimits/module';
import enterpriseReportingModule from 'MainRoot/enterpriseReporting/module';
import sastScanModule from 'MainRoot/sastScan/module';
import prioritiesPageModule from 'MainRoot/development/prioritiesPage/priorities.page.module';
import sbomManagerModule from 'MainRoot/sbomManager/sbom.manager.module';
import advancedSearchModule from 'MainRoot/advancedSearch/module';
import developerModule from 'MainRoot/development/developer.module';
import applicationLatestEvaluationsModule from 'MainRoot/applicationLatestEvaluations/module';

export default angular
  .module('managementApp', [
    MainModule.name,
    UserModule.name,
    SecurityModule.name,
    RoleModule.name,
    ownerManagerModule.name,
    labsModule.name,
    configurationModule.name,
    legacyConfigurationModule.name,
    dashboardModule.name,
    reduxConfigModule.name,
    changeDefaultAdminPasswordModule.name,
    applicationReportModule.name,
    vulnerabilitySearchModule.name,
    vulnerabilityCustomizeModule.name,
    violationPageModule.name,
    waiversModule.name,
    firewallOnboardingModule.name,
    componentDetailsModule.name,
    dependencyTreeModule.name,
    quarantinedComponentReportModule.name,
    innerSourceRepositoryConfigurationModule.name,
    artifactoryRepositoryConfigurationModule.name,
    atlassianCrowdConfigurationModule.name,
    apiModule.name,
    baseUrlConfigurationModule.name,
    baseUrlNotSetNoticeModule.name,
    sourceControlRateLimitsModule.name,
    enterpriseReportingModule.name,
    sastScanModule.name,
    sbomManagerModule.name,
    prioritiesPageModule.name,
    advancedSearchModule.name,
    developerModule.name,
    standaloneFirewallModule.name,
    applicationLatestEvaluationsModule.name,
  ])
  .component('systemNotice', iqReact2Angular(SystemNoticeContainer, [], ['$ngRedux']));
