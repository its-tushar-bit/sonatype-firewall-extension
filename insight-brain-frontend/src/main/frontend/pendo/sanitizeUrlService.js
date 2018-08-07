/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import hash from '../util/hash';
import { zipWith } from 'ramda';

import ownerConstant from '../utility/services/owner.constant';
/**
 * Provides a `sanitize` function that removes the baseUrl and any dynamic route parameters from URLs within the
 * main IQ app.  Not for use within the reports or other bundles.
 * NOTE: This implementation assumes that any hash query parameters (eg timeFilterFeature) do not have dynamic values.
 */
function sanitizeUrlService($urlService, baseUrlService) {
  /*
   * Recursive function to create a parameterized path from router state objects by tracing up the state
   * parent heirarchy.
   * @param state the current router state object being examined
   * @param pathSoFar the path postfix built up so far from processing child states
   */
  function getParameterizedPathFromState(state, pathSoFar) {
    const selfUrl = state.self.url || '';

    // This special string marks the root state and thus the recursive base case
    if (selfUrl === '^') {
      return pathSoFar;
    }
    else {
      const [beforeQuery] = selfUrl.split('?'),
          newPath = beforeQuery + pathSoFar;

      return getParameterizedPathFromState(state.parent, newPath);
    }
  }

  // detect both of ui-router's parameter syntax styles: brace-enclosed and leading colon
  function isUrlParameter(part) {
    return (part[0] === '{' && part[part.length - 1] === '}') || part[0] === ':';
  }

  /**
   * Takes an actual part of the URL (ie, a section between /'s), along with the corresponding
   * parameterized part - which is either the same thing, or something in {}'s, and returns a corresponding
   * URL part that can be used to build up a safe URL.
   */
  function maybeObfuscateUrlPart(realPart, parameterizedPart) {
    if (realPart === parameterizedPart) {
      // a hardcoded part, return as-is
      return realPart;
    }
    else if (isUrlParameter(parameterizedPart)) {
      // we specifically want to exclude ROOT_ORGANIZATION_ID from obfuscation
      return (realPart === ownerConstant.ROOT_ORGANIZATION_ID) ? realPart : hash(realPart);
    }
    else {
      // sanity check
      throw new Error(`Unexpected URL parts. Real: ${realPart}; parameterized: ${parameterizedPart}`);
    }
  }

  return {
    sanitize(url) {
      const baseUrl = baseUrlService.get(),
          indexOfBaseUrl = url.indexOf(baseUrl),
          isExternal = indexOfBaseUrl === -1 && url.indexOf('#') !== 0;

      if (isExternal) {
        return url;
      }
      else {
        const [, beforeHash, , , hash, , hashQuery] = /^([^#]*)?(#(([^?]*)(\?(.*))?))?/.exec(url),
            baseUrlEndIndex = indexOfBaseUrl + baseUrl.length,
            urlAfterBaseUrl = beforeHash ? beforeHash.substring(baseUrlEndIndex) : '';

        if (hash) {
          const routerMatch = $urlService.match({ path: hash }),
              state = routerMatch && routerMatch.rule.state,
              parameterizedHash = state ? getParameterizedPathFromState(state, '', []) : hash,
              parameterizedHashParts = parameterizedHash.split('/'),
              hashParts = hash.split('/'),
              obfuscatedParts = zipWith(maybeObfuscateUrlPart, hashParts, parameterizedHashParts),
              obfuscatedHash = obfuscatedParts.join('/'),
              obfuscatedHashWithQuery = hashQuery ? `${obfuscatedHash}?${hashQuery}` : obfuscatedHash;

          if (hashQuery && hashQuery.indexOf('=') !== -1) {
            // a warning for devs in the future in case we add a dynamic query part
            console.warn('Possible unobfuscated dynamic URL part detected in sanitizeUrlService');
          }

          return `${urlAfterBaseUrl}#${obfuscatedHashWithQuery}`;
        }
        else {
          return urlAfterBaseUrl;
        }
      }
    }
  };
}

sanitizeUrlService.$inject = ['$urlService', 'BaseUrl'];

export default sanitizeUrlService;
