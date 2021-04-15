/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqPolicyThreatLevelSlider.html';
import rangeHighlights from './rangeHighlights';

import '../../lib/bootstrap/bootstrap-slider-2.0.0-sonatype-1';

/**
 * @name iqPolicyThreatLevelSlider
 * @param selectedRange [number, number] array of two values - min Threat and max Threat
 * @param onChange callback expression - called with the changed selectedRange. Context: {value: [number, number]}
 */
const iqPolicyThreatLevelSlider = {
  template,
  bindings: {
    selectedRange: '<',
    onChange: '&',
  },
  controller: iqPolicyThreatLevelSliderController,
  controllerAs: 'vm',
};

export default iqPolicyThreatLevelSlider;

function iqPolicyThreatLevelSliderController() {
  var vm = this;

  Object.assign(vm, {
    rangeHighlights,
  });
}
