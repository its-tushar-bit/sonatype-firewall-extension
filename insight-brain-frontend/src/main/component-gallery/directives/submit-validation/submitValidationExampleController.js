/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const INITIAL_SAVED_VALUE = 'foo';

export default function submitValidationExampleController() {
  const vm = this;

  Object.assign(vm, {
    savedUpdateFormVal: INITIAL_SAVED_VALUE,
    updateVal: INITIAL_SAVED_VALUE,

    savedSaveFormVal: undefined,
    saveVal: undefined,

    updateFormIsDirty() {
      return vm.updateVal !== vm.savedUpdateFormVal;
    },

    submitSaveForm() {
      if (vm.saveForm.$valid) {
        vm.savedSaveFormVal = vm.saveVal;
      }
    },

    submitUpdateForm() {
      if (vm.updateForm.$valid && vm.updateFormIsDirty()) {
        vm.savedUpdateFormVal = vm.updateVal;
      }
    }
  });
}
