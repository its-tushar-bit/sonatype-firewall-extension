/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqTreeViewPolicyThreatLevelSlider.html';

/**
 * @name iqTreeViewPolicyThreatLevelSlider
 * @param selectedRange [number, number] array of two values - min Threat and max Threat
 * @param onChange callback expression - called with the changed selectedRange. Context: {value: [number, number]}
 */
const iqTreeViewPolicyThreatLevelSlider = {
  template,
  bindings: {
    selectedRange: '<',
    onChange: '&',
  },
  controllerAs: 'vm',
};

export default iqTreeViewPolicyThreatLevelSlider;
