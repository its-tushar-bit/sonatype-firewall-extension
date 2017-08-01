/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './deleteFiltersModal.html';

export default
function DeleteFiltersModal($modal) {
  return {
    open: openModal
  };

  function openModal(savedNamedFilters) {
    return $modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      windowClass: 'delete-filters-modal iq-modal clm-modal',
      controller: 'deleteFiltersModalController as vm',
      template: template,
      resolve: {
        savedNamedFilters: function() {
          return savedNamedFilters;
        }
      }
    }).result;
  }
}

DeleteFiltersModal.$inject = ['$modal'];
