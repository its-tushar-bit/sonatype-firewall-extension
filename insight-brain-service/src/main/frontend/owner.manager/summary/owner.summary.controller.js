/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerSummaryController($state, $q, $http, $window, OwnerEditor, ApplicationStore, OrganizationStore,
                                  CLMLocations,
                                  StageTypeStore)
  {
    var vm = this;

    vm.error = undefined;
    vm.owner = undefined;
    vm.stages = undefined;
    vm.doLoad = doLoad;
    vm.edit = edit;
    vm.getShortTypeName = getShortTypeName;
    vm.openReport = openReport;

    var siblings,
        isApp = $state.current.name.indexOf('application') !== -1,
        stateIdField = isApp ? 'applicationPublicId' : 'organizationId',
        idField = isApp ? 'publicId' : 'id',
        type = isApp ? 'application' : 'organization',
        id = $state.params[stateIdField];

    vm.doLoad();

    function doLoad() {
      var promises = [
        ( isApp ? ApplicationStore : OrganizationStore)[vm.error ? 'refresh' : 'get']()
      ];

      if (isApp) {
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

        if (isApp) {
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

    function getShortTypeName() {
      return type === 'application' ? 'App' : 'Org';
    }

    function openReport(stage) {
      if (vm.applicationSummary.policyEvaluations[stage.id]) {
        $window.open($state.href('report', {
          publicId: vm.applicationSummary.publicId,
          scanId: vm.applicationSummary.policyEvaluations[stage.id].scanId
        }), '_blank');
      }
    }
  }

  OwnerSummaryController.$inject = [
    '$state', '$q', '$http', '$window', 'OwnerEditorService', 'ApplicationStore', 'OrganizationStore', 'CLMLocations',
    'StageTypeStore'
  ];

  angular//
      .module('owner.manager.module')//
      .controller('OwnerSummaryController', OwnerSummaryController);

}(angular));
