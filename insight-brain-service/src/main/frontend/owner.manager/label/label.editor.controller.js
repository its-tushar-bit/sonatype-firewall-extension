/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelEditorController($stateParams, $state, $scope, LabelStore, Messages, DeleteModalService, formMaskDelay) {
    var vm = this;

    vm.dirtyLabel = undefined;
    vm.deleteLabel = deleteLabel;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.labelEditor = undefined;
    vm.siblings = [];
    vm.save = save;

    vm.doLoad();

    function deleteLabel() {
      DeleteModalService.deleteResource('Label', vm.dirtyLabel.label, vm.dirtyLabel).then(function() {
        $state.go('^.create-label');
      });
    }

    function doLoad() {
      LabelStore[vm.error ? 'refresh' : 'get']().then(function(labelCandidates) {
        vm.siblings = labelCandidates;
        if (!$stateParams.labelId) {
          vm.dirtyLabel = LabelStore.create();
        } else {
          labelCandidates.forEach(function(labelCandidate) {
            if (labelCandidate.id === $stateParams.labelId) {
              vm.dirtyLabel = labelCandidate.$clone();
              return true;
            }
          });
        }
        if (!vm.dirtyLabel) {
          vm.error = 'Unable to locate label.';
        }
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }

    function save() {
      var isNew = vm.dirtyLabel.$new;
      delete vm.error;

      formMaskDelay.wrap($scope, vm.dirtyLabel.$save()).then(function() {
        if (isNew) {
          vm.dirtyLabel = LabelStore.create();
          vm.labelEditor.$setPristine();
        }
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  }

  LabelEditorController.$inject = ['$stateParams', '$state', '$scope', 'LabelStore', 'Messages', 'DeleteModalService', 'FormMaskDelay'];

  angular.module('owner.manager.module').controller('label.editor.controller', LabelEditorController);

}(angular));
