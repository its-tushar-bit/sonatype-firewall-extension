/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

function validateZScalerHostName(val) {
  if (!val) return 'URL is required';

  const trimmedVal = val.trim();

  try {
    const url = new URL(trimmedVal);

    if (url.protocol !== 'http:' && url.protocol !== 'https:') {
      return 'Protocol must be http or https';
    }

    if (!url.hostname || url.hostname.trim() === '') {
      return 'Hostname is required in URL';
    }

    if (url.pathname !== '/' || val.trim().endsWith('/')) {
      return 'Only base URL allowed - no paths or trailing slashes';
    }

    if (url.search || url.hash) {
      return 'Query parameters and fragments not allowed';
    }

    return null;
  } catch {
    return 'Not a valid URL';
  }
}

function validateZscalerApiKey(val) {
  if (!val) return 'API key is required';

  if (val.length !== 12) {
    return 'API key must be 12 characters.';
  }
  return null;
}

export { validateZScalerHostName, validateZscalerApiKey};
