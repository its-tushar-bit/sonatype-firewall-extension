import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipClaimComponent', function() {
  let createController,
      $scope;

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function($componentController, $rootScope) {
    $scope = $rootScope.$new();
    createController = (component, reloadReport) => {
      const controller = $componentController('cipClaimComponent', { $scope }, {component, reloadReport});
      controller.claimForm = {
        $setPristine: jasmine.createSpy('$setPristine')
      };
      return controller;
    };
  }));

  describe('$onInit()', function() {
    let controller;

    beforeEach(function() {
      controller = createController('foo');
    });

    it('calls resets form', function() {
      spyOn(controller, 'resetForm');
      controller.$onInit();
      $scope.$digest();
      expect(controller.resetForm).toHaveBeenCalled();
    });

    it('initializes datepicker', function() {
      controller.$onInit();
      $scope.$digest();
      expect(controller.datePickerElement.datepicker).toBeDefined();
    });

    it('sets vm.component watcher to reset form when component changes', function() {
      spyOn(controller, 'resetForm');
      controller.$onInit();
      // init the watcher
      $scope.$digest();
      expect(controller.resetForm.calls.count()).toBe(1);
      controller.component = 'bar';
      $scope.$digest();
      expect(controller.resetForm.calls.count()).toBe(2);
    });
  });

  describe('resetForm()', function() {
    let component;
    beforeEach(function() {
      component = {
        componentIdentifier: {
          coordinates: {
            groupId: 'testGroupId',
            artifactId: 'testArtifactId',
            version: 'testVersion',
            classifier: 'testClassifier',
            extension: 'testExtension'
          }
        },
        comment: 'testComment',
        createTime: 1544418000000
      };
    });

    describe('when component is claimed', function() {
      let controller;
      beforeEach(function() {
        component.identificationSource = 'Manual';
        controller = createController(component);
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and populates with component info', function() {
        controller.resetForm();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          groupId: 'testGroupId',
          artifactId: 'testArtifactId',
          version: 'testVersion',
          classifier: 'testClassifier',
          extension: 'testExtension',
          comment: 'testComment',
          createTimeText: '12/10/2018'
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', new Date(1544418000000));
      });
    });

    describe('when component is not claimed and has createTime', function() {
      let controller;
      beforeEach(function() {
        controller = createController(component);
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and populates createTime from component info', function() {
        controller.resetForm();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          createTimeText: '12/10/2018'
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', new Date(1544418000000));
      });
    });

    describe('when component is not claimed and has no createTime', function() {
      let controller;
      beforeEach(function() {
        component.createTime = null;
        controller = createController(component);
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and sets blank createTime', function() {
        controller.resetForm();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          createTimeText: null
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', '');
      });
    });
  });

  describe('submit methods', function() {
    let $httpBackend, CLMLocations, $q, controller, reloadReport, reloadReportResult, expectedPayload;

    beforeEach(inject(function(_$httpBackend_, _CLMLocations_, _$q_) {
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;
      $q = _$q_;

      const component = {
        hash: 'c2d6a87d5c2bcd383900'
      };
      expectedPayload = {
        hash: 'c2d6a87d5c2bcd383900',
        componentIdentifier: {
          format: 'maven',
          coordinates: {
            groupId: 'testGroupId',
            artifactId: 'testArtifactId',
            version: 'testVersion',
            extension: 'testExtension',
            classifier: 'testClassifier'
          }
        },
        createTime: new Date('12/10/2018').getTime(),
        comment: 'testComment'
      };
      reloadReportResult = $q.defer();
      reloadReport = jasmine.createSpy('reloadReport').and.returnValue(reloadReportResult.promise);
      controller = createController(component, reloadReport);
      controller.claimData = {
        groupId: 'testGroupId',
        artifactId: 'testArtifactId',
        version: 'testVersion',
        classifier: 'testClassifier',
        extension: 'testExtension',
        comment: 'testComment',
        createTimeText: '12/10/2018'
      };
    }));

    afterEach(function() {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    describe('claimComponent()', function() {
      it('does not submit if claimForm is invalid', function() {
        controller.claimForm.$valid = false;
        controller.claimComponent();
        expect(controller.loading).toBe(false);
        expect(reloadReport).not.toHaveBeenCalled();
        expect(controller.error).toBeFalsy();
      });

      it('submits claim if claimForm is valid', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPOST(urlRegex, expectedPayload).respond(200);
        controller.claimComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.resolve();
        $scope.$digest();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
      });

      it('handles submit error', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPOST(urlRegex, expectedPayload).respond(500, 'test error');
        controller.claimComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(reloadReport).not.toHaveBeenCalled();
        expect(controller.error).toBe('test error');
      });

      it('handles reloadReport error', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPOST(urlRegex, expectedPayload).respond(200);
        controller.claimComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.reject();
        $scope.$digest();
        expect(controller.error).toBeFalsy();
      });
    });

    describe('updateComponent()', function() {
      it('does not submit if claimForm is invalid', function() {
        controller.claimForm.$valid = false;
        controller.updateComponent();
        expect(controller.loading).toBe(false);
        expect(reloadReport).not.toHaveBeenCalled();
        expect(controller.error).toBeFalsy();
      });

      it('submits claim if claimForm is valid', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPUT(urlRegex, expectedPayload).respond(200);
        controller.updateComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.resolve();
        $scope.$digest();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
      });

      it('handles submit error', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPUT(urlRegex, expectedPayload).respond(500, 'test error');
        controller.updateComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(reloadReport).not.toHaveBeenCalled();
        expect(controller.error).toBe('test error');
      });

      it('handles reloadReport error', function() {
        controller.claimForm.$valid = true;
        const urlRegex = SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl());
        $httpBackend.expectPUT(urlRegex, expectedPayload).respond(200);
        controller.updateComponent();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.reject();
        $scope.$digest();
        expect(controller.error).toBeFalsy();
      });
    });

    describe('revokeClaim()', function() {
      let url;
      beforeEach(function() {
        url = CLMLocations.getClaimComponentUrl() + '/c2d6a87d5c2bcd383900';
      });

      it('submits even if claimForm is invalid', function() {
        controller.claimForm.$valid = false;
        $httpBackend.expectDELETE(SpecUtil.toRegExp(url)).respond(200);
        controller.revokeClaim();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.resolve();
        $scope.$digest();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
      });

      it('handles submit error', function() {
        controller.claimForm.$valid = true;
        $httpBackend.expectDELETE(SpecUtil.toRegExp(url)).respond(500, 'test error');
        controller.revokeClaim();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(reloadReport).not.toHaveBeenCalled();
        expect(controller.error).toBe('test error');
      });

      it('handles reloadReport error', function() {
        controller.claimForm.$valid = true;
        $httpBackend.expectDELETE(SpecUtil.toRegExp(url)).respond(200);
        controller.revokeClaim();
        expect(controller.loading).toBe(true);
        $httpBackend.flush();
        expect(reloadReport).toHaveBeenCalled();
        expect(controller.loading).toBe(true);
        reloadReportResult.reject();
        $scope.$digest();
        expect(controller.error).toBeFalsy();
      });
    });
  });
});
