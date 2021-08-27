/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  template: `
      <main class="nx-page-main nx-viewport-sized">
        <notice-details-header></notice-details-header>
        <div id="component-notice-details-content" class="legal-details-content nx-viewport-sized__container">
          <notice-details-list class="nx-scrollable nx-viewport-sized__scrollable"></notice-details-list>
          <ui-view></ui-view>
        </div>
      </main>
  `,
};
