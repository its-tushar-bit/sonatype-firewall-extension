/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function FormMaskDirective($parse) {
  return {
    restrict: 'A',
    controller: FormMaskController,
    require: 'formMask',
    link: FormMaskLink
  };

  function FormMaskLink(scope, element, attrs, maskController) {
    var maskElement,
        maskMessage = 'Saving',
        attachToBody = attrs.hasOwnProperty('maskAttachToBody'); //if maskAttachToBody flag is set the mask will cover the whole page

    maskController.removeMask = removeMask;
    maskController.activateMask = activateMask;
    maskController.showSuccessMask = showSuccessMask;

    if (!attrs.formMask.length) {
      throw ('Form Mask requires a name to bind the controller.');
    }

    $parse(attrs.formMask).assign(scope, maskController);

    attrs.$observe('maskMessage', function(newMessage) {
      maskMessage = (newMessage != null ? newMessage : 'Saving');
    });

    scope.$on('$destroy', removeMask);

    function removeMask() {
      if (maskElement) {
        maskElement.remove();
        maskElement = null;
      }
    }

    function activateMask() {
      if (!maskElement) {
        var msgElement,
            targetElement = attachToBody ? $('body') : element;

        // open mask
        maskElement = $('<div class="form-mask"/>');

        if (maskMessage) {
          msgElement = $('<div class="form-mask-msg"><h3><i class="fa fa-circle-o-notch fa-spin"></i> ' +
            maskMessage + '</h3></div>');

          maskElement.append(msgElement);
        }

        targetElement.append(maskElement);

        if (msgElement) {
          // prior to adding to the DOM the element has no height
          msgElement.css('margin-top', -msgElement.outerHeight());
        }
      }
    }

    function showSuccessMask() {
      var msgElement = $('.form-mask-msg', attachToBody ? $('body') : element);

      if (msgElement) {
        msgElement.addClass('success');
        msgElement.html('<h3><i class="fa fa-check-circle"></i> Success!</h3>');
      }
    }
  }
}

FormMaskDirective.$inject = ['$parse'];

function FormMaskController($q, $timeout, $attrs) {
  var maskController = this,
      skipSuccess = $attrs.hasOwnProperty('maskSkipSuccess');

  maskController.wrap = wrap;

  function wrap(promise) {
    var deferred = $q.defer();
    maskController.activateMask();

    promise.then(function() {
      var args = arguments;

      if (!skipSuccess) {
        maskController.showSuccessMask();
      }

      $timeout(function() {
        maskController.removeMask();
        deferred.resolve.apply(deferred, args);
      }, 800);
    }, function() {
      maskController.removeMask();
      deferred.reject.apply(deferred, arguments);
    });

    return deferred.promise;
  }
}

FormMaskController.$inject = ['$q', '$timeout', '$attrs'];

angular.module('utility.directives').directive('formMask', FormMaskDirective);
