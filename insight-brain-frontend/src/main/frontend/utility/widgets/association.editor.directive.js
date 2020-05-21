/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './association.editor.directive.html';

export default function AssociationEditor() {
  return {
    restrict: 'E',
    scope: {
      items: '=',
      checkboxParam: '@',
      icon: '@',
      description: '@',
      isRadioButton: '@',
      selected: '=',
      disabled: '=?ngDisabled'
    },
    replace: true,
    template,
    controller: AssociationEditorController,
    controllerAs: 'vm',
    bindToController: true
  };
}

function AssociationEditorController() {
  var vm = this;
  vm.ceil = Math.ceil;
  vm.toggleSelected = toggleSelected;

  function toggleSelected(item, checkboxParam) {
    item[checkboxParam] = !item[checkboxParam];
  }
}
