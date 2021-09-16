/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ReEvaluateModalService(Modal) {
  return {
    open: function () {
      return Modal.open({
        backdrop: 'static',
        templateUrl: 'repository-reevaluate-modal-template',
        controller: 'repository.reevaluate.modal.controller as vm',
      }).result;
    },
  };
}
ReEvaluateModalService.$inject = ['Modal'];
