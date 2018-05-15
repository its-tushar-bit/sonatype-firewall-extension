/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import configurationModule from './configuration/module';
import legacyConfigurationModule from './LegacyConfigurationModule';
import directivesModule from './directives/module';
import dashboardModule from './dashboard/dashboard.module';
import reduxConfigModule from './reduxConfig/module';
import changeDefaultAdminPasswordModule from './changeDefaultAdminPasswordNotice/module';
import applicationReportModule from './applicationReport/module';

export default angular.module('managementApp',
    [
      'MainModule', 'UserModule', 'RoleModule', 'ldap.module', 'owner.manager.module',
      'root.organization.migrate', 'ProductLicense', 'webhook.module', 'systemNoticeConfigurationModule',
      'systemNoticeModule', 'components', directivesModule.name, 'labsModule', configurationModule.name,
      legacyConfigurationModule.name, dashboardModule.name, reduxConfigModule.name,
      changeDefaultAdminPasswordModule.name, applicationReportModule.name
    ]);
