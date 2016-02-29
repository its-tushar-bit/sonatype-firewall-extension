/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AssociationEditor() {
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
      templateUrl: 'utility/widgets/association.editor.directive.html',
      controller: AssociationEditorController,
      controllerAs: 'vm',
      bindToController: true
    };
  }

  function AssociationEditorController() {
    var vm = this;
    vm.ceil = Math.ceil;
  }

  angular.module('utility').directive('associationEditor', AssociationEditor);

}(angular));
