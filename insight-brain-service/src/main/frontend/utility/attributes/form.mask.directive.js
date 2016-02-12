(function(angular) {
  'use strict';

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
        throw('Form Mask requires a name to bind the controller.');
      }

      $parse(attrs.formMask).assign(scope, maskController);

      attrs.$observe('maskMessage', function(newMessage) {
        maskMessage = newMessage || 'Saving';
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
              offset,
              targetElement = attachToBody ? $('body') : element;

          // open mask
          maskElement = $('<div class="form-mask"><div class="form-mask-msg"><h3><i class="fa fa-circle-o-notch fa-spin"></i> ' +
              maskMessage + '</h3></div></div>');

          //note we are tweaking the size a slight bit to not overrun the margin of the form
          offset = targetElement.offset();
          maskElement.css('top', offset.top + 1).css('left', offset.left + 1).css('width',
              targetElement.width() - 2).css('height', targetElement.height() - 2);
          targetElement.append(maskElement);

          // prior to adding to the DOM the element has no height
          msgElement = $('.form-mask-msg', element);
          msgElement.css('margin-top', -msgElement.outerHeight());
        }
      }

      function showSuccessMask() {
        var msgElement = $('.form-mask-msg', attachToBody ? $('body') : element);
        msgElement.addClass('success');
        msgElement.html('<h3><i class="fa fa-check-circle"></i> Success!</h3>');
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

  angular.module('utility').directive('formMask', FormMaskDirective);

}(angular));
