/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ErrorModalController(headerText, bodyText) {
    var vm = this;
    vm.headerText = headerText;
    vm.bodyText = bodyText;
  }

  ErrorModalController.$inject = ['headerText', 'bodyText'];

  angular //
      .module('utility') //
      .controller('error.modal.controller', ErrorModalController);

}(angular));
