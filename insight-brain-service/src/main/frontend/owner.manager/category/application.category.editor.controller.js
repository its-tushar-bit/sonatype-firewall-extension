/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ApplicationCategoryEditorController($q, $http, $scope, ApplicationStore, CLMAppLocations, CLMLocations, formMaskDelay)
  {
    var vm = this;

    vm.doLoad = doLoad;
    vm.save = save;
    vm.loadError = undefined;
    vm.submitError = undefined;
    vm.submitErrorMessage = undefined;
    vm.categories = undefined;
    vm.isApp = CLMAppLocations.isApplication();
    vm.ownerName = undefined;
    vm.categoryEditor = undefined;

    vm.doLoad();

    function doLoad() {
      if (vm.isApp) {

        $q.all([
          ApplicationStore[vm.loadError ? 'refresh' : 'get'](),
          $http.get(CLMLocations.getApplicableOrganizationTags(CLMAppLocations.getEntityId())),
          $http.get(CLMLocations.getApplicationTagUrl(CLMAppLocations.getEntityId()))
        ]).then(function(results) {
          var organizationCategories = results[1].data,
              applicationCategories = results[2].data;
          vm.categories = [];

          results[0].some(function(candidate) {
            if (candidate.publicId === CLMAppLocations.getEntityId()) {
              vm.ownerName = candidate.name;
              return true;
            }
          });

          organizationCategories.forEach(function(organizationCategory) {
            organizationCategory.isApplied = false;
            if (applicationCategories.some(function(appliedCategory) {
                  return appliedCategory.id === organizationCategory.id;
                })) {
              organizationCategory.isApplied = true;
            }
            vm.categories.push(organizationCategory);
          });

          if (!vm.ownerName) {
            vm.loadError = 'Could not find an application with ID ' + CLMAppLocations.getEntityId() + '.';
          }
        }, function() {
          vm.loadError = arguments;
        });
      }

      delete vm.loadError;
    }

    function save() {
      delete vm.submitError;

      var appliedCategories = vm.categories.filter(function(category) {
        return category.isApplied;
      });

      formMaskDelay.wrap($scope, $http.put(CLMLocations.getApplicationTagUrl(CLMAppLocations.getEntityId()),
          appliedCategories)).then(function() {
        vm.categoryEditor.$setPristine();
      }, function(error) {
        vm.submitError = error;
      });
    }
  }

  ApplicationCategoryEditorController.$inject = [
    '$q', '$http', '$scope', 'ApplicationStore', 'CLMAppLocations', 'CLMLocations', 'FormMaskDelay'
  ];

  angular//
      .module('owner.manager.module')//
      .controller('application.category.editor.controller', ApplicationCategoryEditorController);
}(angular));
