/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.directive('pathnamesPopover', function() {
    var uniqueCounter = 0;
    return {
      restrict: 'A',
      link: function(scope, element, attrs) {
        scope.$watch(attrs.pathnamesPopover, function(pathnames) {
          var uniqueId = 'pathname-popover-' + uniqueCounter;
          uniqueCounter++;

          if (!pathnames) {
            return;
          }

          var pathnamesTitle = 'Component Path';
          if (pathnames.length > 1) {
            pathnamesTitle = 'Component Path, Found in ' + pathnames.length + ' Locations';
          }

          var options = {
            trigger: 'manual',
            placement: 'top',
            content: pathnames[0],
            title: pathnamesTitle,
            // Attach the popover to the body of the document, as certain browsers (IE9) will fail otherwise.
            container: 'body',
            // Add our styling, pathnames-popover and pathnames-popover-content, to the popover template.
            template: '<div data-popup-id="' + uniqueId + '" class="popover pathnames-popover"><div class="pathnames-popover-arrow"></div>' +
            '<div class="popover-inner"><h3 class="popover-title"></h3>' +
            '<div class="popover-content pathnames-popover-content"><p></p></div></div></div>'
          };

          // Configure the popover so that it functions modally.
          element.popover(options);
          // The position function will be modified to move the popover over the text
          // within the TD dynamically based on the current element positions.
          element.data('popover').getOriginalPosition = element.data('popover').getPosition;

          // Display the popover when hovering over the component element, but only hide
          // the popover when the mouse leaves the popover.
          element.on('mouseenter', function() {
            // Add a slight delay so popovers aren't appearing as the user moves
            // their mouse across the table.
            setTimeout(function() {
              if(!element.is(':hover')) {
                return;
              }

              // Calculate the position of the popover in reference to the left adjusted text.
              var emphasizedPathnameElement = element.find('em');
              if (emphasizedPathnameElement.length > 0) {
                var popoverLeftPosition = emphasizedPathnameElement.offset().left;
                element.data('popover').getPosition = function () {
                  var originalPosition = this.getOriginalPosition();
                  originalPosition.left = popoverLeftPosition;
                  // Set the width to the width of the popover so that it stays aligned
                  // with the left of the text.
                  originalPosition.width = this.tip()[0].offsetWidth;
                  return originalPosition;
                };
              }

              element.popover('show');

              var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
              popover.on('mouseleave', function() {
                element.popover('hide');
              });
            }, 50);
          });

          // Also, hide the popover if the mouse leaves the component element and is no
          // longer hovering over the popover.
          element.on('mouseleave', function() {
            var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
            setTimeout(function() {
              if (popover.length > 0 && !popover.is(':hover')) {
                element.popover('hide');
              }
            }, 100);
          });

          // When the element is removed we need to remove the popover as well.
          scope.$on('$destroy', function() {
            var popover = $('.pathnames-popover[data-popup-id=' + uniqueId + ']');
            if (popover.length > 0) {
              popover.remove();
            }
          });

        });
      }
    };
  });

}());
