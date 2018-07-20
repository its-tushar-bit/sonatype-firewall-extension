/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './saveFilterModal.html';
export default
function SaveFilterModal(Modal) {
  return {
    open: openModal
  };

  function openModal() {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'saveFilterModalController as vm',
      template: template
    }).result;
  }
}

SaveFilterModal.$inject = ['Modal'];
