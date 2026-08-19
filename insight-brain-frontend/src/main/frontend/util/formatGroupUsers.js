/* eslint-disable no-useless-escape */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { ascend, assoc, map, omit, prop, sort } from 'ramda';

const groupSuffix = ' (Group)';
export const formatMembersForTransferList = map((item) => ({ ...item, id: `${item.internalName}${item.type}` }));

export const formatMembersForSaving = map(omit(['id']));

export const formatGroupUsers = map((user) =>
  assoc('displayName', `${user.displayName}${user.type === 'GROUP' ? groupSuffix : ` (${user.internalName})`}`, user)
);

function removeSuffix(user, suffix) {
  return user.displayName.slice(0, -suffix.length);
}

export const removeFormatting = (user) => {
  const internalNameSuffix = ` (${user.internalName})`;
  if (user.type === 'GROUP' && user.displayName.endsWith(groupSuffix)) {
    return assoc('displayName', removeSuffix(user, groupSuffix), user);
  } else if (user.displayName.endsWith(internalNameSuffix)) {
    return assoc('displayName', removeSuffix(user, internalNameSuffix), user);
  }
  return user;
};

export const sortByDisplayName = sort(ascend(prop('displayName')));
