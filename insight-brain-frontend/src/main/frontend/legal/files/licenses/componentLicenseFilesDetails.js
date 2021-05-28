/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  template: `
      <main class="nx-page-main nx-viewport-sized">
        <license-files-details-header></license-files-details-header>
        <div id="component-license-details-content" class="legal-details-content nx-viewport-sized__container">
          <license-files-details-list></license-files-details-list>
          <ui-view></ui-view>
        </div>
      </main>
  `,
};
