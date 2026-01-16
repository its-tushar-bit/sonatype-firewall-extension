/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angularDebug */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import configurationModule from './configuration/module';
import store from './reduxConfig/store';
import { setUrlService } from './pendo/mainBundlePendoService';
import { initialize as initializeRouteStateUtilService } from './utility/services/routeStateUtilService';
import { selectUsername } from 'MainRoot/user/userSessionSelectors';
import { selectError } from 'MainRoot/session/appErrorSelectors';
import legacyConfigurationModule from './LegacyConfigurationModule';
import dashboardModule from './dashboard/dashboard.module';
import componentDetailsModule from './componentDetails/module';
import dependencyTreeModule from './DependencyTree/module';
import atlassianCrowdConfigurationModule from './configuration/crowd/module';
import './reduxConfig/store';
import reduxUiRouterModule from './reduxUiRouter/module';
import ChangeDefaultAdminPasswordNotice from './changeDefaultAdminPasswordNotice/ChangeDefaultAdminPasswordNotice';
import applicationReportModule from './applicationReport/module';
import ownerManagerModule from './OrgsAndPolicies/owner.manager.module';
import main from './main';
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
import operationalReportingModule from 'MainRoot/operationalReporting/module';
import sastScanModule from 'MainRoot/sastScan/module';
import prioritiesPageModule from 'MainRoot/development/prioritiesPage/priorities.page.module';
import sbomManagerModule from 'MainRoot/sbomManager/sbom.manager.module';
import advancedSearchModule from 'MainRoot/advancedSearch/module';
import developerModule from 'MainRoot/development/developer.module';
import applicationLatestEvaluationsModule from 'MainRoot/applicationLatestEvaluations/module';
import RootRouteModule from './RootRouteModule';
import IqHttpInterceptorsModule from './utilAngular/IqHttpInterceptors';
import ReportModule from './ReportApp';
import react2ShellModule from './report/react2shell/react2shell.module';
import Report from 'MainRoot/OrgsAndPolicies/repositories/repositoryResultsSummaryPage/module';
import legalModule from './legal/legal.module';
import loginModalModule from './user/LoginModal/module';
import toastContainerModule from './toastContainer/module';
import routeProductLicenseValidator from './routeProductLicenseValidator/module';
import displayThemeModule from './configuration/displayTheme/module';
import modalContainerModule from './modalContainer/module';
import footerModule from './react/Footer/module';
import MainHeader from './mainHeader/MainHeader.jsx';
import NavigationContainer from './navigationContainer/NavigationContainer';

export default angular
  .module('managementApp', [
    'ui.router',
    RootRouteModule.name,
    ReportModule.name,
    react2ShellModule.name,
    Report.name,
    IqHttpInterceptorsModule.name,
    UserModule.name,
    SecurityModule.name,
    RoleModule.name,
    ownerManagerModule.name,
    labsModule.name,
    configurationModule.name,
    legacyConfigurationModule.name,
    dashboardModule.name,
    legalModule.name,
    reduxUiRouterModule.name,
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
    operationalReportingModule.name,
    sastScanModule.name,
    sbomManagerModule.name,
    prioritiesPageModule.name,
    advancedSearchModule.name,
    developerModule.name,
    standaloneFirewallModule.name,
    applicationLatestEvaluationsModule.name,
    loginModalModule.name,
    toastContainerModule.name,
    routeProductLicenseValidator.name,
    displayThemeModule.name,
    modalContainerModule.name,
    footerModule.name,
  ])
  .component('mainHeader', iqReact2Angular(MainHeader, ['clmServerVersion'], ['$state']))
  .component(
    'navigationContainer',
    iqReact2Angular(NavigationContainer, ['clmServerVersion'], ['$rootScope', '$state'])
  )
  .component('systemNotice', iqReact2Angular(SystemNoticeContainer, [], []))
  .component('changeDefaultAdminPasswordNotice', iqReact2Angular(ChangeDefaultAdminPasswordNotice, [], []))
  .config([
    '$compileProvider',
    function configureAngularTemplateCompilation($compileProvider) {
      /**
       * Allow for images to be sourced from blobs. This was removed from AngularJS with closed issue:
       * https://github.com/angular/angular.js/issues/3889
       */
      $compileProvider.imgSrcSanitizationWhitelist(/^\s*(https?|ftp|file|blob):|data:image\//);
      $compileProvider.debugInfoEnabled(angularDebug);
    },
  ])
  .run([
    '$rootScope',
    '$state',
    '$urlRouter',
    '$urlService',
    function initializeRoutingServices($rootScope, $state, $urlRouter, $urlService) {
      // Make $state available globally in templates
      $rootScope.$state = $state;
      // Initialize the singleton pendoService with urlService
      setUrlService($urlService);

      // Initialize the ES6 routeStateUtilService module with Angular dependencies
      initializeRouteStateUtilService($state, store);
    },
  ])
  .run([
    '$rootScope',
    function syncRootScopeFromRedux($rootScope) {
      // There are still a few places, mainly in index.html, that require properties on the $rootScope. Sync them
      // from redux
      const unsubscribe = store.subscribe(() => {
        const state = store.getState();

        // Sync username
        const username = selectUsername(state);
        if (username) {
          $rootScope.username = username;
        } else {
          delete $rootScope.username;
        }

        // Sync error
        const error = selectError(state);
        if (error) {
          $rootScope.error = error;
        } else {
          delete $rootScope.error;
        }

        // Trigger Angular digest cycle to update the view
        // Use $evalAsync to safely trigger digest from outside Angular context
        $rootScope.$evalAsync();
      });

      // Initialize from Redux state
      const initialState = store.getState();

      const initialUsername = selectUsername(initialState);
      if (initialUsername) {
        $rootScope.username = initialUsername;
      }

      const initialError = selectError(initialState);
      if (initialError) {
        $rootScope.error = initialError;
      }

      $rootScope.$on('$destroy', unsubscribe);
    },
  ])
  .run(['$state', '$transitions', main]);
