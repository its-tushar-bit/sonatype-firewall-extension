/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './externalLink.html';

export default {
  // final newline in the template file results in whitespace in the DOM, so trim it off
  template: template.trim(),
  controllerAs: 'vm',
  bindings: {
    href: '@',
    linkText: '@',
    onClick: '&',
  },
};
