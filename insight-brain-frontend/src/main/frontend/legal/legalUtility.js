/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {findIndex, propEq} from 'ramda';

export function isScopeOverride(originalOwnerId, ownerId, availableScopeValues) {
  const originalOwnerLevel = findIndex(propEq('id', originalOwnerId), availableScopeValues);
  const newOwnerLevel = findIndex(propEq('id', ownerId), availableScopeValues);
  return originalOwnerLevel > newOwnerLevel;
}
