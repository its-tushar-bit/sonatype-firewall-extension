/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import configurationModule from './configuration/module';
import legacyConfigurationModule from './LegacyConfigurationModule';
import directivesModule from './directives/module';
import componentsModule from './components/module';
import dashboardModule from './dashboard/dashboard.module';
import reduxConfigModule from './reduxConfig/module';
import changeDefaultAdminPasswordModule from './changeDefaultAdminPasswordNotice/module';
import applicationReportModule from './applicationReport/module';
import ownerManagerModule from './owner.manager/owner.manager.module';
import {MainModule} from './MainModule';
import {UserModule} from './security/UserModule';
import RoleModule from './security/RoleModule';
import systemNoticeModule from './systemNotice/systemNoticeModule';
import labsModule from './labs/module';
import vulnerabilitySearchModule from './vulnerabilitySearch/module';
import violationPageModule from './violation/module';
import {react2angular} from 'react2angular';
import withStoreProvider from './reactAdapter/StoreProvider';
import AdvancedSearchContainer from './advancedSearch/AdvancedSearchContainer';
import waiversModule from './waivers/module';
import reportModule from './applicationReport/react/module';
import advancedLegalModule from './advancedLegal/module';
import FirewallContainer from './firewall/FirewallContainer';

export default angular.module('managementApp',
    [
      MainModule.name, UserModule.name, RoleModule.name, ownerManagerModule.name, systemNoticeModule.name,
      componentsModule.name, directivesModule.name, labsModule.name, configurationModule.name,
      legacyConfigurationModule.name, dashboardModule.name, reduxConfigModule.name,
      changeDefaultAdminPasswordModule.name, applicationReportModule.name, vulnerabilitySearchModule.name,
      violationPageModule.name, waiversModule.name, reportModule.name, advancedLegalModule.name
    ])
    .component('advancedSearch', react2angular(withStoreProvider(AdvancedSearchContainer), [], ['$ngRedux', '$state']))
    .component('firewall', react2angular(withStoreProvider(FirewallContainer), [], ['$ngRedux', '$state']))
    .config(routes);

function routes($stateProvider) {
  $stateProvider
      .state('advancedSearch', {
        component: 'advancedSearch',
        url: '/advancedSearch',
        data: {
          title: 'Advanced Search'
        }
      })
      .state('firewall', {
        component: 'firewall',
        url: '/firewall',
        data: {
          title: 'Firewall'
        }
      });
}

routes.$inject = ['$stateProvider'];
