/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';
  
  var types = {
    'maven': ['groupId', 'artifactId', 'version'],
    'a-name': ['name', 'qualifier', 'version']
  };

  // colon separated to object
  function parse(input) {
    var parts = (input || '').split(':'),
        coordinates = {
          format: parts.shift().trim()
        },
        fields = types[coordinates.format];

    if (fields) {
      parts.forEach(function(part, partIndex) {
        coordinates[fields[partIndex]] = part.trim();
      });
    }
    else {
      coordinates.format = 'maven';
    }
    return coordinates;
  }

  function CoordinatesInputController($scope) {
    var vm = this;

    vm.coordinates = parse($scope.value);
    vm.invalidRegex = '[^:]*';

    $scope.$watch('vm.coordinates', function(newCoordinates, oldCoordinates) {
      if (oldCoordinates !== newCoordinates) {
        if (oldCoordinates.format !== newCoordinates.format) {
          vm.coordinates = parse(newCoordinates.format);
          $scope.value = undefined;
        }
        else {
          var typeFields = types[vm.coordinates.format];
          if (typeFields) {
            var values = [vm.coordinates.format];
            typeFields.forEach(function(field) {
              values.push(vm.coordinates[field]);
            });

            while(values.length > 0 && !values[values.length - 1]) {
              values.pop();
            }

            $scope.value = values.length > 1 ? values.join(':') : undefined;
          }
          else {
            $scope.value = undefined;
          }
        }
      }
    }, true);
  }
  CoordinatesInputController.$inject = ['$scope'];

  function CoordinatesInput() {
    return {
      transclude: true,
      restrict: 'E',
      scope: {
        value: '='
      },
      controller: CoordinatesInputController,
      controllerAs: 'vm',
      link: function(scope, element, attrs, ctrl, transclude) {
        transclude(scope, function(clone) {
          element.append(clone);
        });
        scope.identifier = Math.random();
      }
    };
  }

  angular //
      .module('owner.manager.module') //
      .directive('coordinatesInput', CoordinatesInput);

}(angular));
