/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.configuration

/**
 * @since 1.7
 */
class LDAPUserAndGroupMappingConfigurationPage
    extends LDAPConfigurationPage
{
  static at = { isActiveTab(userAndGroupSettingsTab) }

  static content = {
    // user and group settings editor
    ldapUserMappingEditor { $('#ldapUserMappingEditor') }
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
    checkUserMapping { $('button', text: 'Check User Mapping') }
    checkUserLogin { $('button', text: 'Check Login') }
    reset { $('.ldap-button-group button', text: 'Reset') }
    save { $('.ldap-button-group button', text: 'Save') }
  }
}
