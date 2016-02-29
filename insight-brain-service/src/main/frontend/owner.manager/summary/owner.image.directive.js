/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function OwnerImageDirective(CLMAppLocations) {
    return {
      scope: {
        owner: '=ownerImage'
      },
      template: '<img ng-src="{{ownerUrl}}" ng-if="ownerUrl">',
      link: function(scope) {
        scope.$watch('owner', function() {
          if (scope.owner) {
            scope.ownerUrl = CLMAppLocations.getOwnerImageUrl(scope.owner);
          }
        });

        scope.$on('owner.image.change', function(owner) {
          if (scope.owner === owner && scope.ownerUrl) {
            if (scope.ownerUrl.indexOf('?') !== -1) {
              scope.ownerUrl = scope.ownerUrl.substring(0, scope.ownerUrl.indexOf('?'));
            }
            scope.ownerUrl += '?timestamp=' + Date.now();
          }
        });
      }
    };
  }

  OwnerImageDirective.$inject = ['CLMAppLocations'];

  angular
      .module('owner.manager.module')
      .directive('ownerImage', OwnerImageDirective);

}(angular));
