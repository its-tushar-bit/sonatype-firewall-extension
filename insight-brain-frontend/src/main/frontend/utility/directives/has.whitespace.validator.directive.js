/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function hasWhitespaceValidator() {
  return {
    require: 'ngModel',
    restrict: 'A',
    link: hasWhitespaceValidatorLink,
  };

  function hasWhitespaceValidatorLink(scope, elem, attr, ctrl) {
    ctrl.$validators.spaces = function (value) {
      return !value || !value.match(/ {2,}|\t/);
    };
    // Allows validation to be invoked by code or user input
    scope.$watch(attr.ngModel, function (newValue) {
      if (typeof newValue !== 'undefined' && newValue !== null) {
        ctrl.$$parseAndValidate();
      }
    });
  }
}
