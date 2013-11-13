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

    appliedMembers(required: false) { $('div[app-security-editor] .selectList:first-child .large-select-list-item') }
    appliedMemberNames(required: false) { appliedMembers.find('.large-select-list-item-title') }
    appliedMember(required: false) { displayName -> appliedMembers.has('.large-select-list-item-title', text: displayName ) }
    appliedMemberEmail(required: false) { appliedMembers.find('.large-select-list-item-detail:not(.right-detail)') }
    appliedMemberRealm(required: false) { appliedMembers.find('.large-select-list-item-detail.right-detail') }

    availableMembers(required: false) { $('div[app-security-editor] .selectList:last-child .large-select-list-item', 'ng-click': 'addUser(user)') }
    availableMemberNames(required: false) { availableMembers.find('.large-select-list-item-title') }
    availableMember(required: false) { displayName -> availableMembers.has('.large-select-list-item-title', text: displayName ) }
    availableMemberEmail(required: false) { availableMembers.find('.large-select-list-item-detail:not(.right-detail)') }
    availableMemberRealm(required: false) { availableMembers.find('.large-select-list-item-detail.right-detail') }

    confirmButton(required: false) { $('button.btn-primary') }
  }
}