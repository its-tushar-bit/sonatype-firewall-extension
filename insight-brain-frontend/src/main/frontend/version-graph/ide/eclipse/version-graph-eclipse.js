/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.clmEndpoint = {
  type: 'ide',
  migrate: true,
  selectApplication: false,
  viewDetails: true,
  openView: function (scope, value) {
    scope.$emit('openView', value);
  },
  linkTarget: '_self',
  path: '../../',
};

window.CLM = {
  path: window.clmEndpoint.path + '../../',
  assetsPath: window.clmEndpoint.path + '../',
};

// disable the context menu to prevent the user from reloading the page which would break the integration with
// the IDE plugin's Java code
$(document).bind('contextmenu', function () {
  return false;
});

$(document).ready(function () {
  if (navigator.platform === 'MacIntel') {
    $('html').addClass('macos');
  }
});
