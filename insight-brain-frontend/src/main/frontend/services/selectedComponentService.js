/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function SelectedComponent() {
  var selectedComponent = null;
  return {
    get: function () {
      return selectedComponent;
    },
    toggle: function (component) {
      if (component === selectedComponent) {
        selectedComponent = null;
      } else {
        selectedComponent = component;
      }
    },
  };
}

export default angular.module('selectedComponentService', []).service('SelectedComponent', SelectedComponent);
