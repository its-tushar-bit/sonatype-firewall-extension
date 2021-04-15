/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import angularCommonModule from './util/AngularCommon';

var module = angular
  .module('FormsModule', [angularCommonModule.name])
  /**
   * Watches for changes to the input validity and shows a popover above the input field if invalid input is seen, or
   * if a required field loses focus without having been set.
   * Customized messages for error conditions can be provided using the 'messages' attr and providing an Object of
   * error key to message. The 'messages' Object will be consulted first so it can also be used to override the defaults.
   */
  .directive('clmInput', [
    '$timeout',
    '$window',
    function ($timeout, $window) {
      /**
       * @param error {Object} Map of invalid keys
       * @param attrs {Object} Attributes to consult for settings
       * @param messages {Object} Optional custom or override messages
       * @returns {string} The appropriate message for the error
       */
      function determineErrorMessage(error, attrs, messages) {
        var message = '';

        //first check any custom messages provided
        if (messages) {
          $.each(error, function (key, inError) {
            if (inError && messages[key]) {
              message = messages[key];
              return false;
            }
          });
        }
        if (message) {
          return message;
        }

        //look in the default messages
        if (error.email) {
          message = 'Use valid format: abc@xyz.com';
        } else if (error.spaces) {
          message = 'No leading, trailing or double spaces or tabs';
        } else if (error.validNameCharacters) {
          message = 'Use valid characters: alphanumeric, "_", ".", "-", or spaces';
        } else if (error.number) {
          message = 'Please enter a valid number';
        } else if (error.minlength) {
          message = 'Minimum length is ' + attrs.ngMinlength;
        } else if (error.maxlength) {
          message = 'Maximum length is ' + attrs.ngMaxlength;
        } else if (error.pattern) {
          message = 'Must match pattern: ' + attrs.ngPattern;
        } else if (error.min) {
          message = 'Minimum allowed value is ' + attrs.min;
        } else if (error.max) {
          message = 'Maximum allowed value is ' + attrs.max;
        } else if (error.required) {
          message = 'Please enter a value';
        } else if (error.noSpaces) {
          message = 'Spaces or tabs are not allowed';
        }
        return message;
      }

      return {
        restrict: 'A',
        require: ['ngModel', '^form'],
        link: function (scope, element, attrs, ctrls) {
          var ctrl = ctrls[0],
            form = ctrls[1],
            messages,
            debounce,
            debounceDelay = 100;

          if (attrs.messages) {
            messages = scope.$eval(attrs.messages);
            if (typeof messages !== 'object') {
              throw new Error('Messages provided to the input must be an Object!');
            }
          }

          if (!ctrl.$name) {
            throw new Error('The input must have a name');
          }
          if (!form.$name) {
            throw new Error('The form must have a name');
          }

          // display a popover on validation errors
          function updateValidationMessages(myElem, myAttrs, myCtrl) {
            var currentPopover = myElem.data('popover');
            if (myCtrl.$invalid) {
              var errorMessage = determineErrorMessage(myCtrl.$error, myAttrs, messages);
              if (currentPopover) {
                if (currentPopover.options.content !== errorMessage) {
                  currentPopover.options.content = errorMessage;
                  // Must call show or the position is not updated
                  myElem.popover('show');
                }
              } else {
                // Popover template removes the title and adds our style overrides
                myElem.popover({
                  placement: 'top',
                  content: errorMessage,
                  trigger: 'manual',
                  html: true,
                  container: element.parent(),
                  template:
                    '<div id="' +
                    myCtrl.$name +
                    '-popover" class="popover input-popover fade top in">' +
                    '<div class="arrow"></div>' +
                    '<div class="popover-content">' +
                    '</div>' +
                    '</div>',
                });

                var popover = myElem.data('popover');
                popover.getOriginalPosition = popover.getPosition;
                // Reposition the popover on the right edge of the input field
                popover.getPosition = function () {
                  var position = this.getOriginalPosition();
                  var newPosition = {
                    left: myElem[0].getBoundingClientRect().left + myElem[0].offsetWidth - popover.tip()[0].offsetWidth,
                    top: position.top + 5,
                    width: popover.tip()[0].offsetWidth,
                  };
                  return angular.extend(position, newPosition);
                };
                myElem.popover('show');
              }
            } else {
              // remove any previous warning
              if (currentPopover) {
                myElem.popover('destroy');
              }
            }
          }

          // When the element is removed we need to remove the popover as well.
          scope.$on('$destroy', function () {
            element.popover('destroy');
          });

          scope.$watch(
            function () {
              return form.$submitted;
            },
            function (isFormSubmitted) {
              if (isFormSubmitted) {
                updateValidationMessages(element, attrs, ctrl);
              }
            }
          );

          // Update state whenever the $error state is changed and/or the dirty state has changed
          scope.$watch(
            function () {
              return {
                error: form[ctrl.$name].$error,
                dirty: ctrl.$dirty,
              };
            },
            function () {
              if (ctrl.$dirty) {
                updateValidationMessages(element, attrs, ctrl);
              }
            },
            true
          );

          // If $setPristine() is called the ctrl will not be $dirty, so
          // handle this case specifically
          scope.$watch(
            function () {
              return form[ctrl.$name].$pristine;
            },
            function (newValue, oldValue) {
              if (!oldValue && newValue) {
                // If the form is set to pristine we can remove any existing popover
                var currentPopover = element.data('popover');
                if (currentPopover) {
                  element.popover('destroy');
                }
              }
            }
          );

          var repositionPopover = function () {
            $timeout.cancel(debounce);
            debounce = $timeout(function () {
              if (element.data('popover')) {
                element.popover('show');
              }
            }, debounceDelay);
          };
          angular.element($window).on('resize', repositionPopover);

          scope.$on('$destroy', function () {
            angular.element($window).off('resize', repositionPopover);
          });
        },
      };
    },
  ])
  /**
   * Template for common structure of form inputs and associated labels.
   */
  .directive('clmControlGroup', function () {
    return {
      restrict: 'A',
      require: '^form',
      replace: true,
      transclude: true,
      scope: {
        for: '@',
        label: '@',
      },
      template:
        '<div class="control-group">' +
        '<label ng-if="label" class="control-label" for="{{for}}">{{label}}</label>' +
        '<div class="controls">' +
        '<div ng-transclude></div>' +
        '</div>' +
        '</div>',
    };
  });

export default module;
