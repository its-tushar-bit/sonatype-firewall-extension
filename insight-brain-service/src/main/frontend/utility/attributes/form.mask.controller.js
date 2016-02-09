(function(angular) {
  'use strict';

  function FormMaskController($q, $timeout) {
    var maskController = this;

    maskController.wrap = wrap;

    function wrap(promise) {
      var deferred = $q.defer();
      maskController.activateMask();

      promise.then(function() {
        var args = arguments;

        maskController.showSuccessMask();

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

  FormMaskController.$inject = ['$q', '$timeout'];

  angular.module('utility').controller('form.mask.controller', FormMaskController);

}(angular));
