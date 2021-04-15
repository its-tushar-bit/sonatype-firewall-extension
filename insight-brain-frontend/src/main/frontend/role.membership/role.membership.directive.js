/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './role.membership.view.html';

export default function RoleMembershipDirective() {
  return {
    scope: {
      role: '<',
      save: '<',
      groupSearchEnabled: '<',
      originalMembers: '<',
      getCurrentMembers: '=',
      getCurrentMembersToSave: '=',
      isDirty: '=',
    },
    restrict: 'E',
    template,
    controller: 'role.membership.controller',
    controllerAs: 'vm',
    transclude: true,
    bindToController: true,
  };
}
