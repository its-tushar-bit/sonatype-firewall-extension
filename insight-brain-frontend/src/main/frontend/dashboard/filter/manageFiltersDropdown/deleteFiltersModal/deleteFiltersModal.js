/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './deleteFiltersModal.html';

export default
function DeleteFiltersModal(Modal) {
  return {
    open: openModal
  };

  function openModal() {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'deleteFiltersModalController as vm',
      template: template
    }).result;
  }
}

DeleteFiltersModal.$inject = ['Modal'];
