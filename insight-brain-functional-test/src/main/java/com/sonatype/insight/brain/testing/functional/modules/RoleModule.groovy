/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing.functional.modules

import geb.Module

class RoleModule
    extends Module
{
  static content = {
    memberNames(required: false) { $('.member-list').text().split(', ') }

    editButton { $('button').has('i.icon-pencil') }

    editor(required: false) { $('div[app-security-editor] > div') }
    queryInput(required: false) { $('input[name=filter]') }
    searchButton (required: false) { $('#user-search-button') }
    queryHelp(required: false) { $('#user-search-help') }
    queryHelpPopover(required: false) { page.$('.popover') }

    appliedMembersList(required: false) { $('div[app-security-editor] .selectList-large:first-child') }
    appliedMembers(required: false) { appliedMembersList.find('.large-select-list-item') }
    appliedMemberNames(required: false) {
      appliedMembers.find('span:not(.large-select-list-item-detail):not(.ui-match)')
    }
    appliedMember(required: false) { displayName ->
      appliedMembers.has('span:not(.large-select-list-item-detail):not(.ui-match)', text: displayName)
    }
    appliedMemberUsername(required: false) {
      appliedMembers.
          find('.large-select-list-item-content > .flexbox-container:first-child > .large-select-list-item-detail')
    }
    appliedMemberEmail(required: false) {
      appliedMembers.find(
          '.large-select-list-item-content > .flexbox-container:last-child > .large-select-list-item-detail:first-child')
    }
    appliedMemberRealm(required: false) {
      appliedMembers.find(
          '.large-select-list-item-content > .flexbox-container:last-child > .large-select-list-item-detail:last-child')
    }

    availableMembersList(required: false) { $('div[app-security-editor] .selectList-large:last-child') }
    availableMembers(required: false) { availableMembersList.find('.large-select-list-item') }
    availableMemberNames(required: false) {
      availableMembers.find('span:not(.large-select-list-item-detail):not(.ui-match)')
    }
    availableMember(required: false) { displayName ->
      availableMembers.has('span:not(.large-select-list-item-detail):not(.ui-match)', text: displayName)
    }
    availableMemberUsername(required: false) {
      availableMembers.
          find('.large-select-list-item-content > .flexbox-container:first-child > .large-select-list-item-detail')
    }
    availableMemberEmail(required: false) {
      availableMembers.find(
        '.large-select-list-item-content > .flexbox-container:last-child > .large-select-list-item-detail:first-child')
    }
    availableMemberRealm(required: false) {
      availableMembers.find(
          '.large-select-list-item-content > .flexbox-container:last-child > .large-select-list-item-detail:last-child')
    }

    confirmButton(required: false) { $('button.btn-primary') }

    usersButton(required: false) { $('button[ng-click="mtype=null"]') }
    groupsButton(required: false) { $('button[ng-click="mtype=\\"groups\\""]') }
    addGroupButton(required: false) { $('button[ng-click="addGroup()"]') }
  }
}
