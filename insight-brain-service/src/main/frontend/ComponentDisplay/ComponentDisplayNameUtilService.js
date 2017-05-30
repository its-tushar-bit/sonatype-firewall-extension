/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function ComponentDisplayNameUtil($filter) {
  var renderToString = function(displayName) {
    return $.map(displayName.parts, function(part) {
      return part.value;
    }).join('');
  };

  var deriveComponentName = function(component) {
    if (component.displayName) {
      return renderToString(component.displayName);
    }
    else {
      return component.pathnames ? $filter('fileName')(component.pathnames[0]) : 'Unknown';
    }
  };

  return {
    renderToString: renderToString,
    deriveComponentName: deriveComponentName
  };
}

ComponentDisplayNameUtil.$inject = ['$filter'];
