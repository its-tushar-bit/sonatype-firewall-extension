/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  angular.module('cip.label.editor').directive('spinner', function() {
    var properties = ['-ms-transform', '-webkit-transform', '-moz-transform', 'transform'];

    function setElement(element, value) {
      angular.forEach(properties, function(prop) {
        element.css(prop, value);
      });
      return element;
    }

    return function(scope, element) {
      element.bind('click', function() {
        setElement(element, '').prop('rotate', null).animate({ rotate: '+360'}, {
          step: function(now) {
            now = now % 360;
            setElement(element, 'rotate(' + now + 'deg)');
          }
        });
      });
    };
  });
}());
