/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LabelModification(Modal) {
  return {
    add: function (label) {
      return Modal.open({
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'add-modal-service',
        controller: 'LabelAddController',
        resolve: {
          label: function () {
            return label;
          },
        },
      }).result;
    },

    remove: function (label) {
      return Modal.open({
        backdrop: 'static',
        keyboard: false,
        templateUrl: 'delete-modal-service',
        controller: 'LabelRemoveController',
        resolve: {
          label: function () {
            return label;
          },
        },
      }).result;
    },
  };
}
LabelModification.$inject = ['Modal'];
