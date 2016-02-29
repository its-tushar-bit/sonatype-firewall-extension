/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, Insight*/
(function() {
  'use strict';

  angular.module('version.graph').directive('graph', ['Coordinates', 'Properties', function (Coordinates, Properties) {
    return {
      scope : {
        versions : '=graph'
      },
      template : '<div ng-show="versions">' +
                   '<div id="aiVersionChartContainer">' +
                     '<div id="aiVersionChartLabels"></div>' +
                     '<div id="aiVersionChartViz" style="overflow:hidden"></div>' +
                   '</div>' +
                 '</div>',
      link : function (scope) {
        scope.$watch('versions', function (versions) {
          if (versions) {
            $.each(versions, function(index, component) {
              if (component.componentIdentifier.coordinates.version === Coordinates.get().version) {
                component.hash = Properties.getHash();
                return false;
              }
            });

            Insight.ComponentInformation({
              data: {
                nextMajorRevisionIndex : versions.nextMajorRevisionIndex,
                versions: versions,
                version: Coordinates.get().version
              },
              selectable: true,
              versionClick: function(version) {
                scope.$apply(function () {
                  $.each(versions, function(index, component) {
                    if (component.componentIdentifier.coordinates.version === version) {
                      Coordinates.setSelected(component.componentIdentifier.coordinates);
                      Properties.setHash(component.hash);
                      return false;
                    }
                  });
                });
              },
              versionDblClick: function(version) {
                scope.$emit('viewDetails', version);
              }
            });
          }
        });
      }
    };
  }]);
}());
