/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './cipModal.html';

export default {
  template: template,
  controller: CipModalController,
  controllerAs: 'vm',
  bindings: {
    dismiss: '&',
    stageId: '<',
    selectedComponent: '<',
    selectedComponentIndex: '<',
    selectComponent: '<',
    entriesCount: '<',
    unselectRootAncestor: '&',
    selectedRootAncestor: '<',
    previousComponent: '<',
    selectedReport: '<',
    metadata: '<',
  },
};

function CipModalController($ngRedux, $scope, SelectedComponent, Coordinates, ComponentUtil, Properties) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      Properties.setStageId(vm.stageId);

      $scope.$watch('vm.selectedComponent', function (selectedComponent) {
        if (selectedComponent) {
          setupVersionGraphGlobalState(selectedComponent);
          SelectedComponent.toggle(selectedComponent);
        }
      });

      $scope.$on('modal.closing', function () {
        // un-select component so that watchers are triggered when the same component is selected again
        SelectedComponent.toggle();
      });
    },

    previous() {
      vm.selectComponent(vm.selectedComponentIndex - 1);
    },

    next() {
      vm.selectComponent(vm.selectedComponentIndex + 1);
    },

    isPreviousDisabled() {
      return vm.selectedComponentIndex <= 0;
    },

    isNextDisabled() {
      return vm.selectedComponentIndex >= getLastIndex();
    },
  });

  function getLastIndex() {
    return vm.entriesCount - 1;
  }

  // set up global state required by version graph (see ci-version-graph.js)
  function setupVersionGraphGlobalState(selectedComponent) {
    ComponentUtil.enhanceWithComponentIdentifier(selectedComponent);

    Properties.setHash(selectedComponent.hash);
    Properties.setFilename(selectedComponent.matchState === 'unknown' ? selectedComponent.coordinates : null);
    Properties.setProprietary(selectedComponent.proprietary || false);
    Properties.setMatchState(selectedComponent.matchState);
    Coordinates.setIdentificationSource(selectedComponent.identificationSource);
    Properties.setDependencyType(
      selectedComponent.dependencyInfo &&
        (selectedComponent.dependencyInfo.isDirectDependency ? 'direct' : 'transitive')
    );
    Properties.setInnerSource(selectedComponent.innerSource);
    if (selectedComponent.componentIdentifier) {
      const { coordinates, format } = selectedComponent.componentIdentifier;
      Coordinates.set(format, coordinates);
    } else {
      Coordinates.set(null, {}); // unknown
    }
  }
}

CipModalController.$inject = ['$ngRedux', '$scope', 'SelectedComponent', 'Coordinates', 'ComponentUtil', 'Properties'];
