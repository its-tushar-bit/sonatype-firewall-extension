/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Provides a `sanitize` function that removes the baseUrl from URLs within the standalone Version Graph App
 */
function sanitizeUrlService(baseUrlService) {
  return {
    sanitize(url) {
      const baseUrl = baseUrlService.get(),
        indexOfBaseUrl = url.indexOf(baseUrl),
        isExternal = indexOfBaseUrl === -1;

      if (isExternal) {
        return url;
      } else {
        const urlWithoutBase = url.substring(baseUrl.length);

        return urlWithoutBase;
      }
    },
  };
}

sanitizeUrlService.$inject = ['BaseUrl'];

export default sanitizeUrlService;
