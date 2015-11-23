/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DeleteModalService($modal) {
    var service = {
      deleteResource: DeleteResource
    };

    function DeleteResource(resourceType, resourceName, resource, saveOnDelete) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'clm-modal delete-modal',
        controller: 'DeleteModalController as vm',
        templateUrl: 'utility/services/delete.modal.service.html',
        resolve: {
          resource: function() {
            return resource;
          },
          resourceType: function() {
            return resourceType;
          },
          resourceName: function() {
            return resourceName;
          },
          saveOnDelete: function() {
            return saveOnDelete;
          }
        }
      }).result;
    }

    return service;
  }

  DeleteModalService.$inject = ['$modal'];

  angular //
      .module('utility') //
      .service('DeleteModalService', DeleteModalService);

}(angular));
