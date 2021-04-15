/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('evaluate.application.modal.controller.spec.js', function () {
  var scope, vm, $timeout, $httpBackend, CLMLocations, mockSelectedApplication;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (
    $rootScope,
    $q,
    $controller,
    _$timeout_,
    _$httpBackend_,
    _CLMLocations_,
    StageTypeStore
  ) {
    var stageTypeStoreDefer = $q.defer();

    scope = $rootScope.$new();
    scope.$dismiss = jasmine.createSpy('$dismiss').and.returnValue(undefined);

    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;

    mockSelectedApplication = {
      publicId: '1234567890',
      name: 'test app',
    };

    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'get').and.returnValue(stageTypeStoreDefer.promise);
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);

    vm = $controller('evaluate.application.modal.controller', {
      $scope: scope,
      selectedApplication: mockSelectedApplication,
    });

    expect(vm.evaluationState).toBe('loading');
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    $httpBackend.flush();
  }));

  it('Loads bundle and stages properly', function () {
    expect(vm.evaluationState).toBe('ready');
    expect(vm.stages).toEqual(MockData.getDashboardStageData());
    expect(vm.bundle.applicationPublicId).toEqual(
      mockSelectedApplication.publicId
    );
    expect(vm.bundle.applicationName).toEqual(mockSelectedApplication.name);
    expect(vm.bundle.stage).toBeUndefined();
    expect(vm.bundle.notify).toEqual('true');
    expect(vm.error).toBeFalsy();
  });

  it('Compiles bundle url with expected values', inject(function (
    CLMLocations
  ) {
    spyOn(CLMLocations, 'getBundleUploadUrl').and.returnValue(true);

    vm.bundle.stage = vm.stages[2];
    vm.bundle.file = '/test/test/test.war';
    vm.uploadBundleUrl();

    expect(CLMLocations.getBundleUploadUrl).toHaveBeenCalledWith(
      mockSelectedApplication.publicId,
      'release',
      'true'
    );
  }));

  it('Test Form validation', function () {
    vm.bundle.file = 'testfile';
    expect(vm.isFormValid()).toBeFalsy();

    vm.bundle.stage = vm.stages[2];
    expect(vm.isFormValid()).toBeTruthy();
  });

  it('dismisses on navigating away', inject(function ($rootScope) {
    $rootScope.$broadcast('pageChangeAccepted');
    expect(scope.$dismiss).toHaveBeenCalled();
  }));

  describe('Bundle submit', function () {
    var appendSpy, originalFormData;
    beforeEach(inject(function ($window) {
      originalFormData = $window.FormData;
      $window.FormData = function () {
        this.append = jasmine.createSpy();
        appendSpy = this.append;
      };
      var original = angular.element;
      spyOn(angular, 'element').and.callFake(function (selector) {
        if (selector === '#bundle-file') {
          return [
            {
              files: [
                {
                  name: 'testfile',
                },
              ],
              value: 'testfile',
            },
          ];
        }
        return original(selector);
      });
      angular.element.cleanData = original.cleanData;

      vm.bundle.file = 'testfile';
      vm.bundle.stage = vm.stages[2];
    }));

    afterEach(inject(function ($window) {
      $window.FormData = originalFormData;
    }));

    function validateInitialState() {
      expect(vm.evaluationState).toEqual('polling');
      expect(vm.evaluationStatus.currentStep).toEqual(1);
      expect(vm.evaluationStatus.totalSteps).toEqual(1);
      expect(vm.evaluationStatus.currentStepName).toEqual('Uploading');
      expect(vm.error).toBeNull();
      expect(vm.bundle.filename).toEqual('testfile');
      expect(vm.bundle.applicationName).toEqual(mockSelectedApplication.name);
      expect(vm.pollingUrl).toBeNull();
    }

    it('Test submit failure', function () {
      $httpBackend
        .expectPOST(
          CLMLocations.getBundleUploadUrl(
            mockSelectedApplication.publicId,
            'release',
            'true'
          )
        )
        .respond(500, 'Some failure');

      vm.doSubmit();
      validateInitialState();
      $httpBackend.flush();

      expect(vm.evaluationState).toEqual('polling');
      expect(vm.error).toEqual('Some failure');
    });

    it('Test submit success', function () {
      vm.bundle.notify = 'false';
      $httpBackend
        .expectPOST(
          CLMLocations.getBundleUploadUrl(
            mockSelectedApplication.publicId,
            'release',
            'false'
          )
        )
        .respond({
          ticketId: 'ticket',
        });

      vm.doSubmit();
      expect(appendSpy).toHaveBeenCalledWith('filename', 'testfile');
      validateInitialState();

      $httpBackend
        .expectGET(
          CLMLocations.getEvaluationStatusUrl(
            mockSelectedApplication.publicId,
            'ticket'
          )
        )
        .respond({
          ticketId: 'ticket',
          scanId: 'scanId',
          currentStep: 1,
          totalSteps: 1,
        });

      $httpBackend.flush();
      expect(vm.pollingUrl).toEqual(
        CLMLocations.getEvaluationStatusUrl(
          mockSelectedApplication.publicId,
          'ticket'
        )
      );
    });

    it('Test evaluation polling loop', function () {
      $httpBackend
        .expectPOST(
          CLMLocations.getBundleUploadUrl(
            mockSelectedApplication.publicId,
            'release',
            'true'
          )
        )
        .respond({
          ticketId: 'ticket',
        });

      vm.doSubmit();
      validateInitialState();
      $httpBackend
        .expectGET(
          CLMLocations.getEvaluationStatusUrl(
            mockSelectedApplication.publicId,
            'ticket'
          )
        )
        .respond({
          ticketId: 'ticket',
          currentStep: 1,
          totalSteps: 2,
        });

      $httpBackend.flush();

      $httpBackend
        .expectGET(
          CLMLocations.getEvaluationStatusUrl(
            mockSelectedApplication.publicId,
            'ticket'
          )
        )
        .respond({
          ticketId: 'ticket',
          scanId: 'scanId',
          currentStep: 2,
          totalSteps: 2,
        });

      $timeout.flush();
      $httpBackend.flush();
      expect(vm.pollingUrl).toEqual(
        CLMLocations.getEvaluationStatusUrl(
          mockSelectedApplication.publicId,
          'ticket'
        )
      );
    });

    it('Test evaluation error', function () {
      $httpBackend
        .expectPOST(
          CLMLocations.getBundleUploadUrl(
            mockSelectedApplication.publicId,
            'release',
            'true'
          )
        )
        .respond({
          ticketId: 'ticket',
        });

      vm.doSubmit();
      validateInitialState();
      $httpBackend
        .expectGET(
          CLMLocations.getEvaluationStatusUrl(
            mockSelectedApplication.publicId,
            'ticket'
          )
        )
        .respond({
          ticketId: 'ticket',
          currentStep: 1,
          totalSteps: 2,
          error: 'something aint right',
        });

      $timeout.flush();
      $httpBackend.flush();
      expect(vm.pollingUrl).toEqual(
        CLMLocations.getEvaluationStatusUrl(
          mockSelectedApplication.publicId,
          'ticket'
        )
      );
      expect(vm.error).toEqual('something aint right');
    });
  });
});
