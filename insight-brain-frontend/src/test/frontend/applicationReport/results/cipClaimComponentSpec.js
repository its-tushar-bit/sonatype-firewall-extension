/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import cipModalModule from '../../../../main/frontend/applicationReport/results/cipModal/module';

describe('cipClaimComponent', function () {
  let createController, $scope, $httpBackend, CLMLocations;

  beforeEach(angular.mock.module(cipModalModule.name));

  beforeEach(inject(function ($componentController, $rootScope, _$httpBackend_, _CLMLocations_) {
    $scope = $rootScope.$new();
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    createController = (component) => {
      const controller = $componentController('cipClaimComponent', { $scope }, { component });
      controller.claimForm = {
        $setPristine: jasmine.createSpy('$setPristine'),
      };
      return controller;
    };
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  /**
   * A collection of tests for an action in the Claim tab that should result in a backend call with standard
   * handling of the loading flag, error property, and form resetting
   * @param makeBackendExpectation A function which should call $httpBackend.expect... and return the result. This
   * function should NOT specify the expected response
   * @param doAction A function that does the thing in the claim tab which we are testing
   */
  function testServerAction(getController, makeBackendExpectation, doAction) {
    describe('server response handling', function () {
      const serverResponseData = { a: 1 };

      let controller;

      beforeEach(function () {
        controller = getController();
        spyOn(controller, 'setServerData');
      });

      it('handles loading and form reset on success', function () {
        controller.loading = false;
        controller.error = {};

        makeBackendExpectation().respond(200, serverResponseData);

        doAction();

        expect(controller.loading).toBe(true);
        expect(controller.error).toBeFalsy();
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
        expect(controller.setServerData).toHaveBeenCalledWith(serverResponseData);
      });

      it('sets error and does not call setServerData on error other than 404', function () {
        controller.loading = false;
        controller.error = {};

        makeBackendExpectation().respond(500, 'test error');

        doAction();

        expect(controller.loading).toBe(true);
        expect(controller.error).toBeFalsy();
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBe('test error');
        expect(controller.setServerData).not.toHaveBeenCalled();
      });

      it('calls setServerData with no parameter when the server returns a 404', function () {
        controller.loading = false;
        controller.error = {};

        makeBackendExpectation().respond(404);

        doAction();

        expect(controller.loading).toBe(true);
        expect(controller.error).toBeFalsy();
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
        expect(controller.setServerData).toHaveBeenCalledWith(); // no args
      });
    });
  }

  describe('$onInit()', function () {
    let controller;

    beforeEach(function () {
      controller = createController({ hash: 'foo' });
    });

    it('initializes datepicker', function () {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl('foo'))).respond(200, {});

      controller.$onInit();
      $scope.$digest();
      $httpBackend.flush();

      expect(controller.datePickerElement.datepicker).toBeDefined();
    });

    testServerAction(
      () => controller,
      () => $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl('foo'))),
      () => {
        controller.$onInit();
        $scope.$digest();
      }
    );
  });

  describe('when vm.component changes', function () {
    let controller;

    beforeEach(function () {
      controller = createController({ hash: 'foo' });
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl('foo'))).respond(200, {});
      controller.$onInit();

      $httpBackend.flush();
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    testServerAction(
      () => controller,
      () => $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl('bar'))),
      () => {
        controller.component = { hash: 'bar' };
        $scope.$digest();
      }
    );
  });

  describe('resetFormFromServerData', function () {
    const component = {
        hash: '1234',
        createTime: new Date('1/1/1970').getTime(),
      },
      serverClaimData = {
        componentIdentifier: {
          coordinates: {
            groupId: 'testGroupId',
            artifactId: 'testArtifactId',
            version: 'testVersion',
            classifier: 'testClassifier',
            extension: 'testExtension',
          },
        },
        comment: 'testComment',
        createTime: new Date('12/10/2018').getTime(),
      };

    describe('when component is claimed', function () {
      let controller;
      beforeEach(function () {
        controller = createController(component);
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and populates with component info', function () {
        controller.serverClaimData = serverClaimData;
        controller.resetFormFromServerData();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          groupId: 'testGroupId',
          artifactId: 'testArtifactId',
          version: 'testVersion',
          classifier: 'testClassifier',
          extension: 'testExtension',
          comment: 'testComment',
          createTimeText: '12/10/2018',
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', new Date('12/10/2018'));
      });
    });

    describe('when component is not claimed and has createTime', function () {
      let controller;
      beforeEach(function () {
        controller = createController(component);
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and populates createTime from component info', function () {
        controller.serverClaimData = undefined;
        controller.resetFormFromServerData();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          createTimeText: '01/01/1970',
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', new Date('1/1/1970'));
      });
    });

    describe('when component is not claimed and has no createTime', function () {
      let controller;
      beforeEach(function () {
        controller = createController(omit(['createTime'], component));
        controller.datePickerElement = jasmine.createSpyObj('datePickerElement', ['datepicker']);
      });

      it('resets form and sets blank createTime', function () {
        controller.serverClaimData = undefined;
        controller.resetFormFromServerData();
        expect(controller.claimForm.$setPristine).toHaveBeenCalled();
        expect(controller.claimData).toEqual({
          createTimeText: null,
        });
        expect(controller.datePickerElement.datepicker).toHaveBeenCalledWith('update', '');
      });
    });
  });

  describe('setServerData', function () {
    const component = { hash: '1234' },
      serverClaimData = { a: 1 };

    let controller;

    beforeEach(function () {
      controller = createController(component);
    });

    it('sets vm.serverClaimData from its argument', function () {
      expect(controller.serverClaimData).toBe(undefined);
      spyOn(controller, 'resetFormFromServerData');

      controller.setServerData(serverClaimData);
      expect(controller.serverClaimData).toBe(serverClaimData);
    });

    it('calls vm.resetFormFromServerData after setting the server data', function () {
      let serverClaimDataWhenResetFormFromServerDataCalled;
      spyOn(controller, 'resetFormFromServerData').and.callFake(function () {
        serverClaimDataWhenResetFormFromServerDataCalled = this.serverClaimData;
      });

      controller.setServerData(serverClaimData);
      expect(serverClaimDataWhenResetFormFromServerDataCalled).toBe(serverClaimData);
    });
  });

  describe('submit methods', function () {
    let $httpBackend, CLMLocations, controller, expectedPayload;

    beforeEach(inject(function (_$httpBackend_, _CLMLocations_) {
      $httpBackend = _$httpBackend_;
      CLMLocations = _CLMLocations_;

      const component = {
        hash: 'c2d6a87d5c2bcd383900',
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
            classifier: 'testClassifier',
          },
        },
        createTime: new Date('12/10/2018').getTime(),
        comment: 'testComment',
      };
      controller = createController(component);
      controller.claimData = {
        groupId: 'testGroupId',
        artifactId: 'testArtifactId',
        version: 'testVersion',
        classifier: 'testClassifier',
        extension: 'testExtension',
        comment: 'testComment',
        createTimeText: '12/10/2018',
      };
    }));

    describe('claimComponent()', function () {
      it('does not submit if claimForm is invalid', function () {
        controller.claimForm.$valid = false;
        controller.claimComponent();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
      });

      describe('when claimForm is valid', function () {
        beforeEach(function () {
          controller.claimForm.$valid = true;
        });

        testServerAction(
          () => controller,
          () => $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl()), expectedPayload),
          () => controller.claimComponent()
        );
      });
    });

    describe('updateComponent()', function () {
      it('does not submit if claimForm is invalid', function () {
        controller.claimForm.$valid = false;
        controller.updateComponent();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
      });

      describe('when claimForm is valid', function () {
        beforeEach(function () {
          controller.claimForm.$valid = true;
        });

        testServerAction(
          () => controller,
          () => $httpBackend.expectPUT(SpecUtil.toRegExp(CLMLocations.getClaimComponentUrl()), expectedPayload),
          () => controller.updateComponent()
        );
      });
    });

    describe('revokeClaim()', function () {
      let url;
      beforeEach(function () {
        url = CLMLocations.getClaimComponentUrl('c2d6a87d5c2bcd383900');
      });

      describe('when the claimForm is invalid', function () {
        beforeEach(function () {
          controller.claimForm.$valid = false;
        });

        testServerAction(
          () => controller,
          () => $httpBackend.expectDELETE(SpecUtil.toRegExp(url)),
          () => controller.revokeClaim()
        );
      });

      describe('when the claimForm is valid', function () {
        beforeEach(function () {
          controller.claimForm.$valid = true;
        });

        // same as when the claimForm is invalid; revoke isn't affected
        testServerAction(
          () => controller,
          () => $httpBackend.expectDELETE(SpecUtil.toRegExp(url)),
          () => controller.revokeClaim()
        );
      });

      it('calls setServerData with undefined when the server returns a 204', function () {
        controller.loading = false;
        controller.error = {};

        spyOn(controller, 'setServerData');
        $httpBackend.expectDELETE(SpecUtil.toRegExp(url)).respond(204);

        controller.revokeClaim();

        expect(controller.loading).toBe(true);
        expect(controller.error).toBeFalsy();
        $httpBackend.flush();
        expect(controller.loading).toBe(false);
        expect(controller.error).toBeFalsy();
        expect(controller.setServerData).toHaveBeenCalledWith(undefined);
      });
    });
  });

  describe('isClaimedComponent', function () {
    let controller;

    beforeEach(function () {
      controller = createController();
      spyOn(controller, 'setServerData');
    });

    it('returns true if controller.serverClaimData is set', function () {
      controller.serverClaimData = {};
      expect(controller.isClaimedComponent()).toBe(true);
    });

    it('returns false if controller.serverClaimData is not set', function () {
      controller.serverClaimData = undefined;
      expect(controller.isClaimedComponent()).toBe(false);
    });
  });
});
