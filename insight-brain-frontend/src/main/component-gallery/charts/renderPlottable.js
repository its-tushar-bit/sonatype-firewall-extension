/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function renderPlottable() {
  return {
    scope: {
      chart: '=renderPlottable'
    },
    link: function(scope, el) {
      scope.chart.renderTo(el[0]);
      window.addEventListener("resize", function() {
        scope.chart.redraw();
      });
    }
  };
}
