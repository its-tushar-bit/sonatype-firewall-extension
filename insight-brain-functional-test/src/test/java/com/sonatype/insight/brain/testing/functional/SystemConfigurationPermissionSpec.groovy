/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional

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

  def "cog icon and proper items shown for system_config privileged user"() {
    given: "a system_config privileged user"
    User systemPrivilegedUser = temporaryEntity.newUser()
    Role role = temporaryEntity.newRole(true, Permission.CONFIGURE_SYSTEM)
    temporaryEntity.newMembershipMapping("global", role.getId(), systemPrivilegedUser.getUsername())

    when: "who logs into the system"
    loginAsUserVia(systemPrivilegedUser.getUsername(), systemPrivilegedUser.getPassword())

    then: "can see the cog menu"
    waitFor { systemConfig.dropdown.present }

    when: "user clicks on the cog menu"
    systemConfig.dropdown.click()

    then: "user sees all menu items except proprietary"
    waitFor { systemConfig.manageUsers.present }
    systemConfig.manageRoles.present
    systemConfig.manageAdministrators.present
    systemConfig.manageProductLicense.present
    systemConfig.manageLdap.present
    !systemConfig.manageProprietary.present
  }

  def "cog icon and proper items shown for manage_proprietary privileged user"() {
    given: "a manage_proprietary privileged user"
    User proprietaryPrivilegedUser = temporaryEntity.newUser()
    Role role = temporaryEntity.newRole(true, Permission.MANAGE_PROPRIETARY)
    temporaryEntity.newMembershipMapping("global", role.getId(), proprietaryPrivilegedUser.getUsername())

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
}
