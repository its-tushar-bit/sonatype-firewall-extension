/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ComponentInformationPanel() {
    return {
      templateUrl: 'cip/component.information.panel.directive.html',
      controllerAs: 'vm',
      controller: ['$scope', 'SelectedComponent', function ($scope, selectedComponent) {
        var vm = this;

        vm.hide = hide;
        vm.showCIP = false;
        vm.selectedTab = undefined;
        vm.tabs = [];

        vm.tabs = [{
          title: 'Labels',
          directive: 'cip-label-editor'
        }];

        function hide() {
          selectedComponent.toggle();
        }

        $scope.$watch(function () {
          return selectedComponent.get();
        }, function () {
          vm.showCIP = selectedComponent.get();
          if (vm.showCIP) {
            vm.selectedTab = vm.tabs[0];
          }
        });
      }]
    };
  }

  angular.module('component.information.panel').directive('componentInformationPanel', ComponentInformationPanel);
}());
