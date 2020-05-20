/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default angular.module('TestComponentProvider', []).service('SelectedComponent', function() {
  var component = {
    hash: '3102cdd0edd5a05afe00',
    observedLicenses: [],
    componentIdentifier: {
      groupId: 'tomcat',
      artifactId: 'catalina',
      version: '5.0.28'
    }
  };
  return {
    get: function () {
      return component;
    },
    set: function(newComponent) {
      component = newComponent;
    }
  };
}).service('OwnerContext', function () {
  return {
    ownerId: 'bom1-12345678',
    ownerType: 'application'
  };
});

