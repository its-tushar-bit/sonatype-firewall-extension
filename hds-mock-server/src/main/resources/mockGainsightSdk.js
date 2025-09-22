/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */


// Mock JS file for Gainsight SDK. To test locally, copy and paste here
// the full javascript generated from a working  LC IQ server instance under the
// rest endpoint  GET http://<iq-server>/rest/user-telemetry/javascript.
// This is for LOCAL TESTING ONLY. Do NOT commit/push any changes to this file.

// Below implementation is just a stub mock implementation for Gainsight SDK
const sonatypeStartGainsightPx = () => console.log('Mock sonatypeStartGainsightPx() called');
const setupPendoAdapter = () => console.log('Mock setupPendoAdapter() called');

(function() {
      window.pendo ? setupPendoAdapter() : sonatypeStartGainsightPx()
    }
)();