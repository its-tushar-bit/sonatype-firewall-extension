/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { getRolesWithoutLocalMembers } from '../../OrgsAndPolicies/utility/util';

export const findOutIfRolesWithoutLocalMembersExists = (membersByRoles) =>
  getRolesWithoutLocalMembers(membersByRoles).length > 0;
