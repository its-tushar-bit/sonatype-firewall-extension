/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './componentInformationPanelDirective.html';

/**
 * component-information-panel directive
 *
 * Parameters:
 *   tabs <Object>: tabs config
 *   hide-close-button <no value>: if present, the close button (x) will be hidden
 */
export default function ComponentInformationPanel() {
  return {
    template,
    scope: {
      tabs: '<',
    },
    controllerAs: 'vm',
    bindToController: true,
    require: 'componentInformationPanel',
    link(scope, element, attrs, controller) {
      controller.showCloseButton = attrs.hideCloseButton == null;
    },
    controller: [
      '$scope',
      'SelectedComponent',
      function ($scope, selectedComponent) {
        var vm = this;

        vm.hide = hide;
        vm.showCIP = false;
        vm.selectedTab = undefined;
        vm.tabShown = tabShown;

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

        $scope.$watch(
          function () {
            return (
              selectedComponent.get() && {
                hash: selectedComponent.get().hash,
                componentIdentifier: selectedComponent.get().componentIdentifier,
              }
            );
          },
          function () {
            vm.showCIP = selectedComponent.get();
            if (vm.showCIP) {
              vm.selectedTab = vm.tabs[0];
            } else {
              vm.selectedTab = null;
            }
          },
          true
        );
      },
    ],
  };
}
