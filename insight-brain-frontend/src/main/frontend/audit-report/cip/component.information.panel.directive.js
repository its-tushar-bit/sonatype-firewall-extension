/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ComponentInformationPanel() {
  return {
    templateUrl: 'cip/component.information.panel.directive.html',
    controllerAs: 'vm',
    controller: ['$scope', 'SelectedComponent', function ($scope, selectedComponent) {
      var vm = this;

      vm.hide = hide;
      vm.showCIP = false;
      vm.selectedTab = undefined;
      vm.tabs = [];
      vm.tabShown = tabShown;

      vm.tabs = [{
        title: 'Component Info',
        directive: 'information-panel'
      }, {
        title: 'Policy',
        directive: 'cip-policy-violations'
      },{
        title: 'Licenses',
        directive: 'cip-license-editor',
        matchedOnly: true
      }, {
        title: 'Vulnerabilities',
        directive: 'cip-vulnerability-editor',
        matchedOnly: true
      }, {
        title: 'Labels',
        directive: 'cip-label-editor',
        matchedOnly: true
      }];

      function hide() {
        selectedComponent.toggle();
      }

      function tabShown(tab) {
        var isUnknown = selectedComponent.get() && selectedComponent.get().matchState === 'unknown';
        if (isUnknown) {
          return !tab.matchedOnly;
        }
        return true;
      }

      $scope.$watch(function () {
        return selectedComponent.get() && {
          hash: selectedComponent.get().hash,
          componentIdentifier: selectedComponent.get().componentIdentifier
        };
      }, function () {
        vm.showCIP = selectedComponent.get();
        if (vm.showCIP) {
          vm.selectedTab = vm.tabs[0];
        }
        else {
          vm.selectedTab = null;
        }
      }, true);
    }]
  };
}
