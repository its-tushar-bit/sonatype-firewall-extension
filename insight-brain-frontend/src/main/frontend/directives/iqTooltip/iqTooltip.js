/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * "iq-tooltip" reusable directive
 * Attributes:
 *
 *
 * Example:
 *
 */
var iqTooltip = function ($timeout, $window) {
  return {
    restrict: 'A',
    scope: {
      enabled: '<iqTooltip',
    },
    link: function (scope, element, attrs) {
      // iq-tooltip attr can be used either with a boolean value or without a value. If no value, that means enabled
      if (scope.enabled === false) {
        return;
      }

      var tooltipClass = attrs.tooltipClass || '',
        templateString =
          '<div class="tooltip iq-tooltip ' +
          tooltipClass +
          '" role="tooltip">' +
          '<div class="tooltip-arrow"></div>' +
          '<div class="tooltip-inner"></div>' +
          '</div>',
        options = {
          title: attrs.tooltipText,
          container: attrs.tooltipAttachToBody === undefined ? false : 'body',
          template: templateString,
          html: attrs.tooltipContentIsHtml,
          trigger: 'manual',
        },
        overflowFlag,
        showTooltipPromise,
        resetOverflowFlag = function () {
          overflowFlag = undefined;
        };
      angular.element($window).on('resize', resetOverflowFlag);

      function elementOverflows() {
        if (overflowFlag === undefined) {
          var overflowElement = attrs.tooltipOverflowElementSelector
            ? element[0].querySelector(attrs.tooltipOverflowElementSelector)
            : element[0];
          overflowFlag = overflowElement.scrollWidth > overflowElement.clientWidth;
        }
        return overflowFlag;
      }

      $(element).tooltip(options);

      $(element).hover(
        function () {
          // on mouseenter
          if (scope.enabled === false) {
            return;
          }
          if (attrs.tooltipOnlyOnOverflow !== 'true' || elementOverflows()) {
            showTooltipPromise = $timeout(function () {
              $(element).tooltip('show');
            }, attrs.tooltipDelay || 300);
          }
        },
        function () {
          // on mouseleave
          $timeout.cancel(showTooltipPromise);
          $(element).tooltip('hide');
        }
      );

      scope.$on('$destroy', function () {
        angular.element($window).off('resize', resetOverflowFlag);
        $(element).tooltip('destroy');
      });
    },
  };
};

iqTooltip.$inject = ['$timeout', '$window'];

export default iqTooltip;
