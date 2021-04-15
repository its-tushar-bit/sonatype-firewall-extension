/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.clmEndpoint = {
  type: 'ide',
  migrate: false,
  selectApplication: false,
  viewDetails: true,
  linkTarget: '_self',
  path: '../../',
};

window.CLM = {
  path: window.clmEndpoint.path + '../../',
  assetsPath: window.clmEndpoint.path + '../',
};

// disable the context menu to prevent the user from reloading the page
$(document).bind('contextmenu', function () {
  return false;
});
