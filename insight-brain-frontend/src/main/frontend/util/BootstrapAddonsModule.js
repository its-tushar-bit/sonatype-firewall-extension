/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular, $ */
var bootstrapAddons = angular.module('BootstrapAddons', []);
export default bootstrapAddons;

bootstrapAddons.directive('slider', function() {
  return {
    restrict: 'A',
    scope: {
      model: '<value',
      min: '@',
      max: '@',
      hideLabels: '@',
      rangeHighlights: '<?',
      onChange: '&'
    },
    priority: 99,
    link: function(scope, element) {
      $(element).slider({
        min: parseInt(scope.min),
        max: parseInt(scope.max),
        value: scope.model,
        orientation: 'horizontal',
        selection: 'after',
        handle: 'square',
        tooltip: 'none',
        labels: !scope.hideLabels,
        showHandleValues: true,
        rangeHighlights: scope.rangeHighlights
      }).on('slide', function(event) {
        if (!angular.equals(event.value, scope.model)) {
          scope.$apply(function() {
            scope.onChange({value: event.value});
          });
        }
      });

      scope.$watch('model', function(newValue) {
        $(element).slider('setValue', newValue);
      });
    }
  };
});

bootstrapAddons.directive('toggleCheckbox', [function() {
  return {
    restrict: 'A',
    require: 'ngModel',
    scope: {
      disabled: '=ngDisabled'
    },
    link: function($scope, $element, $attr, ngModel) {
      $element.bootstrapToggle({
        height: '24px'
      });
      $element.on('change', function() {
        var checked = $element.prop('checked');
        if (checked !== ngModel.$viewValue) {
          ngModel.$setViewValue(checked);
        }
      });
      $scope.$watch(function() {
        return ngModel.$viewValue;
      }, function() {
        // BootstrapToggle API disables changes if disabled. Call to enable does not need to invoke change as UI does
        // not need updates.
        var disabled = $element.prop('disabled');
        if (disabled) {
          $element.prop('disabled', false);
        }
        $element.prop('checked', ngModel.$viewValue).change();
        if (disabled) {
          $element.prop('disabled', true).change();
        }
      });
      $scope.$watch('disabled', function() {
        $element.prop('disabled', $scope.disabled);
      });
    }
  };
}]);
