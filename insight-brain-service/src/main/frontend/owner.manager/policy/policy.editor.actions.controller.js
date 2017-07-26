/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function PolicyEditorActionsController(StageTypeStore)
{
  var vm = this;

  vm.doLoad = doLoad;
  vm.actionStages = undefined;
  vm.loadError = undefined;

  vm.doLoad();

  function doLoad() {
    StageTypeStore.getActionStages().then(function(results) {
      vm.actionStages = results;
    }, function(error) {
      vm.loadError = error;
    });

    delete vm.loadError;
  }
}

PolicyEditorActionsController.$inject = ['StageTypeStore'];

angular //
    .module('owner.manager.module') //
    .controller('policy.editor.actions.controller', PolicyEditorActionsController);
