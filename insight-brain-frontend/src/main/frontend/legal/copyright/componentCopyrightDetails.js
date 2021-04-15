/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  template: `
      <main class="nx-page-main nx-viewport-sized">
        <copyright-details-header></copyright-details-header>
        <div id="component-copyright-details-content" class="nx-viewport-sized__container">
          <copyright-list></copyright-list>      
          <ui-view></ui-view>
        </div>
      </main>
  `,
};
