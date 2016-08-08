/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  angular.module('managementApp',
      [
        'MainModule', 'UserModule', 'RoleModule', 'ldap.module', 'owner.manager.module',
        'root.organization.migrate', 'ProductLicense'
      ]);
}());
