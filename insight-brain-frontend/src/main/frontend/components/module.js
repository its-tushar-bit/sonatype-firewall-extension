/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqCheckbox from './iqCheckbox/iqCheckbox';
import iqRadio from './iqRadio/iqRadio';
import iqBackButton from './iqBackButton/iqBackButton';
import iqTreeViewMultiSelect from './iqTreeViewMultiSelect/iqTreeViewMultiSelect';
import iqTreeViewRadioSelect from './iqTreeViewRadioSelect/iqTreeViewRadioSelect';
import iqPolicyThreatLevelSlider from './iqPolicyThreatLevelSlider/iqPolicyThreatLevelSlider';
import iqTreeViewPolicyThreatLevelSlider from './iqTreeViewPolicyThreatLevelSlider/iqTreeViewPolicyThreatLevelSlider';
import utilityModule from '../utility/utility.module';
import utilityDirectivesModule from '../utility/directives/utility.directives.module';
import iqOrgAppPickerAngular from './iqOrgAppPicker/iqOrgAppPickerAngular';
import iqRenderPlottable from './iqRenderPlottable/iqRenderPlottable';
import coverageDonut from './coverageDonut';
import externalLink from './externalLink/externalLink';

export default angular.module('components', [utilityModule.name, utilityDirectivesModule.name])
    .component('iqCheckbox', iqCheckbox)
    .component('iqRadio', iqRadio)
    .component('iqBackButton', iqBackButton)
    .component('iqPolicyThreatLevelSlider', iqPolicyThreatLevelSlider)
    .component('iqTreeViewMultiSelect', iqTreeViewMultiSelect)
    .component('iqTreeViewRadioSelect', iqTreeViewRadioSelect)
    .component('iqTreeViewPolicyThreatLevelSlider', iqTreeViewPolicyThreatLevelSlider)
    .component('iqOrgAppPickerAngular', iqOrgAppPickerAngular)
    .component('externalLink', externalLink)
    .directive('iqRenderPlottable', iqRenderPlottable)
    .directive('coverageDonut', coverageDonut)
;
