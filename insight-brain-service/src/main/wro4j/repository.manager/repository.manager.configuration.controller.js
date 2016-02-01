/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function(angular) {
  'use strict';

  function RepositoryManagerConfigurationController($http, clmLocations, Dialog, ErrorDialog) {

    var vm = this;

    vm.doLoad = doLoad;
    vm.error = undefined;
    vm.repositories = null;
    vm.viewRemoveRepository = viewRemoveRepository;
    vm.repositorySubmittingMap = {};

    vm.doLoad();

    function viewRemoveRepository(repository) {
      Dialog.open({
        title: 'Delete Repository',
        body: 'Are you sure you want to delete the Repository with ID "' + repository.repository.publicId + '"? This action is not reversible.',
        buttons: [
          {
            name: 'Cancel',
            type: 'cancel'
          }, {
            name: 'Delete',
            type: 'danger',
            click: function() {
              vm.repositorySubmittingMap[repository.repository.id] = true;
              $http.delete(clmLocations.getRepositoryInfoUrl(repository.repository.id))
                  .success(function() {
                    // Remove the repository from the view on delete.
                    // Cannot use a Store here because of the nested id field
                    vm.repositories.forEach(function(item, itemIndex) {
                      if (item.repository.id === repository.repository.id) {
                        vm.repositories.splice(itemIndex, 1);
                        return true;
                      }
                    });
                  })
                  .error(function() {
                    vm.repositorySubmittingMap[repository.repository.id] = false;
                    ErrorDialog.open(arguments[0]);
                  });
            }
          }
        ]
      });
    }

    function doLoad() {
      delete vm.error;
      $http.get(clmLocations.getRepositoriesUrl())
          .success(function(results) {
            vm.repositories = results.repositories || [];
          })
          .error(function() {
            vm.error = arguments;
          });
    }

  }

  RepositoryManagerConfigurationController.$inject = ['$http', 'CLMLocations', 'Dialog', 'ErrorDialog'];

  angular.module('repository.manager.module').controller('repository.manager.configuration.controller', RepositoryManagerConfigurationController);
}(angular));
