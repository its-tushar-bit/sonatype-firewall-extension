/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Provides a `sanitize` function that removes the baseUrl and any dynamic route parameters from URLs within the
 * firewall reports
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
        const urlWithoutBase = url.substring(baseUrl.length),
          urlWithoutQuery = urlWithoutBase.split('?')[0];

        // the page URL for the firewall report includes the repository id as a query parameter.  Just strip that off
        return urlWithoutQuery;
      }
    },
  };
}

sanitizeUrlService.$inject = ['BaseUrl'];

export default sanitizeUrlService;
