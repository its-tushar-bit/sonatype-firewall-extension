/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './owner.editor.service.html';

export default
function OwnerEditorService(Modal) {
  return {
    open: function(owner, ownerType, siblings) {
      Modal.open({
        animation: false,
        backdrop: 'static',
        keyboard: false,
        controller: 'owner.editor.controller as vm',
        template,
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

OwnerEditorService.$inject = ['Modal'];
