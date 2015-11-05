/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelEditorController($q, $http, $stateParams, $scope, LabelStore, CLMAppLocations, DeleteModalService, formMaskDelay, SameOwnerStateNavigationService) {
    var vm = this;

    vm.dirtyLabel = undefined;
    vm.deleteLabel = deleteLabel;
    vm.doLoad = doLoad;
    vm.loadError = undefined;
    vm.labelEditor = undefined;
    vm.siblings = [];
    vm.save = save;
    vm.submitError = undefined;

    vm.doLoad();

    function deleteLabel() {
      DeleteModalService.deleteResource('Label', vm.dirtyLabel.label, vm.dirtyLabel).then(function() {
        SameOwnerStateNavigationService.goEdit('create-label');
      });
    }

    function doLoad() {
      $q.all([LabelStore[vm.loadError ? 'refresh' : 'get'](), $http.get(CLMAppLocations.getApplicableLabelsUrl(CLMAppLocations.getEntityId()))]).then(function(results) {
        results[1].data.labelsByOwner.forEach(function(owner) {
          vm.siblings = vm.siblings.concat(owner.labels);
        });

        if (!$stateParams.labelId) {
          vm.dirtyLabel = LabelStore.create();
        } else {
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

      formMaskDelay.wrap($scope, vm.dirtyLabel.$save()).then(function() {
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

  LabelEditorController.$inject = ['$q', '$http', '$stateParams', '$scope', 'LabelStore', 'CLMAppLocations', 'DeleteModalService', 'FormMaskDelay', 'SameOwnerStateNavigationService'];

  angular.module('owner.manager.module').controller('label.editor.controller', LabelEditorController);

}(angular));
