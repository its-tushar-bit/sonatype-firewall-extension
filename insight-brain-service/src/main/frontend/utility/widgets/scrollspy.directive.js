/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ScrollSpy($timeout) {
    return {
      scope: {
        scrollspy: '@'
      },
      link: function($scope, element) {
        element.scrollspy({
          target: $scope.scrollspy,
          offset: 0
        });

        var eventHandlerFn = function() {
          var me = $(this);
          element.scrollTop($(me.attr('data-target')).position().top + element.scrollTop());
          $timeout(function() {
            $($scope.scrollspy + ' .nav li').removeClass('active');
            me.parent().addClass('active');
          });
        };

        $(document).on('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);

        $scope.$on('$destroy', function() {
          $(document).off('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);
        });
      }
    };
  }

  ScrollSpy.$inject = ['$timeout'];

  angular
      .module('utility')
      .directive('scrollspy', ScrollSpy);

}(angular));
