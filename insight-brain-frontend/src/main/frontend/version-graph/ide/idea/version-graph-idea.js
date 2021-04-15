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
  openView: window.IDEA_openView,
  linkTarget: '_self',
  path: '../../',
};

window.CLM = {
  path: window.clmEndpoint.path + '../../',
  assetsPath: window.clmEndpoint.path + '../',
};
