/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function InitialValueDropdownSelector() {
  return {
    restrict: 'E',
    require: 'ngModel',
    link: link
  };

  function link(scope, element, attrs, ngModelController) {
    if (element.parents('.clm-form').length > 0) {
      var initialValue;
      element.addClass('initial-value');

      var initialValueWatch = scope.$watch(function() {
        return ngModelController.$viewValue;
      }, function(viewValue) {
        // viewValue is properly initialized once it is no longer 'NaN'
        // Source: https://github.com/angular/angular.js/blob/v1.0.6/src/ng/directive/input.js#L879
        if (initialValue === undefined && !(angular.isNumber(viewValue) && isNaN(viewValue))) {
          initialValue = viewValue || '';
          initialValueWatch();
        }
      });

      scope.$watch(function() {
        return ngModelController.$pristine;
      }, function(isPristine) {
        if (isPristine) {
          initialValue = ngModelController.$viewValue || '';
          element.addClass('initial-value');
        }
      });

      ngModelController.$viewChangeListeners.push(function() {
        if (ngModelController.$viewValue === initialValue) {
          element.addClass('initial-value');
        }
        else {
          element.removeClass('initial-value');
        }
      });
    }
  }
}
