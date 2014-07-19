/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, AngularUtils */
(function() {
  'use strict';
  angular.module('FormsModule', ['AngularCommon'])
  /**
   * Watches for changes to the input validity and shows a popover above the input field if invalid input is seen, or
   * if a required field loses focus without having been set.
   * Customized messages for error conditions can be provided using the 'messages' attr and providing an Object of
   * error key to message. The 'messages' Object will be consulted first so it can also be used to override the defaults.
   */
      .directive('clmInput', function() {
        /**
         * @param error {Object} Map of invalid keys
         * @param attrs {Object} Attributes to consult for settings
         * @returns {string} The appropriate message for the error
         */
        function determineErrorMessage(error, attrs, messages) {
          var message = '';

          //first check any custom messages provided
          if (messages) {
            $.each(error, function(key, inError) {
                  if (inError && messages[key]) {
                    message = messages[key];
                    return false;
                  }
                }
            );
          }
          if(message){
            return message;
          }

          //look in the default messages
          if (error.number) {
            message = 'Please enter a valid number';
          }
          else if (error.minlength) {
            message = 'Minimum length is ' + attrs.ngMinlength;
          }
          else if (error.maxlength) {
            message = 'Maximum length is ' + attrs.ngMaxlength;
          }
          else if (error.pattern) {
            message = 'Must match pattern: ' + attrs.ngPattern;
          }
          else if (error.min) {
            message = 'Minimum allowed value is ' + attrs.min;
          }
          else if (error.max) {
            message = 'Maximum allowed value is ' + attrs.max;
          }
          else if (error.required) {
            message = 'Please enter a value';
          }
          return message;
        }

        return{
          restrict: 'A',
          require: ['ngModel', '^form'],
          link: function(scope, element, attrs, ctrls) {
            var ctrl = ctrls[0], form = ctrls[1], messages;

            if (attrs.messages) {
              messages = scope.$eval(attrs.messages);
              if (typeof messages !== 'object') {
                throw 'Messages provided to the input must be an Object!';
              }
            }

            if (!ctrl.$name) {
              throw 'The input must have a name';
            }
            if (!form.$name) {
              throw 'The form must have a name';
            }

            // display a popover on validation errors
            function updateValidationMessages(myElem, myAttrs, myCtrl) {
              var currentPopover = myElem.data('popover');
              if (myCtrl.$invalid) {
                if (currentPopover) {
                  currentPopover.options.content = determineErrorMessage(myCtrl.$error, myAttrs, messages);
                } else {
                  //popover template removes the title and adds our style overrides
                  myElem.popover({
                    placement: 'top',
                    content: determineErrorMessage(myCtrl.$error, myAttrs, messages),
                    trigger: 'manual',
                    template: '<div class="popover input-popover fade top in">' +
                        '<div class="arrow"></div>' +
                        '<div class="popover-content">' +
                        '</div>' +
                        '</div>'
                  });
                  myElem.popover('show');
                  var popover = myElem.data('popover');
                  //reposition the popover on the right edge of the input field
                  var position = popover.getPosition();
                  position.left = position.right - popover.tip()[0].offsetWidth;
                  position.top = position.top - 30;
                  popover.applyPlacement(position, 'top');
                }
              } else {
                // remove any previous warning
                if (currentPopover) {
                  myElem.popover('destroy');
                }
              }
            }

            // When the element is removed we need to remove the popover as well.
            scope.$on('$destroy', function() {
              element.popover('destroy');
            });

            // Force validation when the field loses focus
            element.on('blur keyup', function() {
              //force the element to validate and set a dirty state
              AngularUtils.safeApply(scope, function() {
                ctrl.$setViewValue(element.val());
              });
              updateValidationMessages(element, attrs, ctrl);
            });

            angular.forEach(['$dirty', '$valid', '$invalid'], function(propName) {
              scope.$watch(form.$name + '.' + ctrl.$name + '.' + propName, function() {
                if (ctrl.$dirty) {
                  updateValidationMessages(element, attrs, ctrl);
                }
              });
            });
          }
        };
      }
  )
  /**
   * Template for common structure of form inputs and associated labels.
   */
      .directive('clmControlGroup', function() {
        return {
          restrict: 'A',
          require: '^form',
          replace: true,
          transclude: true,
          scope: {
            for: '@',
            label: '@',
            form: '@'
          },
          template: '<div class="control-group">' +
              '<label class="control-label" for="{{for}}">{{label}}</label>' +
              '<div class="controls">' +
              '<div ng-transclude></div>' +
              '</div>' +
              '</div>'
        };
      });

}());