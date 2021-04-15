/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
window.clmEndpoint = {
  type: 'ide',
  showContext: false,
  linkTarget: '_self',
};

// disable the context menu to prevent the user from reloading the page which would break the integration with
// the IDE plugin's Java code
$(document).bind('contextmenu', function () {
  return false;
});
