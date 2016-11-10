/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SaveFilterModal($modal) {
    return {
      open: openModal
    };

    function openModal(filterJson, name, existingFilters) {
      return $modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        windowClass: 'save-filter-modal clm-modal',
        controller: 'save.filter.modal.controller as vm',
        templateUrl: 'dashboard/save.filter.modal.html',
        resolve: {
          filterJson: function() {
            return filterJson;
          },
          filterName: function() {
            return name || '';
          },
          existingFilters: function() {
            return existingFilters;
          }
        }
      }).result;
    }
  }

  SaveFilterModal.$inject = ['$modal'];

  angular //
      .module('dashboard.module') //
      .service('save.filter.modal', SaveFilterModal);

}(angular));
