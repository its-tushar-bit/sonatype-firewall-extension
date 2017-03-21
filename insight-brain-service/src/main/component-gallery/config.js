/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../frontend/components/module';
import '../frontend/components/iqCheckbox/iqCheckbox';

export default angular.module('config', ['components'])
    .constant('componentsConfig', {
      'iq-checkbox': 'components/iq-checkbox.html',
    })

    .constant('directivesConfig', {
      'example-directive': 'directives/example-directive.html',
    })

    .constant('stylesConfig', {
      'iq-action-list': 'styles/iq-action-list.html',
      'iq-dropdown': 'styles/iq-dropdown.html',
      'iq-scrollable': 'styles/iq-scrollable.html',
    });
