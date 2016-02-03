/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
(function() {
  'use strict';

  function ReEvaluateModalService($modal) {
    return {
      open: function () {
        return $modal.open({
          backdrop: 'static',
          templateUrl: 'repository-reevaluate-modal-template',
          controller: 'repository.reevaluate.modal.controller as vm'
        }).result;
      }
    };
  }
  ReEvaluateModalService.$inject = ['$modal'];

  angular.module('Report').service('ReEvaluateModal', ReEvaluateModalService);
}());
