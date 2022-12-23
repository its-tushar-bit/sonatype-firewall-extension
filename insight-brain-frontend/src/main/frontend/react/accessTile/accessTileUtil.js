/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getRolesWithoutLocalMembers } from '../../OrgsAndPolicies/utility/util';

export const reformatRouteStateParams = (routerState, routerParams) => {
  const isApp = routerState.name.includes('application'),
    isRepositories = routerState.name.includes('repositories'),
    type = isApp ? 'application' : isRepositories ? 'repositories' : 'organization';
  const newTo = `management.edit.${type}.add-access`;
  const params = routerParams || {};
  return { to: newTo, params: params };
};

export const findOutIfRolesWithoutLocalMembersExists = (membersByRoles) =>
  getRolesWithoutLocalMembers(membersByRoles).length > 0;
