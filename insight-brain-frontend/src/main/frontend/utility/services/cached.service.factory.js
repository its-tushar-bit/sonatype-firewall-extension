/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * Caches services similar to store, note that callers of get() should not modify the returned object directly as it
 * is shared.
 */
export default function CachedServiceFactory($q, $http, Messages) {
  function createCachedService(urlFn) {
    var deferred,
      usedUrl,
      resolved = false,
      error = false;

    return {
      get: function () {
        var currentUrl = urlFn();
        if (error || !deferred || currentUrl !== usedUrl) {
          // do load
          error = resolved = false;
          usedUrl = currentUrl;

          deferred = $http.get(currentUrl).then(
            function (response) {
              if (currentUrl === usedUrl) {
                resolved = true;
              }
              return response.data;
            },
            function (response) {
              if (currentUrl === usedUrl) {
                error = true;
              }
              return $q.reject(Messages.getHttpErrorMessage(response));
            }
          );
        }

        return deferred;
      },
      refresh: function () {
        if (deferred && resolved) {
          deferred = undefined;
        }
        return this.get();
      },
    };
  }

  return {
    create: function (url) {
      if (typeof url === 'string') {
        url = function () {
          return url;
        };
      }
      return createCachedService(url);
    },
  };
}
CachedServiceFactory.$inject = ['$q', '$http', 'Messages'];
