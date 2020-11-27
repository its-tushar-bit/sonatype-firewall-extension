/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './update.source.control.modal.service.html';

export default function UpdateSourceControlModalService(Modal) {
  var service = {
    updateSourceControl: UpdateSourceControl
  };

  function UpdateSourceControl(continueAction, dismissOnError) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'UpdateSourceControlModalController as vm',
      template,
      resolve: {
        resource: angular.noop,
        resourceType: angular.noop,
        resourceName: angular.noop,
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

UpdateSourceControlModalService.$inject = ['Modal'];
