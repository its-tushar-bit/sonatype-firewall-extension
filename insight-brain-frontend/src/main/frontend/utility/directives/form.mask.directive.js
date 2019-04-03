/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function FormMaskDirective($parse) {
  return {
    restrict: 'A',
    controller: FormMaskController,
    require: 'formMask',
    link: FormMaskLink
  };

  function FormMaskLink(scope, element, attrs, maskController) {
    var maskElement,
        maskMessage = 'Saving',
        // if maskAttachToBody flag is set the mask will cover the whole page
        attachToBody = attrs.hasOwnProperty('maskAttachToBody');

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
  maskController.showSuccessMaskBriefly = showSuccessMaskBriefly;

  function wrap(promise) {
    maskController.activateMask();

    return promise.then(function() {
      var args = arguments;

      if (!skipSuccess) {
        maskController.showSuccessMask();
      }

      return waitAndRemove(args);
    }, function() {
      maskController.removeMask();
      return $q.reject.apply($q, arguments);
    });
  }

  // show the success mask for 800 ms and then remove it.  Returns a promise for when this is complete
  function showSuccessMaskBriefly() {
    maskController.showSuccessMask();

    return waitAndRemove(arguments);
  }

  function waitAndRemove(resolutionArgs) {
    return $q(function(resolve) {
      $timeout(function() {
        maskController.removeMask();
        resolve.apply(null, resolutionArgs);
      }, 800);
    });
  }
}

FormMaskController.$inject = ['$q', '$timeout', '$attrs'];
