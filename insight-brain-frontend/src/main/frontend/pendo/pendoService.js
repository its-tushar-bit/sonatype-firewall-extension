/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function pendoService($http, $q, $window, $document, CLMLocations, sanitizeUrlService) {
  /* eslint-disable */
  // Snippet from Pendo which creates a stub pendo object and adds pendo script, slightly changed to modify the URL and
  // to use Angular wrapper objects.
  (function (p, e, n, d, o) {
    var v, w, x, y, z;
    o = p[d] = p[d] || {};
    o._q = [];
    v = ['initialize', 'identify', 'updateOptions', 'pageLoad'];
    for (w = 0, x = v.length; w < x; ++w)
      (function (m) {
        o[m] =
          o[m] ||
          function () {
            o._q[m === v[0] ? 'unshift' : 'push']([m].concat([].slice.call(arguments, 0)));
          };
      })(v[w]);
    y = e.createElement(n);
    y.async = !0;
    y.src = CLMLocations.getUserTelemetryJavascript();
    z = e.getElementsByTagName(n)[0];
    z.parentNode.insertBefore(y, z);
  })($window, $document[0], 'script', 'pendo');
  /* eslint-enable */

  /**
   * Fetch the user-telemetry configuration and start pendo. It is safe to call this multiple times, for instance
   * to re-initialize pendo after the user logs in
   */
  function start() {
    $http.get(CLMLocations.getUserTelemetryConfig()).then(function (response) {
      const configuration = {
        contentHost: CLMLocations.getUserTelemetryProxy(),
        dataHost: CLMLocations.getUserTelemetryProxy(),
        excludeAllText: true,
        excludeTitle: true,
        guides: {
          disabled: true,
        },
        sanitizeUrl: sanitizeUrlService.sanitize,
        ...response.data,
      };

      $window.pendo.initialize(configuration);
    });
  }

  function flush() {
    if ($window.pendo.flushNow) {
      return $window.pendo.flushNow();
    } else {
      return $q.resolve();
    }
  }

  return {
    start,
    flush,
  };
}

pendoService.$inject = ['$http', '$q', '$window', '$document', 'CLMLocations', 'sanitizeUrlService'];
