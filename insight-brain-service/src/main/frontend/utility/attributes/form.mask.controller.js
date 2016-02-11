(function(angular) {
  'use strict';

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

  angular.module('utility').controller('form.mask.controller', FormMaskController);

}(angular));
