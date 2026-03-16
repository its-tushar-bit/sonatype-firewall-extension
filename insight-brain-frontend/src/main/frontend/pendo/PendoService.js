/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getUserTelemetryConfig, getUserTelemetryProxy, getUserTelemetryJavascript } from 'MainRoot/util/CLMLocation';

export default function PendoService(sanitizeUrlService) {
  /* eslint-disable */
  // Snippet from Pendo which creates a stub pendo object and adds pendo script, slightly changed to modify the URL
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
    y.src = getUserTelemetryJavascript();
    z = e.getElementsByTagName(n)[0];
    z.parentNode.insertBefore(y, z);
  })(window, document, 'script', 'pendo');
  /* eslint-enable */

  /**
   * Fetch the user-telemetry configuration and start pendo. It is safe to call this multiple times, for instance
   * to re-initialize pendo after the user logs in
   */
  async function start() {
    const response = await axios.get(getUserTelemetryConfig());
    const configuration = {
      contentHost: getUserTelemetryProxy(),
      dataHost: getUserTelemetryProxy(),
      excludeAllText: true,
      excludeTitle: true,
      guides: {
        disabled: true,
      },
      sanitizeUrl: sanitizeUrlService.sanitize,
      ...response.data,
    };

    window.pendo.initialize(configuration);
  }

  async function flush() {
    if (window.pendo.flushNow) {
      return window.pendo.flushNow();
    } else {
      return;
    }
  }

  return {
    start,
    flush,
  };
}
