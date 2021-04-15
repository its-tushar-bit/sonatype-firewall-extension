/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './externalLinkModalService.html';

export default function externalLinkModalService(Modal) {
  return { open: openModal };

  function openModal(href) {
    return Modal.open({
      animation: false,
      backdrop: 'static',
      keyboard: false,
      template,
      controller: [
        '$scope',
        function (scope) {
          scope.href = href;
        },
      ],
    }).result;
  }
}

externalLinkModalService.$inject = ['Modal'];
