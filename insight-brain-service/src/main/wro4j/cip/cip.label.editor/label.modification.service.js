/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  function LabelModification($modal) {
    return {
      add: function (label) {
        return $modal.open({
          templateUrl: 'add-modal-service',
          controller: 'LabelAddController',
          windowClass: 'clm-modal',
          resolve: {
            label: function () {
              return label;
            }
          }
        }).result;
      },

      remove: function (label) {
        return $modal.open({
          templateUrl: 'delete-modal-service',
          controller: 'LabelRemoveController',
          windowClass: 'clm-modal',
          resolve: {
            label: function () {
              return label;
            }
          }
        }).result;
      }
    };
  }
  LabelModification.$inject = ['$modal'];

  angular.module('cip.label.editor').service('LabelModification', LabelModification);
}());
