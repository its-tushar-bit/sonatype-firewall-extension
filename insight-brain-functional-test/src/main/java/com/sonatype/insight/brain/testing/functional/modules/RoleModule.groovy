/**
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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

    appliedMembers(required: false) { $('div[app-security-editor] .selectList-large:first-child .large-select-list-item') }
    appliedMemberNames(required: false) { appliedMembers.find('span:not(.large-select-list-item-detail):not(.large-select-list-item-right-detail):not(.ui-match)') }
    appliedMember(required: false) { displayName -> appliedMembers.has('span:not(.large-select-list-item-detail):not(.large-select-list-item-right-detail):not(.ui-match)', text: displayName ) }
    appliedMemberEmail(required: false) { appliedMembers.find('.large-select-list-item-detail') }
    appliedMemberRealm(required: false) { appliedMembers.find('.large-select-list-item-right-detail') }

    availableMembers(required: false) { $('div[app-security-editor] .selectList-large:last-child .large-select-list-item') }
    availableMemberNames(required: false) { availableMembers.find('span:not(.large-select-list-item-detail):not(.large-select-list-item-right-detail):not(.ui-match)') }
    availableMember(required: false) { displayName -> availableMembers.has('span:not(.large-select-list-item-detail):not(.large-select-list-item-right-detail):not(.ui-match)', text: displayName ) }
    availableMemberEmail(required: false) { availableMembers.find('.large-select-list-item-detail') }
    availableMemberRealm(required: false) { availableMembers.find('.large-select-list-item-right-detail') }

    confirmButton(required: false) { $('button.btn-primary') }
  }
}