/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint no-console: "off" */

export default function iqTreeViewPolicyThreatLevelSliderController() {
  const vm = this;

  vm.selectedRange = [3, 8];

  vm.onChange = function(selectedRange) {
    console.log(selectedRange);
    vm.selectedRange = selectedRange;
  };
}
