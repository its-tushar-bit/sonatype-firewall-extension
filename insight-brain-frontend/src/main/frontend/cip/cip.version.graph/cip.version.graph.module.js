/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular*/
import versionGraphModule from '../../version-graph/version.graph/version.graph.module';

function getFilename(pathname) {
  var lastSegmentIndex = pathname.lastIndexOf('/');
  if (lastSegmentIndex !== -1) {
    return pathname.substring(lastSegmentIndex + 1);
  }
  return pathname;
}

function run($rootScope, SelectedComponent, Coordinates, Properties) {
  $rootScope.$watch(
    function () {
      return SelectedComponent.get();
    },
    function (newComponent) {
      if (newComponent) {
        if (newComponent.componentIdentifier) {
          Coordinates.set(newComponent.componentIdentifier.format, newComponent.componentIdentifier.coordinates);
        } else if (newComponent.groupId) {
          Coordinates.set('maven', {
            groupId: newComponent.groupId,
            artifactId: newComponent.artifactId,
            version: newComponent.version,
          });
        } else {
          Coordinates.set(null, {}); // unknown
        }
        Properties.setHash(newComponent.hash);
        Properties.setFilename(getFilename(newComponent.pathname));
        Properties.setPathname(newComponent.pathname);
        Properties.setProprietary(newComponent.proprietary || false);
        Properties.setMatchState(newComponent.matchState || 'exact');
      } else {
        Coordinates.set(null, null);
        Properties.reset();
      }
    }
  );
}
run.$inject = ['$rootScope', 'SelectedComponent', 'Coordinates', 'Properties'];

export default angular.module('cip.version.graph', [versionGraphModule.name]).run(run);
