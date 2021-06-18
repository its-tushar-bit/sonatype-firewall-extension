/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { renderVersionGraph } from '@sonatype/version-graph';

export default function graphDirective(Coordinates) {
  return {
    scope: {
      versions: '=graph',
    },
    template:
      '<div ng-show="versions">' +
      '<div id="aiVersionChartContainer">' +
      '<div id="aiVersionChartLabels"></div>' +
      '<div id="aiVersionChartViz" style="overflow:hidden"></div>' +
      '</div>' +
      '</div>',
    link: function (scope) {
      scope.$watch('versions', function (versions) {
        if (versions) {
          renderVersionGraph({
            data: {
              nextMajorRevisionIndex: versions.nextMajorRevisionIndex,
              versions: versions,
              version: Coordinates.get().version,
            },
            selectable: true,
            showCurrentVersionLabel: true,
            versionClick: function (version) {
              scope.$apply(function () {
                $.each(versions, function (index, component) {
                  if (component.componentIdentifier.coordinates.version === version) {
                    Coordinates.setSelected(component.componentIdentifier.coordinates);
                    Coordinates.setIdentificationSource(component.identificationSource);
                    return false;
                  }
                });
              });
            },
            versionDblClick: function (version) {
              if (scope.$root.type !== 'ide') {
                scope.$emit('viewDetails', version);
              }
            },
          });
        }
      });
    },
  };
}

graphDirective.$inject = ['Coordinates'];
