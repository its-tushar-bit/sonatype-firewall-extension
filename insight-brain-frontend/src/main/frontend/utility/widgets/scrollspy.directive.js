/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function ScrollSpy($timeout, EventNameConstant, StableBodyService) {
  return {
    scope: {
      scrollspy: '@'
    },
    link: function($scope, element) {
      var scrollspyObject;
      initScrollspy();

      var eventHandlerFn = function() {
        pauseScrollspy(scrollspyObject.$scrollElement);

        //note the offset is 8 here, as using a higher number will occasionally push us into the next section
        //and select the wrong pill
        const me = $(this),
            targetEl = $(me.attr('data-target')),
            scrollPosition = targetEl.position().top + element.scrollTop() - 8;

        try {
          element[0].scrollTo({
            top: scrollPosition,
            behavior: 'smooth'
          });
        }
        catch (e) {
          // IE is as IE does
          element[0].scrollTop = scrollPosition;
        }

        $($scope.scrollspy + ' .nav li').removeClass('active');
        me.parent().addClass('active');
        $timeout(function() {
          unpauseScrollspy(scrollspyObject.$scrollElement);
        });
      };

      $(document).on('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);

      $scope.$on('$destroy', function() {
        $(document).off('click', $scope.scrollspy + ' .nav li > a', eventHandlerFn);
      });

      $scope.$on(EventNameConstant.UPDATE_SCROLLSPY, function(event, options) {
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
        StableBodyService.whenStable(function() {
          $timeout(function() {
            scrollspyObject = new $.fn.scrollspy.Constructor(element, {
              target: $scope.scrollspy,
              offset: 10
            });
            element.addClass('scroll-spy-initialized');
          }, 250);
        });
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

ScrollSpy.$inject = ['$timeout', 'event.name.constant', 'stable.body.service'];
