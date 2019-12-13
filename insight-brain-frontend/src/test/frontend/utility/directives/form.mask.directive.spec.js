/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityDirectivesModule from '../../../../main/frontend/utility/directives/utility.directives.module';

describe('form.mask.directive.spec.js', function() {

  beforeEach(angular.mock.module(utilityDirectivesModule.name));

  describe('attached to Element', function() {
    maskTests('<div form-mask="formMask"><div>', false);
  });

  describe('attached to Body', function() {
    maskTests('<div form-mask="formMask" mask-attach-to-body><div>', true);
  });

  function maskTests(elementString, attachToBody) {
    var element,
        scope,
        maskController;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      element = $compile(elementString)(scope);

      expect(scope.formMask).toBeDefined();
      maskController = scope.formMask;
    }));

    afterEach(function() {
      scope.$destroy();
    });

    it('Directive properly activates and removes mask', function() {
      expect(getFormMaskElement().length).toEqual(0);
      maskController.activateMask();
      expect(getFormMaskElement().length).toEqual(1);
      maskController.removeMask();
      expect(getFormMaskElement().length).toEqual(0);
    });

    it('Directive properly shows success mask', function() {
      maskController.activateMask();
      expect(getFormMaskElement().find('h3').text()).toEqual(' Saving');
      maskController.showSuccessMask();
      expect(getFormMaskElement().find('h3').text()).toEqual(' Success!');
    });

    it('Directive properly wraps promise with mask', inject(function($q, $timeout) {
      var deferred = $q.defer(),
          results;

      spyOn(maskController, 'activateMask');
      spyOn(maskController, 'showSuccessMask');
      spyOn(maskController, 'removeMask');
      spyOn(deferred.promise, 'then').and.callThrough();

      maskController.wrap(deferred.promise).then(function(data) {
        results = data;
      });

      expect(maskController.activateMask).toHaveBeenCalled();
      expect(deferred.promise.then).toHaveBeenCalled();

      deferred.resolve({data: 123});
      $timeout.flush();
      expect(maskController.showSuccessMask).toHaveBeenCalled();

      $timeout.flush(800);
      expect(maskController.removeMask).toHaveBeenCalled();
      expect(results).toEqual({data: 123});
    }));

    it('Directive properly returns failed arguments from wrap', inject(function($q, $timeout) {
      var deferred = $q.defer(),
          results;

      spyOn(maskController, 'activateMask');
      spyOn(maskController, 'removeMask');
      spyOn(deferred.promise, 'then').and.callThrough();

      maskController.wrap(deferred.promise).then(angular.noop, function(data) {
        results = data;
      });

      expect(maskController.activateMask).toHaveBeenCalled();
      expect(deferred.promise.then).toHaveBeenCalled();

      deferred.reject({data: 123});
      $timeout.flush();
      expect(maskController.removeMask).toHaveBeenCalled();
      expect(results).toEqual({data: 123});
    }));

    it('Directive properly skips success mask', inject(function($compile, $q, $timeout) {
      $compile(element.attr('mask-skip-success', ''))(scope);

      expect(scope.formMask).toBeDefined();
      var maskController = scope.formMask,
          deferred = $q.defer();

      spyOn(maskController, 'activateMask');
      spyOn(maskController, 'showSuccessMask');
      spyOn(maskController, 'removeMask');
      spyOn(deferred.promise, 'then').and.callThrough();

      maskController.wrap(deferred.promise);

      expect(maskController.activateMask).toHaveBeenCalled();
      expect(deferred.promise.then).toHaveBeenCalled();

      deferred.resolve({data: 123});
      $timeout.flush();
      $timeout.flush(800);
      expect(maskController.removeMask).toHaveBeenCalled();
      expect(maskController.showSuccessMask).not.toHaveBeenCalled();
    }));

    describe('showSuccessMaskBriefly', function() {
      var $timeout;

      beforeEach(inject(function(_$timeout_) {
        $timeout = _$timeout_;
      }));

      it('displays the success mask and then removes it after a timeout', function() {
        maskController.activateMask();
        maskController.showSuccessMaskBriefly();
        expect(getFormMaskElement().find('h3').text()).toEqual(' Success!');

        $timeout.flush();

        expect(getFormMaskElement().length).toEqual(0);
      });

      it('resolves the promise with the specified argument after the timeout', function() {
        var resolveSpy = jasmine.createSpy('resolve');

        maskController.activateMask();
        maskController.showSuccessMaskBriefly(1).then(resolveSpy);
        expect(resolveSpy).not.toHaveBeenCalled();

        $timeout.flush();

        expect(resolveSpy).toHaveBeenCalledWith(1);
      });
    });

    function getFormMaskElement() {
      return attachToBody ? angular.element($('div.form-mask', $('body'))) : element.find('div.form-mask');
    }
  }
});
