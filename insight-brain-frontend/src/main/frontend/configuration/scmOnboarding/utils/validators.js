/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

function validateIsUrl(val) {
  try {
    if (!val) {
      return null;
    }
    const url = new URL(val);
    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return 'Protocol must be http or https';
    }
    return null;
  } catch (_) {
    return 'Not a valid URL';
  }
}

const validateHostUrl = validateIsUrl;

export { validateHostUrl };
