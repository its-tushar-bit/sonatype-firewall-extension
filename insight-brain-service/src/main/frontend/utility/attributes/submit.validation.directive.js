/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SubmitValidation() {
    return {
      restrict: 'A',
      scope: {
        submitValidation: '=',
        submitType: '&'
      },
      require: '^form',
      link: function(scope, element, attrs, formCtrl) {
        var isSubmissionValid;

        scope.$watch('submitValidation', function(newValue) {
          isSubmissionValid = newValue;

          if (isSubmissionValid) {
            element.tooltip('destroy');
            element.removeClass('disabled');
          }
          else {
            insertAndUpdateTooltip();
            element.addClass('disabled');
          }
        });

        scope.$watchGroup([
          attrs.submitType, function() {
            return formCtrl.$pristine;
          }
        ], function() {
          if (!isSubmissionValid) {
            insertAndUpdateTooltip();
          }
        });

        function insertAndUpdateTooltip() {
          var title,
              submitType = scope.submitType() || attrs.submitType;

          switch (submitType) {
            case 'update':
              title = formCtrl.$pristine ? 'There are no changes to update.' : 'Unable to ' +
              (submitType ? submitType : 'save') + ' with invalid or missing fields.';
              break;
            default:
              title = 'Unable to ' + (submitType ? submitType : 'save') + ' with invalid or missing fields.';
              break;
          }

          element.tooltip({container: element}).attr('title', title).tooltip('fixTitle');
        }
      }
    };
  }

  angular.module('utility').directive('submitValidation', SubmitValidation);
}(angular));
