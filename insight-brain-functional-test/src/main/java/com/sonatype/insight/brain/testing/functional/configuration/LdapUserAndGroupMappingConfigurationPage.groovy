/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

/**
 * @since 1.7
 */
class LdapUserAndGroupMappingConfigurationPage
    extends LdapConfigurationPage
{
  static at = { userBaseDN.displayed }

  static content = {
    // user and group settings editor
    userBaseDN { $('#userBaseDN') }
    userSubtree { $('#userSubtree') }
    userObjectClass { $('#userObjectClass') }
    userFilter { $('#userFilter') }
    userIDAttribute { $('#userIDAttribute') }
    userRealNameAttribute { $('#userRealNameAttribute') }
    userEmailAttribute { $('#userEmailAttribute') }
    useUserPasswordAttribute { $('#useUserPasswordAttribute') }
    userPasswordAttribute { $('#userPasswordAttribute') }
    groupMappingType { $('#groupMappingType') }
    groupBaseDN { $('#groupBaseDN') }
    groupSubtree { $('#groupSubtree') }
    groupObjectClass { $('#groupObjectClass') }
    groupIDAttribute { $('#groupIDAttribute') }
    groupMemberAttribute { $('#groupMemberAttribute') }
    groupMemberFormat { $('#groupMemberFormat') }
    userMemberOfGroupAttribute { $('#userMemberOfGroupAttribute') }

    requiredFields { [userObjectClass, userIDAttribute, userRealNameAttribute, userEmailAttribute] }

    //controls
    checkUserMapping { $('#ldap-mapping-check') }
    checkUserLogin { $('#ldap-mapping-checklogin') }
    cancel { $('#ldap-mapping-cancel') }
    save { $('#ldap-mapping-save') }

    //test user mapping dialog
    userMappingDialog(required: false) { $('div.modal-ldap') }
    userMappingDialogClose(required: false) { $('div.modal-ldap button') }

    //test user login dialog
    userLoginDialog(required: false) { $('#ldap-check-login-modal') }
    userLoginUsername(required: false) { $('#ldap-check-login-modal input[name="username"]') }
    userLoginPassword(required: false) { $('#ldap-check-login-modal input[name="password"]') }
    userLoginDialogTest(required: false) { $('#ldap-check-login-modal .btn-primary') }
    userLoginDialogClose(required: false) { $('#ldap-check-login-modal .btn-cancel') }
  }
}
