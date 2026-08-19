/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * @return true if this window is not the top frame of the page, and the top frame is also part of the IQ UI
 */
export default function isIqIframe($window) {
  try {
    // return true if we aren't the top frame AND the top frame allows access (implying it's on the same origin)
    return !!($window.top !== $window && $window.top.document);
  } catch (e) {
    return false;
  }
}
