/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function OwnerImageDirective(CLMContextLocations) {
  return {
    scope: {
      owner: '=ownerImage',
    },
    template: '<img ng-src="{{ownerUrl}}" ng-if="ownerUrl">',
    link: function (scope) {
      scope.$watch('owner', function () {
        if (scope.owner) {
          scope.ownerUrl = CLMContextLocations.getOwnerImageUrl(scope.owner) + '?timestamp=' + Date.now();
        }
      });
    },
  };
}

OwnerImageDirective.$inject = ['CLMContextLocations', 'event.name.constant'];
