/* eslint-disable no-useless-escape */
/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { ascend, assoc, map, omit, prop, sort } from 'ramda';

export const formatMembersForTransferList = map((item) => ({ ...item, id: `${item.internalName}${item.type}` }));

export const formatMembersForSaving = map(omit(['id']));

export const formatGroupUsers = map((user) =>
  assoc('displayName', `${user.displayName}${user.type === 'GROUP' ? ' (Group)' : ''}`, user)
);

export const removeFormatGroupUsers = (user) =>
  assoc('displayName', user.displayName.replace(new RegExp(' \\(Group\\)(?!.* \\(Group\\))'), ''), user);

export const sortByDisplayName = sort(ascend(prop('displayName')));
