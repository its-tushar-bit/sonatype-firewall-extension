/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import configurationModule from './configuration/module';
import legacyConfigurationModule from './LegacyConfigurationModule';
import directivesModule from './directives/module';
import componentsModule from './components/module';
import dashboardModule from './dashboard/dashboard.module';
import componentDetailsModule from './componentDetails/module';
import dependencyTreeModule from './DependencyTree/module';
import reduxConfigModule from './reduxConfig/module';
import changeDefaultAdminPasswordModule from './changeDefaultAdminPasswordNotice/module';
import applicationReportModule from './applicationReport/module';
import ownerManagerModule from './owner.manager/owner.manager.module';
import { MainModule } from './MainModule';
import { UserModule } from './security/users/UserModule';
import { SecurityModule } from './security/SecurityModule';
import RoleModule from './security/RoleModule';
import labsModule from './labs/module';
import vulnerabilitySearchModule from './vulnerabilitySearch/module';
import violationPageModule from './violation/module';
import withStoreProvider from './reactAdapter/StoreProvider';
import AdvancedSearchContainer from './advancedSearch/AdvancedSearchContainer';
import waiversModule from './waivers/module';
import firewallModule from './firewall/module';
import quarantinedComponentReportModule from './quarantinedComponentReport/module';
import SystemNoticeContainer from './systemNotice/SystemNoticeContainer';
import innerSourceRepositoryConfigurationModule from './innerSourceRepositoryConfiguration/module';

export default angular
  .module('managementApp', [
    MainModule.name,
    UserModule.name,
    SecurityModule.name,
    RoleModule.name,
    ownerManagerModule.name,
    componentsModule.name,
    directivesModule.name,
    labsModule.name,
    configurationModule.name,
    legacyConfigurationModule.name,
    dashboardModule.name,
    reduxConfigModule.name,
    changeDefaultAdminPasswordModule.name,
    applicationReportModule.name,
    vulnerabilitySearchModule.name,
    violationPageModule.name,
    waiversModule.name,
    firewallModule.name,
    componentDetailsModule.name,
    dependencyTreeModule.name,
    quarantinedComponentReportModule.name,
    innerSourceRepositoryConfigurationModule.name,
  ])
  .component('advancedSearch', react2angular(withStoreProvider(AdvancedSearchContainer), [], ['$ngRedux', '$state']))
  .component('systemNotice', react2angular(withStoreProvider(SystemNoticeContainer), [], ['$ngRedux']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider.state('advancedSearch', {
    component: 'advancedSearch',
    url: '/advancedSearch',
    data: {
      title: 'Advanced Search',
    },
  });
}

routes.$inject = ['$stateProvider'];
