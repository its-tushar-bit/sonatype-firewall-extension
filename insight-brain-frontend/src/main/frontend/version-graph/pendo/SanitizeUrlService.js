/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { BASE_URL } from 'MainRoot/util/urlUtil';

/**
 * Provides a `sanitize` function that removes the baseUrl from URLs within the standalone Version Graph App
 */
function SanitizeUrlService() {
  return {
    sanitize(url) {
      const indexOfBaseUrl = url.indexOf(BASE_URL),
        isExternal = indexOfBaseUrl === -1;

      if (isExternal) {
        return url;
      } else {
        const urlWithoutBase = url.substring(BASE_URL.length);

        return urlWithoutBase;
      }
    },
  };
}

export default SanitizeUrlService;
