/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function LabelEditorController($scope, $q, $http, $stateParams, LabelStore, CLMContextLocations, DeleteModalService,
                               SameOwnerStateNavigationService) {
  var vm = this;

  vm.dirtyLabel = undefined;
  vm.deleteLabel = deleteLabel;
  vm.doLoad = doLoad;
  vm.loadError = undefined;
  vm.labelEditor = undefined;
  vm.labelEditorMask = undefined;
  vm.siblings = [];
  vm.save = save;
  vm.submitError = undefined;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function(event) {
    if (vm.dirtyLabel.isDirty()) {
      event.preventDefault();
    }
  });

  function deleteLabel() {
    DeleteModalService.deleteResource('Label', vm.dirtyLabel.label, vm.dirtyLabel).then(function() {
      // Model needs to be clean in order to navigate
      vm.dirtyLabel.$revert();
      SameOwnerStateNavigationService.goEdit('create-label');
    });
  }

  function doLoad() {
    const applicableLabelsUrl = CLMContextLocations.getApplicableLabelsUrl(CLMContextLocations.getEntityId());
    $q.all([LabelStore[vm.loadError ? 'refresh' : 'get'](), $http.get(applicableLabelsUrl)]).then(function(results) {
      results[1].data.labelsByOwner.forEach(function(owner) {
        vm.siblings = vm.siblings.concat(owner.labels);
      });

      if (!$stateParams.labelId) {
        vm.dirtyLabel = LabelStore.create();
      }
      else {
        results[0].forEach(function(labelCandidate) {
          if (labelCandidate.id === $stateParams.labelId) {
            vm.dirtyLabel = labelCandidate.$clone();
            return true;
          }
        });
      }
      if (!vm.dirtyLabel) {
        vm.loadError = 'Unable to locate label.';
      }
    }, function(error) {
      vm.loadError = error;
    });
    delete vm.loadError;
  }

  function save() {
    var isNew = vm.dirtyLabel.$new;
    delete vm.submitError;

    vm.labelEditorMask.wrap(vm.dirtyLabel.$save()).then(function() {
      if (isNew) {
        vm.siblings.push(vm.dirtyLabel);
        vm.dirtyLabel = LabelStore.create();
      }
      vm.labelEditor.$setPristine();
    }, function(error) {
      vm.submitError = error;
    });
  }
}

LabelEditorController.$inject = [
  '$scope', '$q', '$http', '$stateParams', 'LabelStore', 'CLMContextLocations', 'DeleteModalService',
  'SameOwnerStateNavigationService'
];
