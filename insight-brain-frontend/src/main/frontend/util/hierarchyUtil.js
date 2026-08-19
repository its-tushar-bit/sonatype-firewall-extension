/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { capitalize } from './jsUtil';

/**
 * Flattens the Org/Apps hierarchy
 */
export function processOwnerHierarchy(context) {
  // note that since the context data only includes the ancestors of the owner, `children` should
  // never have more than one element
  const processedChildren = context.children ? processOwnerHierarchy(context.children[0]) : [],
    { type, id, publicId, name } = context,
    label = capitalize(type);

  return processedChildren.concat({ type, id, publicId, name, label });
}
