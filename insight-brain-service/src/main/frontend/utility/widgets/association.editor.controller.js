/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function AssociationEditorController() {
    var vm  = this;
    vm.ceil = Math.ceil;
  }

  angular.module('utility').controller('AssociationEditorController', AssociationEditorController);

}(angular));
