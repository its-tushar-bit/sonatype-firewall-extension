/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import hash from '../../util/hash';

/**
 * Provides a `sanitize` function that removes the baseUrl and app and report ids from URLs within pages
 * containing the cip-loader (eg the app report)
 */
function sanitizeUrlService(baseUrlService) {
  // A regex to identify the replaceable parameters in a URL
  const urlParametersRegex = /\/rest\/report\/([^/]+)\/([^/]+)/;

  return {
    sanitize(url) {
      const baseUrl = baseUrlService.get(),
        indexOfBaseUrl = url.indexOf(baseUrl),
        isExternal = indexOfBaseUrl === -1;

      if (isExternal) {
        return url;
      } else {
        const urlWithoutBase = url.substring(baseUrl.length),
          regexResult = urlParametersRegex.exec(urlWithoutBase);

        if (regexResult) {
          const [, applicationId, reportId] = regexResult,
            hashedAppId = hash(applicationId),
            hashedReportId = hash(reportId),
            obfuscatedUrl = urlWithoutBase.replace(
              urlParametersRegex,
              `/rest/report/${hashedAppId}/${hashedReportId}`
            );

          return obfuscatedUrl;
        } else {
          return urlWithoutBase;
        }
      }
    },
  };
}

sanitizeUrlService.$inject = ['BaseUrl'];

export default sanitizeUrlService;
