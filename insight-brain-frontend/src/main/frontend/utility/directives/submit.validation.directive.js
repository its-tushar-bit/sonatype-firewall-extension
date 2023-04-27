/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNil } from 'ramda';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';
export default function SubmitValidation() {
  return {
    restrict: 'A',
    scope: {
      submitDirty: '&',
      submitValidationError: '&?',
      submitType: '&',
      submitTooltipTarget: '@?', // useful for elements relatively positioned
    },
    require: '^form',
    link: function (scope, element, attrs, formCtrl) {
      var isSubmissionValid, isSubmissionDirty;

      scope.$watchGroup(
        [
          function () {
            return scope.submitDirty();
          },
          function () {
            return formCtrl.$valid;
          },
          function () {
            if (scope.submitValidationError) {
              return scope.submitValidationError();
            }
          },
        ],
        function (results) {
          const validationError = scope.submitValidationError ? scope.submitValidationError() : null;

          isSubmissionDirty = results[0];
          isSubmissionValid = !isNil(validationError)
            ? !validationError && isSubmissionDirty && formCtrl.$valid
            : isSubmissionDirty && formCtrl.$valid;

          if (isSubmissionValid) {
            element.tooltip('destroy');
            element.removeClass('disabled');
          } else {
            insertAndUpdateTooltip();
            element.addClass('disabled');
          }
        }
      );

      scope.$watch(
        function () {
          return scope.submitType() || attrs.submitType;
        },
        function () {
          if (!isSubmissionValid) {
            insertAndUpdateTooltip();
          }
        }
      );

      // prevent form submissions if not valid
      element.on('click', function (e) {
        if (!(isSubmissionValid && isSubmissionDirty)) {
          e.preventDefault();
        }
      });

      function insertAndUpdateTooltip() {
        var title,
          submitType = scope.submitType() || attrs.submitType;

        if (submitType === 'update' && !isSubmissionDirty) {
          title = MSG_NO_CHANGES_TO_UPDATE;
        } else {
          title = 'Unable to ' + (submitType ? submitType : 'save') + ': fields with invalid or missing data.';
        }

        var options = {};
        if (scope.submitTooltipTarget) {
          options.container = scope.submitTooltipTarget;
        }
        element.tooltip(options).attr('title', title).tooltip('fixTitle');
      }
    },
  };
}
