/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function hasWhitespaceValidator() {
  return {
    require: 'ngModel',
    restrict: 'A',
    link: hasWhitespaceValidatorLink
  };

  function hasWhitespaceValidatorLink(scope, elem, attr, ctrl) {
    if (attr.ngTrim !== 'false') {
      throw new Error('has-whitespace-validator directive requires that the ngTrim attribute be set to false');
    }

    ctrl.$validators.spaces = function(value) {
      return !value || !value.match(/^ | {2,}|\t| $/);
    };
    // Allows validation to be invoked by code or user input
    scope.$watch(attr.ngModel, function(newValue) {
      if (typeof newValue !== 'undefined' && newValue !== null) {
        ctrl.$$parseAndValidate();
      }
    });
  }
}

angular.module('utility.directives').directive('hasWhitespaceValidator', hasWhitespaceValidator);
