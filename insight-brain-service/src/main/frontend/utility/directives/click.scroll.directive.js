/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ClickScroll($uiViewScroll) {
    return {
      restrict: 'A',
      link: function(scope, element) {
        element.on('click', function() {
          $uiViewScroll(element);
        });
      }
    };
  }

  ClickScroll.$inject = ['$uiViewScroll'];

  angular.module('utility.directives').directive('clickScroll', ClickScroll);

}(angular));
