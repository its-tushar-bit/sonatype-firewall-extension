/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  template: `
    <div class="nx-page-content">
      <sidebar-nav-list class="nx-page-sidebar nx-page-sidebar--nav-sidebar"></sidebar-nav-list>
      <div class="nx-page-main" ui-view></div>
    </div>
  `
};
