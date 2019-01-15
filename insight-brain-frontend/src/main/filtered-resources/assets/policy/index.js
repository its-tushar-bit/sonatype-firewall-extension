/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// script for legacy ./index.html, which has been pulled out into a separate file due to Content-Security-Policy
// concerns

// backward-compatibility: redirect old clients (e.g. CI plugin 2.9) to the new management UI
var query = window.location.search.substring(1).split('&'),
    baseUrl = window.location.href.split('policy')[0];

baseUrl += 'assets/index.html#/management/application/';

for (var i = 0; i < query.length; i++) {
  var appId = query[i].split('=');
  if (appId.length > 0 && appId[0] === 'appId') {
    window.location.href = baseUrl + appId[1] + '/policies';
  }
}

window.location.href = baseUrl;
