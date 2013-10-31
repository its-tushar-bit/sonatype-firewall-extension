/**
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class RoleModule extends Module {
  static content = {
    memberNames(required: false) { $('div > div:last-child > div.expandable > span:first-child').text().split(', ') }

    editButton { $('button').has('i.icon-pencil') }

    editor(required: false) { $('div[app-security-editor] > div') }
    queryInput(required: false) { $('input[name=filter]') }

    appliedMembers(required: false) { $('div[app-security-editor] .selectList:first-child .licenseSelectListItem') }
    appliedMemberNames(required: false) { appliedMembers.children('span') }
    appliedMember(required: false) { displayName -> appliedMembers.has('span', text: displayName ) }

    availableMembers(required: false) { $('div[app-security-editor] .selectList:last-child .licenseSelectListItem', 'ng-click': 'addUser(user)') }
    availableMemberNames(required: false) { availableMembers.children('span') }
    availableMember(required: false) { displayName -> availableMembers.has('span', text: displayName ) }

    confirmButton(required: false) { $('button.btn-primary') }
  }
}