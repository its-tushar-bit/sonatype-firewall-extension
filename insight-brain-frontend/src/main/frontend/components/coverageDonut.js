/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

// sets up HealthCheck global
import '../version-graph/appcheck';

/*global HealthCheck */
export default function CoverageDonut() {
  return {
    scope: {
      percentKnownComponents: '<coverageDonut',
      width: '<donutWidth',
      height: '<donutHeight',
      fillColors: '<',
      strokeColor: '<',
      lineWidth: '<',
      innerRadius: '<',
      outerRadius: '<',
    },
    link: function (scope, element) {
      function updateGraph() {
        if (scope.percentKnownComponents !== undefined) {
          HealthCheck.artifactsChart(1 - scope.percentKnownComponents / 100, {
            ...pick(['width', 'height', 'fillColors', 'strokeColor', 'lineWidth', 'innerRadius', 'outerRadius'], scope),
            element: element[0],
          });
        }
      }

      scope.$watchGroup(
        [
          'percentKnownComponents',
          'width',
          'height',
          'fillColors',
          'strokeColor',
          'lineWidth',
          'innerRadius',
          'outerRadius',
        ],
        updateGraph
      );
    },
  };
}
