/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LabelEditorController($stateParams, LabelStore, Messages) {
    var vm = this;

    vm.dirtyLabel = undefined;
    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.siblings = [];
    vm.reset = reset;
    vm.save = save;

    vm.doLoad();

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
      delete vm.error;
    }

    function reset() {
      vm.dirtyLabel.$revert();
    }

    function save() {
      var isNew = vm.dirtyLabel.$new;
      delete vm.error;

      //TODO CLM-5302 Handle Label Save Mask
      vm.dirtyLabel.$save().then(function() {
        if (isNew) {
          //TODO CLM-5302 Handle Label Save Redirects
        }
      }, function(error) {
        vm.error = Messages.getHttpErrorMessage(error);
      });
    }
  }

  LabelEditorController.$inject = ['$stateParams', 'LabelStore', 'Messages'];

  angular.module('owner.manager.module').controller('label.editor.controller', LabelEditorController);

}(angular));
