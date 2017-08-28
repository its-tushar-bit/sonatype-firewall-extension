/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function DeleteModalService(Modal) {
  var service = {
    deleteResource: DeleteResource,
    deleteCustom: DeleteCustom
  };

  function DeleteResource(resourceType, resourceName, resource) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
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
        headerText: angular.noop,
        bodyText: angular.noop,
        maskText: angular.noop,
        continueAction: angular.noop,
        dismissOnError: angular.noop
      }
    }).result;
  }

  function DeleteCustom(headerText, bodyText, maskText, continueAction, dismissOnError) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'DeleteModalController as vm',
      templateUrl: 'utility/services/delete.modal.service.html',
      resolve: {
        resource: angular.noop,
        resourceType: angular.noop,
        resourceName: angular.noop,
        headerText: function() {
          return headerText;
        },
        bodyText: function() {
          return bodyText;
        },
        maskText: function() {
          return maskText;
        },
        continueAction: function() {
          return continueAction;
        },
        dismissOnError: function() {
          return dismissOnError;
        }
      }
    }).result;
  }

  return service;
}

DeleteModalService.$inject = ['Modal'];
