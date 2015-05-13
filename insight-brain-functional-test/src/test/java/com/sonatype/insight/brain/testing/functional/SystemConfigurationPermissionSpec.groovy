/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

import com.sonatype.insight.brain.model.security.MembershipMapping
import com.sonatype.insight.brain.model.security.Permission
import com.sonatype.insight.brain.model.security.Role
import com.sonatype.insight.brain.model.security.User

class SystemConfigurationPermissionSpec
extends BaseSpec {
  def setup() {
    productLicenseManager.reset()
    clmLicenseManager.installLicense(null)
  }

  def "cog icon hidden for underprivileged users"() {
    given: "an under privileged user"
    User underprivilegeUser = temporaryEntity.newUser()

    when: "who logs into the system"
    loginAsUserVia(underprivilegeUser.getUsername(), underprivilegeUser.getPassword())

    then: "cannot see the cog menu"
    waitFor { helpLinks.dropdown.present && !systemConfig.present }
  }

  def "cog icon and proper items shown for CONFIGURE_SYSTEM privileged user"() {
    given: "a CONFIGURE_SYSTEM privileged user"
    User systemPrivilegedUser = temporaryEntity.newUser()
    Role role = temporaryEntity.newRole(true, Permission.CONFIGURE_SYSTEM)
    temporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), systemPrivilegedUser.getUsername())

    when: "who logs into the system"
    loginAsUserVia(systemPrivilegedUser.getUsername(), systemPrivilegedUser.getPassword())

    then: "can see the cog menu"
    waitFor { systemConfig.dropdown.present }

    when: "user clicks on the cog menu"
    systemConfig.dropdown.click()

    then: "user sees all menu items except roles and proprietary"
    waitFor { systemConfig.manageUsers.present }
    !systemConfig.manageRoles.present
    systemConfig.manageAdministrators.present
    systemConfig.manageProductLicense.present
    systemConfig.manageLdap.present
    !systemConfig.manageProprietary.present
  }

  def "cog icon and proper items shown for MANAGE_PROPRIETARY privileged user"() {
    given: "a MANAGE_PROPRIETARY privileged user"
    User proprietaryPrivilegedUser = temporaryEntity.newUser()
    Role role = temporaryEntity.newRole(true, Permission.MANAGE_PROPRIETARY)
    temporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), proprietaryPrivilegedUser.getUsername())

    when: "who logs into the system"
    loginAsUserVia(proprietaryPrivilegedUser.getUsername(), proprietaryPrivilegedUser.getPassword())

    then: "can see the cog menu"
    waitFor { systemConfig.dropdown.present }

    when: "user clicks on the cog menu"
    systemConfig.dropdown.click()

    then: "user sees only the proprietary menu item"
    waitFor { systemConfig.manageProprietary.present }
    !systemConfig.manageUsers.present
    !systemConfig.manageRoles.present
    !systemConfig.manageAdministrators.present
    !systemConfig.manageProductLicense.present
    !systemConfig.manageLdap.present
  }

  def "cog icon and proper items shown for VIEW_ROLES privileged user"() {
    given: "a VIEW_ROLES privileged user"
    User privilegedUser = temporaryEntity.newUser()
    Role role = temporaryEntity.newRole(true, Permission.VIEW_ROLES)
    temporaryEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), privilegedUser.getUsername())

    when: "who logs into the system"
    loginAsUserVia(privilegedUser.getUsername(), privilegedUser.getPassword())

    then: "can see the cog menu"
    waitFor { systemConfig.dropdown.present }

    when: "user clicks on the cog menu"
    systemConfig.dropdown.click()

    then: "user sees only the Roles menu item"
    waitFor { systemConfig.manageRoles.present }
    !systemConfig.manageUsers.present
    !systemConfig.manageAdministrators.present
    !systemConfig.manageProductLicense.present
    !systemConfig.manageLdap.present
    !systemConfig.manageProprietary.present
  }
}
