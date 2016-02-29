/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ProprietaryConfigurationEditorController() {
    var vm = this, PACKAGE_REGEXP = new RegExp('^[^ /.][^ /]*[^ /.]$');

    vm.add = add;
    vm.validatePackage = validatePackage;
    vm.remove = remove;
    vm.isRegex = false;

    function add($event, entry, group) {
      group.push(entry);
      resetComponent();

      // Use event object to reset calling form to pristine
      angular.element($event.currentTarget).controller('form').$setPristine();
    }

    function validatePackage(value) {
      return {
        invalidPrefix: !value || PACKAGE_REGEXP.test(value),
        wildcards: !value || value.indexOf('*') < 0
      };
    }

    function remove(index, group) {
      group.splice(index, 1);
    }

    function resetComponent() {
      vm.component = {
        prefix: '',
        regex: ''
      };
    }

    resetComponent();
  }

  angular.module('proprietary.configuration.module').controller('proprietary.configuration.editor.controller',
      ProprietaryConfigurationEditorController);
}());
