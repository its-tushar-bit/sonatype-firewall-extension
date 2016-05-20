/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerSummaryController($state, $scope, $rootScope, $q, $http, $window, OwnerEditor, ApplicationStore,
                                  OrganizationStore, CLMLocations, CLMAppLocations, StageTypeStore, DeleteModalService,
                                  SelectApplicationContactService, EvaluateApplicationModalService,
                                  ImportPolicyModalService, ownerConstant, MoveApplicationModal, EventNameConstant,
                                  ChangeApplicationIdService)
  {
    var vm = this;

    vm.error = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.isRootOrg = CLMAppLocations.isRootOrg();
    vm.owner = undefined;
    vm.stages = undefined;
    vm.doLoad = doLoad;
    vm.edit = edit;
    vm.moveApplication = moveApplication;
    vm.evaluateApp = evaluateApp;
    vm.importPolicy = importPolicy;
    vm.deleteOwner = deleteOwner;
    vm.getShortTypeName = getShortTypeName;
    vm.getResourceTypeName = getResourceTypeName;
    vm.openReport = openReport;
    vm.goToParentView = goToParentView;
    vm.selectContact = selectContact;
    vm.changeApplicationId = changeApplicationId;


    var siblings,
        stateIdField = vm.isApp ? 'applicationPublicId' : 'organizationId',
        idField = vm.isApp ? 'publicId' : 'id',
        type = vm.isApp ? ownerConstant.APPLICATION_TYPE : ownerConstant.ORGANIZATION_TYPE,
        id = $state.params[stateIdField];

    vm.doLoad();

    if (vm.isApp) {
      $scope.$on('reload.app.report.data', function() {
        $http.get(CLMLocations.getApplicationSummaryUrl(id)).then(function(result) {
          vm.applicationSummary = result.data;
        }, function(error) {
          vm.error = error;
        });
      });

      $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, doLoad);
    }

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
          vm.error = 'Could not find an ' + type + ' with ID ' + id + '.';
        }
      }, function(error) {
        vm.error = error;
      });

      delete vm.error;
    }

    function edit() {
      OwnerEditor.open(vm.owner, type, siblings);
    }

    function moveApplication() {
      MoveApplicationModal.open(vm.owner);
    }

    function evaluateApp() {
      EvaluateApplicationModalService.open(vm.owner);
    }

    function importPolicy() {
      ImportPolicyModalService.open();
    }

    function selectContact(owner) {
      SelectApplicationContactService.open(owner);
    }

    function changeApplicationId() {
      ChangeApplicationIdService.open(vm.owner, siblings);
    }

    function deleteOwner() {
      DeleteModalService.deleteResource(vm.getResourceTypeName(), vm.owner.name, vm.owner).then(function() {
        $rootScope.$broadcast('owner.deleted', vm.owner, type);
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
    '$state', '$scope', '$rootScope', '$q', '$http', '$window', 'OwnerEditorService', 'ApplicationStore',
    'OrganizationStore', 'CLMLocations', 'CLMAppLocations', 'StageTypeStore', 'DeleteModalService',
    'SelectApplicationContactService', 'evaluate.application.modal.service', 'import.policy.modal.service',
    'owner.constant', 'move.application.modal.service', 'event.name.constant', 'change.application.id.service'
  ];

  angular//
      .module('owner.manager.module')//
      .controller('OwnerSummaryController', OwnerSummaryController);

}(angular));
