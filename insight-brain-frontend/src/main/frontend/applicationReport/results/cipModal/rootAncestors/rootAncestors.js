/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { compose, into, indexBy, isNil, map, pipe, prop, pick, reject, take } from 'ramda';

import { lookup, isNilOrEmpty } from '../../../../util/jsUtil';

import template from './rootAncestors.html';

export default {
  template,
  controllerAs: 'vm',
  controller: RootAncestorsController,
};

const SHOWN_ENTRIES_LIMIT = 3;

function RootAncestorsController($scope, $ngRedux, applicationReportActions) {
  const vm = this;

  Object.assign(vm, {
    showAll: false,

    $onInit() {
      const actions = pick(['selectRootAncestor'], applicationReportActions);
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);

      $scope.$watch('vm.selectedComponent', (selectedComponent) => {
        if (selectedComponent) {
          vm.rootAncestors = findRootAncestors(selectedComponent, vm.selectedReport.allEntries);
          vm.isShowMoreLinkDisplayed = vm.rootAncestors.length > SHOWN_ENTRIES_LIMIT;
        }
      });
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    toggleShowAll() {
      vm.showAll = !vm.showAll;
    },

    getDisplayedRootAncestors() {
      return vm.showAll ? vm.rootAncestors : take(SHOWN_ENTRIES_LIMIT, vm.rootAncestors);
    },

    isAnyDisplayedRootAncestorInnerSource() {
      return this.getDisplayedRootAncestors().some((rootAncestor) => rootAncestor.innerSource);
    },

    isRootAncestorsSectionDisplayed() {
      return vm.rootAncestors && vm.rootAncestors.length > 0;
    },
  });
}

RootAncestorsController.$inject = ['$scope', '$ngRedux', 'applicationReportActions'];

export function mapStateToThis({ applicationReport }) {
  const { selectedReport, selectedComponentIndex, selectedRootAncestor } = applicationReport;
  const selectedComponent = selectedRootAncestor || selectedReport.displayedEntries[selectedComponentIndex];

  return {
    selectedReport,
    selectedComponent,
  };
}

// For each key in dependencyInfo.rootAncestors, find last matching component in allEntries.
// Note, allEntries represent non-aggregated list so there could be multiple entries with the same componentId.
export function findRootAncestors(component, allEntries) {
  if (!component.dependencyInfo || component.directDependency || isNilOrEmpty(component.dependencyInfo.rootAncestors)) {
    return [];
  }

  const allEntriesBySerializedComponentId = into(
    {},
    compose(reject(pipe(prop('serializedComponentIdentifier'), isNil)), indexBy(prop('serializedComponentIdentifier'))),
    allEntries
  );

  const getRootAncestorsFromAllEntries = pipe(map(lookup(allEntriesBySerializedComponentId)), reject(isNil));

  return getRootAncestorsFromAllEntries(component.dependencyInfo.rootAncestors);
}
