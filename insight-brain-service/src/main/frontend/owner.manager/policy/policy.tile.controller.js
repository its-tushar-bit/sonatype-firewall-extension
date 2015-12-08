/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyTileController($q, $http, CLMAppLocations, StageTypeStore, SameOwnerStateNavigationService) {
    var vm = this;
    vm.ownerName = undefined;
    vm.policiesByOwner = undefined;
    vm.error = undefined;
    vm.actionStages = [];
    vm.editPolicy = editPolicy;

    vm.doLoad = doLoad;

    vm.doLoad();

    function doLoad() {
      $q.all([
        $http.get(CLMAppLocations.getApplicablePolicies()),
        StageTypeStore.getActionStages()
      ]).then(function(results) {
        vm.policiesByOwner = results[0].data.policiesByOwner;
        vm.policiesByOwner.forEach(function(policyOwner, index) {
          policyOwner.inherited = index > 0;
        });

        vm.ownerName = vm.policiesByOwner[0].ownerName;
        
        results[1].forEach(function(actionStage) {
          vm.actionStages.push(actionStage.stageTypeId);
        });
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function editPolicy(policyId, inherited) {
      if (!inherited) {
        SameOwnerStateNavigationService.goEdit('policy', {policyId: policyId});
      }
    }
  }

  PolicyTileController.$inject = ['$q', '$http', 'CLMAppLocations', 'StageTypeStore', 
                                  'SameOwnerStateNavigationService'];

  angular //
      .module('owner.manager.module') //
      .controller('policy.tile.controller', PolicyTileController);
}(angular));
