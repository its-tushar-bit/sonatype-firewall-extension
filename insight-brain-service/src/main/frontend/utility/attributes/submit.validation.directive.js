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
        submitDirty: '&',
        submitType: '&'
      },
      require: '^form',
      link: function(scope, element, attrs, formCtrl) {
        var isSubmissionValid,
            isSubmissionDirty;

        scope.$watchGroup([
          function() {
            return scope.submitDirty();
          }, function() {
            return formCtrl.$valid;
          }
        ], function(results) {
          isSubmissionDirty = results[0];
          isSubmissionValid = isSubmissionDirty && formCtrl.$valid;

          if (isSubmissionValid) {
            element.tooltip('destroy');
            element.removeClass('disabled');
          }
          else {
            insertAndUpdateTooltip();
            element.addClass('disabled');
          }
        });

        scope.$watch(function() {
          return scope.submitType() || attrs.submitType;
        }, function() {
          if (!isSubmissionValid) {
            insertAndUpdateTooltip();
          }
        });

        function insertAndUpdateTooltip() {
          var title,
              submitType = scope.submitType() || attrs.submitType;

          if (submitType === 'update' && !isSubmissionDirty) {
            title = 'There are no changes to update.';
          }
          else {
            title = 'Unable to ' + (submitType ? submitType : 'save') + ': fields with invalid or missing data.';
          }

          element.tooltip({container: element}).attr('title', title).tooltip('fixTitle');
        }
      }
    };
  }

  angular.module('utility').directive('submitValidation', SubmitValidation);
}(angular));
