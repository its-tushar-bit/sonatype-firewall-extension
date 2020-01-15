/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentsModule from '../frontend/components/module';
import directivesModule from '../frontend/directives/module';
import utilityModule from '../frontend/utility/utility.module';
import bootstrapAddonsModule from '../frontend/util/BootstrapAddonsModule';
import charts from './charts/module';
import iqModalModule from './styles/iq-modal/module';
import iqTreeViewMultiSelectModule from './components/iqTreeViewMultiSelect/module';
import iqTreeViewRadioSelectModule from './components/iqTreeViewRadioSelect/module';
import iqPolicyThreatLevelSliderModule from './components/iqPolicyThreatLevelSlider/module';
import iqTreeViewPolicyThreatLevelSliderModule from './components/iqTreeViewPolicyThreatLevelSlider/module';
import iqOrgAppPickerModule from './components/iq-org-app-picker/module';
import submitValidationModule from './directives/submit-validation/module';
import iqFormLayoutModule from './styles/iq-form-layout/module';

export default angular.module('config',
    [
      componentsModule.name, directivesModule.name, charts.name, utilityModule.name, iqModalModule.name,
      iqTreeViewMultiSelectModule.name, iqOrgAppPickerModule.name, submitValidationModule.name, iqFormLayoutModule.name,
      iqTreeViewRadioSelectModule.name, bootstrapAddonsModule.name, iqPolicyThreatLevelSliderModule.name,
      iqTreeViewPolicyThreatLevelSliderModule.name
    ])
    .constant('componentsConfig', {
      'iq-checkbox': 'components/iq-checkbox.html',
      'iq-radio': 'components/iq-radio.html',
      'iq-back-button': 'components/iq-back-button.html',
      'color-picker': 'components/color-picker.html',
      'dropdown-selector': 'components/dropdown-selector.html',
      'iq-policy-threat-level-slider': 'components/iqPolicyThreatLevelSlider/iqPolicyThreatLevelSlider.html',
      'iq-tree-view-multi-select': 'components/iqTreeViewMultiSelect/iq-tree-view-multi-select.html',
      'iq-tree-view-radio-select': 'components/iqTreeViewRadioSelect/iq-tree-view-radio-select.html',
      'iq-tree-view-policy-threat-level-slider': 'components/iqTreeViewPolicyThreatLevelSlider/iqTreeViewPolicyThreatLevelSlider.html',
      'iq-org-app-picker': 'components/iq-org-app-picker/iq-org-app-picker.html'
    })

    .constant('directivesConfig', {
      'load-wrapper': 'directives/load-wrapper.html',
      'iq-tooltip': 'directives/iq-tooltip.html',
      'submit-validation': 'directives/submit-validation/submit-validation.html',
      'iq-scroll-to-top': 'directives/iq-scroll-to-top.html'
    })

    .constant('layoutConfig', {
      'page layout': 'styles/page-layout.html',
      'iq-tile': 'styles/iq-tile.html',
      'iq-pull-right': 'styles/iq-pull-right.html',
      'iq-scrollable': 'styles/iq-scrollable.html',
      'iq-grid': 'styles/iq-grid.html'
    })

    .constant('widgetsConfig', {
      'iq-button': 'styles/iq-btn.html',
      'iq-nav-pills': 'styles/iq-nav-pills.html',
      'iq-dropdown': 'styles/iq-dropdown.html',
      'iq-alerts': 'styles/iq-alerts.html',
      'iq-modal': 'styles/iq-modal/iq-modal.html',
      'iq-read-only': 'styles/iq-read-only.html',
      'iq-tree-view': 'styles/iq-tree-view.html',
      'iq-list': 'styles/iq-list.html',
      'iq-list--clickable': 'styles/iq-list--clickable.html',
      'iq-threat-indicators': 'styles/iq-threat-indicators.html',
      'iq-counter': 'styles/iq-counter.html',
      'iq-text-indicators': 'styles/iq-text-indicators.html',
      'iq-threat-bar': 'styles/iq-threat-bar.html',
      'iq-pagination': 'styles/iq-pagination.html',
      'iq-tabs': 'styles/iq-tabs.html'
    })

    .constant('htmlConfig', {
      'iq-table': 'styles/iq-table.html',
      'iq-table with fixed header': 'styles/iq-table-fixed.html',
      'iq-text-input': 'styles/iq-form-text-input.html',
      'iq-textarea': 'styles/iq-form-textarea.html',
      'iq-form-layout': 'styles/iq-form-layout/iq-form-layout.html'
    });
