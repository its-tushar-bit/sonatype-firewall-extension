/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Surrounds non-empty String input with supplied prefix and suffix
 */
export default function wrapWithFilter() {
  return function (input, left, right) {
    if (!input || input.length === 0) {
      return '';
    }
    return left + input + right;
  };
}
