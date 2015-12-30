/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ImportPolicyModalService($modal) {
    return {open: openModal};

    function openModal() {
      return $modal.open({
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'owner.manager/utility/services/import.policy.modal.html',
        controller: 'import.policy.modal.controller as vm'
      }).result;
    }
  }

  ImportPolicyModalService.$inject = ['$modal'];

  angular //
      .module('owner.manager.module') //
      .service('import.policy.modal.service', ImportPolicyModalService);

}(angular));
