/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  angular.module('managementApp',
    ['MainModule', 'OrganizationModule', 'ApplicationModule', 'proprietary.configuration.module', 'UserModule', 'RoleModule', 'LdapConfiguration', 'owner.manager.module']);
}());
