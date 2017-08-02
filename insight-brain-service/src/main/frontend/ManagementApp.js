/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import successMetricsConfigurationModule from './configuration/successMetricsConfiguration/successMetricsConfigurationModule';
import legacyConfigurationModule from './LegacyConfigurationModule';
import directivesModule from './directives/module';

export default angular.module('managementApp',
    [
      'MainModule', 'UserModule', 'RoleModule', 'ldap.module', 'owner.manager.module',
      'root.organization.migrate', 'ProductLicense', 'webhook.module', 'systemNoticeConfigurationModule',
      'systemNoticeModule', 'components', directivesModule.name, 'labsModule', successMetricsConfigurationModule.name,
      legacyConfigurationModule.name
    ]
);
