/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
      controller: 'delete.filters.modal.controller as vm',
      templateUrl: 'dashboard/manage.filter.menu/delete.filters.modal.html',
      resolve: {
        savedNamedFilters: function() {
          return savedNamedFilters;
        }
      }
    }).result;
  }
}

DeleteFiltersModal.$inject = ['$modal'];
