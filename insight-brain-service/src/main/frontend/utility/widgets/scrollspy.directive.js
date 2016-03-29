/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function ScrollSpy($timeout, $http, EventNameConstant) {
    return {
      scope: {
        scrollspy: '@'
      },
      link: function($scope, element) {
        var scrollspyObject;
        initScrollspy();

        var eventHandlerFn = function() {
          pauseScrollspy(scrollspyObject.$scrollElement);
          var me = $(this);
          //note the offset is 8 here, as using a higher number will occassionally push us into the next section
          //and select the wrong pill
          element.scrollTop($(me.attr('data-target')).position().top + element.scrollTop() - 8);
          $($scope.scrollspy + ' .nav li').removeClass('active');
          me.parent().addClass('active');
          $timeout(function(){
            unpauseScrollspy(scrollspyObject.$scrollElement);
          });
        };

        $(document).on('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);

        $scope.$on('$destroy', function() {
          $(document).off('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);
        });

        $scope.$on(EventNameConstant.UPDATE_SCROLLSPY, function(event, options){
          if (scrollspyObject) {
            if (options) {
              if (options.resetScroll) {
                $($scope.scrollspy + ' .nav li:first-child > a').click();
              }
              if (options.refresh) {
                scrollspyObject.refresh();
              }
            }
            else {
              scrollspyObject.refresh();
            }
          }
        });

        function initScrollspy() {
          if ($http.pendingRequests.length === 0 && $($scope.scrollspy + ' .nav li').length) {
            scrollspyObject = new $.fn.scrollspy.Constructor(element, {
              target: $scope.scrollspy,
              offset: 10
            });
          }
          else {
            $timeout(function(){
              initScrollspy();
            }, 100);
          }
        }

        function pauseScrollspy() {
          $(element).off('scroll.scroll-spy.data-api');
        }

        function unpauseScrollspy() {
          $(element).on('scroll.scroll-spy.data-api', $.proxy(scrollspyObject.process, scrollspyObject));
        }
      }
    };
  }

  ScrollSpy.$inject = ['$timeout', '$http', 'event.name.constant'];

  angular
      .module('utility')
      .directive('scrollspy', ScrollSpy);

}(angular));
