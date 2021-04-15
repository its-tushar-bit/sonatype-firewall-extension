/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';

import template from './delete.modal.service.html';

export default function DeleteModalService(Modal) {
  var service = {
    deleteResource: DeleteResource,
    deleteCustom: DeleteCustom,
    deleteRedux: DeleteRedux,
  };

  function DeleteResource(resourceType, resourceName, resource) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'DeleteModalController as vm',
      template,
      resolve: {
        resource: function () {
          return resource;
        },
        resourceType: function () {
          return resourceType;
        },
        resourceName: function () {
          return resourceName;
        },
        headerText: angular.noop,
        bodyText: angular.noop,
        maskText: angular.noop,
        continueAction: angular.noop,
        dismissOnError: angular.noop,
      },
    }).result;
  }

  function DeleteCustom(headerText, bodyText, maskText, continueAction, dismissOnError) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'DeleteModalController as vm',
      template,
      resolve: {
        resource: angular.noop,
        resourceType: angular.noop,
        resourceName: angular.noop,
        headerText: function () {
          return headerText;
        },
        bodyText: function () {
          return bodyText;
        },
        maskText: function () {
          return maskText;
        },
        continueAction: function () {
          return continueAction;
        },
        dismissOnError: function () {
          return dismissOnError;
        },
      },
    }).result;
  }

  /**
   * A Delete Modal factory that is usable in a redux workflow
   * @param continueAction A function to invoke when the user presses the continue button.  This function should
   *   ultimately cause the state mapped by stateMapper to update appropriately
   * @param stateMapper a function which maps from the redux state to an object with the following properties:
   * `errorState` null or an error.  The error will be run through Message.getHttpErrorMessage in order to determine
   * what to display in the UI
   * `deleting` boolean for whether the delete is currently in progress
   * `success` boolean for whether the delete was successful
   */
  function DeleteRedux(headerText, bodyText, maskText, continueAction, stateMapper) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'DeleteModalReduxController as vm',
      template,
      resolve: {
        resourceType: angular.noop,
        resourceName: angular.noop,
        headerText: always(headerText),
        bodyText: always(bodyText),
        maskText: always(maskText),
        continueAction: always(continueAction),
        stateMapper: always(stateMapper),
      },
    }).result;
  }

  return service;
}

DeleteModalService.$inject = ['Modal'];
