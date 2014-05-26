/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

    requiredFields { [userObjectClass, userIDAttribute, userRealNameAttribute, userEmailAttribute]}

    //controls
    checkUserMapping { $('.ldap-button-group > button:first-child') }
    checkUserLogin { $('.ldap-button-group > button:nth-child(2)') }
    reset { $('.ldap-button-group div button:first-child') }
    save { $('.ldap-button-group button.btn-primary') }

    //test user mapping dialog
    userMappingDialog(required: false) { $('div.modal-ldap') }
    userMappingDialogClose(required: false) { $('div.modal-ldap button') }

    //test user login dialog
    userLoginDialog(required: false) { $('#ldap-check-login-modal') }
    userLoginUsername(required: false) { $('#ldap-check-login-modal input[name="username"]') }
    userLoginPassword(required: false) { $('#ldap-check-login-modal input[name="password"]') }
    userLoginDialogTest(required: false) { $('#ldap-check-login-modal button.btn-primary') }
    userLoginDialogClose(required: false) { $('#ldap-check-login-modal button:first-child') }
  }
}
