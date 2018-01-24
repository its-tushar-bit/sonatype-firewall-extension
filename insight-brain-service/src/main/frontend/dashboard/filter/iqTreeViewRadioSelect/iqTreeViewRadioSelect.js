/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqTreeViewRadioSelect.html';

/**
 * @ngDoc directive
 * @name iqTreeViewRadioSelect
 * @restrict E
 *
 * @description
 *
 * Shows a filter dimension in a tree where each available item is shown as a leaf with a radio
 *
 * @param available array of all available entities in this filter type
 * @param selectedEntry object representing selected entry
 * @param idField field used by the entity for the id (defaults to 'id')
 * @param nameField field used by the entity for the name (defaults to 'name')
 * @param name used for the 'name' attribute of radio inputs
 * @param readOnly if true, renders collapsed and disabled tree view (defaults to 'false')
 * @param onChange callback expression - called with the id of the selected option. Context: {selected:Object|String}
 */
export default
function iqTreeViewRadioSelect() {
  return {
    restrict: 'E',
    transclude: true,
    template: template,
    scope: {
      available: '<',
      selectedEntry: '<',
      idField: '@?',
      nameField: '@?',
      name: '@',
      readOnly: '<?',
      onChange: '&'
    },
    controller: IqTreeViewRadioSelectController,
    controllerAs: 'vm',
    link: function ($scope) {
      $scope.idField = $scope.idField || 'id';
      $scope.nameField = $scope.nameField || 'name';
    }
  };
}

function IqTreeViewRadioSelectController($scope) {
  var vm = this;

  vm.select = select;
  vm.isChecked = isChecked;

  function select(item) {
    $scope.onChange({selected: item[$scope.idField]});
  }

  function isChecked(entity) {
    return $scope.selectedEntry[$scope.idField] === entity[$scope.idField];
  }
}

IqTreeViewRadioSelectController.$inject = ['$scope'];
