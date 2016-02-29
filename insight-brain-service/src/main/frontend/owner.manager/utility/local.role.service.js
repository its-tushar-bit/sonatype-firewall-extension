/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LocalRoleService() {
    return {
      getRolesWithLocalMembers: getRolesWithLocalMembers,
      getRolesWithoutLocalMembers: getRolesWithoutLocalMembers
    };

    function getRolesWithLocalMembers(membersByRoles) {
      if (membersByRoles) {
        return membersByRoles.filter(function(membersByRole) {
          return membersByRole.membersByOwner[0].members.length > 0;
        });
      }
      else {
        return [];
      }
    }

    function getRolesWithoutLocalMembers(membersByRoles) {
      if (membersByRoles) {
        return membersByRoles.filter(function(membersByRole) {
          return membersByRole.membersByOwner[0].members.length === 0;
        });
      }
      else {
        return [];
      }
    }
  }

  angular //
    .module('owner.manager.module') //
    .service('local.role.service', LocalRoleService);

}(angular));
