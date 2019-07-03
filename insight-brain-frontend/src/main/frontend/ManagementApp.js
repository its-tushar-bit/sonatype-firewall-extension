/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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
import rootOrganizationMigrateModule from './root.organization.migrate/root.organization.migrate.module';
import systemNoticeModule from './systemNotice/systemNoticeModule';
import labsModule from './labs/module';
import vulnerabilitySearchModule from './vulnerabilitySearch/module';

export default angular.module('managementApp',
    [
      MainModule.name, UserModule.name, RoleModule.name, ownerManagerModule.name, rootOrganizationMigrateModule.name,
      systemNoticeModule.name, componentsModule.name, directivesModule.name, labsModule.name, configurationModule.name,
      legacyConfigurationModule.name, dashboardModule.name, reduxConfigModule.name,
      changeDefaultAdminPasswordModule.name, applicationReportModule.name, vulnerabilitySearchModule.name
    ]);
