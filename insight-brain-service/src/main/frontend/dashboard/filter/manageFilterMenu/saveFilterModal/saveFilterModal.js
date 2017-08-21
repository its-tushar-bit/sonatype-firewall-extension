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

  function openModal(filterJson, name, existingFilters) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      controller: 'saveFilterModalController as vm',
      template: template,
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

SaveFilterModal.$inject = ['Modal'];
