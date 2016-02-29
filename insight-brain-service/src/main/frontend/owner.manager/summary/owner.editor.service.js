/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerEditorService($modal) {
    return {
      open: function(owner, ownerType, siblings) {
        $modal.open({
          animation: false,
          backdrop: 'static',
          keyboard: false,
          windowClass: 'owner-editor-modal clm-modal',
          controller: 'owner.editor.controller as vm',
          templateUrl: 'owner.manager/summary/owner.editor.service.html',
          resolve: {
            owner: function() {
              return owner;
            },
            ownerType: function() {
              return ownerType;
            },
            siblings: function() {
              return siblings;
            }
          }
        });
      }
    };
  }

  OwnerEditorService.$inject = ['$modal'];

  angular //
      .module('owner.manager.module') //
      .service('OwnerEditorService', OwnerEditorService);

}(angular));
