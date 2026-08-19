/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * send a custom event to the gainsight analytics service
 *
 * @param {string} eventName
 * @param {object} payload
 */
export function sendGainsightCustomEvent(eventName, payload = {}) {
  if (isAptrinsicDefined()) {
    window.aptrinsic('track', eventName, payload);
  }
}

function isAptrinsicDefined() {
  return typeof window !== 'undefined' && !!window.aptrinsic;
}
