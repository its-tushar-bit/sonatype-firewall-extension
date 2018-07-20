/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function periodDelimiter() {
  return addWordBreakAfterPeriods;

  function addWordBreakAfterPeriods(input) {
    // NOTE: You can't see it, but we are replacing the periods with a period followed by a zero-width space.
    // This makes our periods into word breaking delimiters. Also, we only replace the periods in between words as
    // to preserve version numbers.
    return input.replace(/(?=\.\D+)\.(?=\D+)/g, '.​');
  }
}
