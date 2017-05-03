/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../frontend/components/module';
import '../frontend/components/iqCheckbox/iqCheckbox';
import '../frontend/components/iqRadio/iqRadio';

import charts from './charts/module';

export default angular.module('config', ['components', charts.name])
    .constant('componentsConfig', {
      'iq-checkbox': 'components/iq-checkbox.html',
      'iq-radio': 'components/iq-radio.html',
    })

    .constant('directivesConfig', {
      'example-directive': 'directives/example-directive.html',
    })

    .constant('stylesConfig', {
      'iq-action-list': 'styles/iq-action-list.html',
      'iq-dropdown': 'styles/iq-dropdown.html',
      'iq-pull-right': 'styles/iq-pull-right.html',
      'iq-scrollable': 'styles/iq-scrollable.html',
      'iq-alerts': 'styles/iq-alerts.html',
    });
