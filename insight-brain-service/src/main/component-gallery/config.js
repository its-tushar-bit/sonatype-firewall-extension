/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../frontend/components/module';
import directivesModule from '../frontend/directives/module';
import utilityDirectivesModule from '../frontend/utility/directives/utility.directives.module';
import charts from './charts/module';
import iqModalModule from './styles/iq-modal/module';
import iqTreeViewMultiSelectModule from './components/iqTreeViewMultiSelect/module';
import iqOrgAppPickerModule from './components/iq-org-app-picker/module';

export default angular.module('config',
    [
      componentsModule.name, directivesModule.name, charts.name, utilityDirectivesModule.name, iqModalModule.name,
      iqTreeViewMultiSelectModule.name, iqOrgAppPickerModule.name
    ])
    .constant('componentsConfig', {
      'iq-checkbox': 'components/iq-checkbox.html',
      'iq-radio': 'components/iq-radio.html',
      'iq-back-button': 'components/iq-back-button.html',
      'iq-tree-view-multi-select': 'components/iqTreeViewMultiSelect/iq-tree-view-multi-select.html',
      'iq-org-app-picker': 'components/iq-org-app-picker/iq-org-app-picker.html'
    })

    .constant('directivesConfig', {
      'load-wrapper': 'directives/load-wrapper.html',
      'iq-tooltip': 'directives/iq-tooltip.html'
    })

    .constant('stylesConfig', {
      'iq-tile-header': 'styles/iq-tile-header.html',
      'iq-action-list': 'styles/iq-action-list.html',
      'iq-dropdown': 'styles/iq-dropdown.html',
      'iq-pull-right': 'styles/iq-pull-right.html',
      'iq-scrollable': 'styles/iq-scrollable.html',
      'iq-alerts': 'styles/iq-alerts.html',
      'iq-modal': 'styles/iq-modal/iq-modal.html',
      'iq-table': 'styles/iq-table.html',
      'iq-tree-view': 'styles/iq-tree-view.html',
      'iq-read-only': 'styles/iq-read-only.html'
    });
