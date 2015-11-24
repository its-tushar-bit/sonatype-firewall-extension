/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerSummaryController($state, $q, $http, $window, OwnerEditor, ApplicationStore, OrganizationStore,
                                  CLMLocations, CLMAppLocations, StageTypeStore, DeleteModalService, SelectApplicationContactService)
  {
    var vm = this;

    vm.error = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.owner = undefined;
    vm.stages = undefined;
    vm.doLoad = doLoad;
    vm.edit = edit;
    vm.deleteOwner = deleteOwner;
    vm.getShortTypeName = getShortTypeName;
    vm.getResourceTypeName = getResourceTypeName;
    vm.openReport = openReport;
    vm.goToParentView = goToParentView;
    vm.selectContact = selectContact;

    var siblings,
        stateIdField = vm.isApp ? 'applicationPublicId' : 'organizationId',
        idField = vm.isApp ? 'publicId' : 'id',
        type = vm.isApp ? 'application' : 'organization',
        id = $state.params[stateIdField];

    vm.doLoad();

    function doLoad() {
      var promises = [
        ( vm.isApp ? ApplicationStore : OrganizationStore)[vm.error ? 'refresh' : 'get']()
      ];

      if (vm.isApp) {
        promises.push(StageTypeStore.getDashboardStages());
        promises.push($http.get(CLMLocations.getApplicationSummaryUrl(id)));
      }

      $q.all(promises).then(function(results) {
        siblings = results[0];
        angular.forEach(siblings, function(candidate) {
          if (candidate[idField] === $state.params[stateIdField]) {
            vm.owner = candidate;
          }
        });

        if (vm.isApp) {
          vm.stages = results[1];
          vm.applicationSummary = results[2].data;
        }

        if (!vm.owner) {
          vm.error = 'Could not find an ' + type + ' with ID ' +  id + '.';
        }
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function edit() {
      OwnerEditor.open(vm.owner, type, siblings);
    }

    function selectContact(owner) {
      SelectApplicationContactService.open(owner);
    }

    function deleteOwner() {
      DeleteModalService.deleteResource(vm.getResourceTypeName(), vm.owner.name, vm.owner).then(function() {
        vm.goToParentView();
      });
    }

    function getShortTypeName() {
      return vm.isApp ? 'App' : 'Org';
    }
    
    function getResourceTypeName() {
      return vm.isApp ? 'Application' : 'Organization';
    }

    function openReport(stage) {
      if (vm.applicationSummary.policyEvaluations[stage.stageTypeId]) {
        $window.open($state.href('report', {
          publicId: vm.applicationSummary.publicId,
          scanId: vm.applicationSummary.policyEvaluations[stage.stageTypeId].scanId
        }), '_blank');
      }
    }

    function goToParentView() {
      if (!vm.isApp) {
        $state.go('management.view.organization', {organizationId: vm.owner.parentOrganizationId});
      } 
      else {
        $state.go('management.view.organization', {organizationId: vm.owner.organizationId});
      }     
    }
  }

  OwnerSummaryController.$inject = [
    '$state', '$q', '$http', '$window', 'OwnerEditorService', 'ApplicationStore', 'OrganizationStore', 'CLMLocations',
    'CLMAppLocations', 'StageTypeStore', 'DeleteModalService', 'SelectApplicationContactService'
  ];

  angular//
      .module('owner.manager.module')//
      .controller('OwnerSummaryController', OwnerSummaryController);

}(angular));
