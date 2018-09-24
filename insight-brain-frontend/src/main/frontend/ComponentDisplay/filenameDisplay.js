/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default {
  controllerAs: 'vm',
  bindings: {
    filename: '<'
  },
  template: `
    <div class="filename">
      <em>{{vm.filename}}</em>
    </div>`
};
