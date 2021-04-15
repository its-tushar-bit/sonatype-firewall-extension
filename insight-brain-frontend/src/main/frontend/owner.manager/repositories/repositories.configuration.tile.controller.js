/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ConfigurationTileController($http, CLMLocations, DeleteModalService) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.error = undefined;
  vm.removeRepository = removeRepository;
  vm.repositories = undefined;
  vm.sortFields = ['repository.publicId'];

  vm.doLoad();

  function doLoad() {
    delete vm.error;

    $http.get(CLMLocations.getRepositoriesUrl()).then(
      function (results) {
        vm.repositories = results.data.repositories || [];
      },
      function () {
        vm.error = arguments;
      }
    );
  }

  function removeRepository(repository) {
    DeleteModalService.deleteCustom(
      'Remove Repository',
      'Are you sure you want to remove the Repository with ID "' +
        repository.repository.publicId +
        '"? This action is not reversible.',
      'Remove',
      deleteRepository
    ).then(function () {
      // Remove the repository from the view on delete.
      // Cannot use a Store here because of the nested id field
      vm.repositories.some(function (item, itemIndex) {
        if (item.repository.id === repository.repository.id) {
          vm.repositories.splice(itemIndex, 1);
          return true;
        }
      });
    });

    function deleteRepository() {
      return $http.delete(CLMLocations.getRepositoryInfoUrl(repository.repository.id));
    }
  }
}

ConfigurationTileController.$inject = ['$http', 'CLMLocations', 'DeleteModalService'];
