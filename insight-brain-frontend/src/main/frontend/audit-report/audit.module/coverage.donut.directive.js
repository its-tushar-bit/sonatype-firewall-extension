/**
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global HealthCheck */
export default function CoverageDonut() {
  return {
    scope: {
      percentKnownComponents: '=coverageDonut'
    },
    link: function(scope, element) {
      function updateGraph() {
        if (scope.percentKnownComponents !== undefined) {
          HealthCheck.artifactsChart(1 - scope.percentKnownComponents / 100, {element: element[0]});
        }
      }

      scope.$watch('percentKnownComponents', updateGraph);
    }
  };
}
