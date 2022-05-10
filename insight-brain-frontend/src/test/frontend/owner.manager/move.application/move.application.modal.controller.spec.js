/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';
import { actions as applicationActions } from 'MainRoot/OrgsAndPolicies/applicationsSlice';

describe('move.application.modal.controller', function () {
  var mockDestinations = [
    {
      id: '1',
      parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      name: 'Org 3',
      nameLowercaseNoWhitespace: 'org3',
    },
    {
      id: '2',
      parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      name: 'Test Organization',
      nameLowercaseNoWhitespace: 'testorganization',
    },
  ];

  var $q,
    initController,
    scope,
    $rootScope,
    destinationsPromise,
    moveApplicationErrorModal,
    moveApplicationSuccessModal,
    moveApplicationActionSpy;

  var moveApplicationServiceMock = {
    getDestinationOrganizations: function () {
      return destinationsPromise;
    },
  };

  beforeEach(
    angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$rootScope_, $controller, _$q_) {
    $rootScope = _$rootScope_;
    $q = _$q_;
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('$close');
    moveApplicationErrorModal = jasmine.createSpyObj('errorModal', ['open']);
    moveApplicationSuccessModal = jasmine.createSpyObj('successModal', ['open']);
    moveApplicationActionSpy = spyOn(applicationActions, 'moveApplication');
    initController = function () {
      return $controller('move.application.modal.controller', {
        $scope: scope,
        $state: jasmine.createSpyObj('$state', ['go']),
        currentApplication: { id: 1 },
        'move.application.error.modal.service': moveApplicationErrorModal,
        'move.application.success.modal.service': moveApplicationSuccessModal,
        'move.application.service': moveApplicationServiceMock,
      });
    };
  }));

  describe('initialization', function () {
    describe('on component init', () => {
      it('subscribes to the redux store', () => {
        destinationsPromise = $q.resolve(mockDestinations);

        const vm = initController();

        expect(vm.unsubscribe).toBeDefined();
      });
    });

    describe('on $destroy()', () => {
      it('unsubscribes from redux store', () => {
        destinationsPromise = $q.resolve(mockDestinations);

        const vm = initController();
        expect(vm.unsubscribe).not.toHaveBeenCalled();

        scope.$destroy();
        expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
      });
    });

    describe('event handlers', function () {
      let vm;

      beforeEach(function () {
        destinationsPromise = $q.resolve(mockDestinations);
        vm = initController();
      });

      describe('pageChangeAccepted event', function () {
        it('calls $scope.$dismiss()', function () {
          scope.$dismiss = jasmine.createSpy('$dismiss');
          $rootScope.$broadcast('pageChangeAccepted');
          expect(scope.$dismiss).toHaveBeenCalled();
        });
      });

      describe('$destroy event', function () {
        it('unsubscribes from ngRedux', function () {
          scope.$destroy();
          expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
        });
      });
    });

    describe('when destinations exist', function () {
      var vm;
      beforeEach(function () {
        destinationsPromise = $q.resolve(mockDestinations);
        vm = initController();
      });

      it('assigns vm.organizations to loaded destinations', function () {
        expect(vm.organizations).toBeUndefined();
        scope.$apply(); // resolve promises
        expect(vm.organizations).toBe(mockDestinations);
      });

      describe('vm.isLoading()', function () {
        it('returns true until destinations are loaded', function () {
          expect(vm.isLoading()).toBeTruthy();
          scope.$apply(); // resolve promises
          expect(vm.isLoading()).toBeFalsy();
        });
      });

      describe('vm.getError()', function () {
        it('returns undefined', function () {
          scope.$apply(); // resolve promises
          expect(vm.getError()).toBeUndefined();
        });
      });
    });

    describe('when error getting destinations', function () {
      var vm;
      beforeEach(function () {
        destinationsPromise = $q.reject('get destinations error');
        vm = initController();
      });

      it('does not set vm.organizations', function () {
        expect(vm.organizations).toBeUndefined();
        scope.$apply(); // resolve promises
        expect(vm.organizations).toBeUndefined();
      });

      it('assigns vm.loadError to resulting error', function () {
        expect(vm.loadError).toBeUndefined();
        scope.$apply(); // resolve promises
        expect(vm.loadError).toEqual('get destinations error');
      });

      describe('vm.getError()', function () {
        it('returns vm.loadError value', function () {
          scope.$apply(); // resolve promises
          expect(vm.getError()).toEqual('get destinations error');
        });
      });

      describe('vm.isLoading()', function () {
        it('returns true until error is received', function () {
          expect(vm.isLoading()).toBeTruthy();
          scope.$apply(); // resolve promises
          expect(vm.isLoading()).toBeFalsy();
        });
      });
    });
  });

  describe('vm.save()', function () {
    var vm;
    beforeEach(function () {
      destinationsPromise = $q.resolve(mockDestinations);
      vm = initController();
      vm.selectedOrganization = { id: 1, name: 'destination org' };
      vm.formMask = {
        wrap: function (promise) {
          return promise;
        },
      };
      vm.moveApplicationForm = { $valid: true };
    });

    it('prevents submission if form is not valid', function () {
      vm.moveApplicationForm.$valid = false;

      vm.save();

      expect(moveApplicationActionSpy).not.toHaveBeenCalled();
    });

    describe('when error moving application', function () {
      beforeEach(function () {
        moveApplicationActionSpy.and.returnValue(
          $q.reject({
            message: 'move application error',
            incompatibilities: ['incompatibility1', 'incompatibility2'],
          })
        );
        vm.save();
      });

      it('assigns resulting error message to vm.saveError', function () {
        expect(moveApplicationActionSpy).toHaveBeenCalledTimes(1);
        expect(vm.saveError).toBeUndefined();
        scope.$apply(); // resolve promises
        expect(vm.saveError).toEqual('move application error');
      });

      it('assigns provided incompatibilities to vm.incompatibilities', function () {
        expect(vm.incompatibilities).toBeUndefined();
        scope.$apply(); // resolve promises
        expect(vm.incompatibilities).toEqual(['incompatibility1', 'incompatibility2']);
      });

      describe('vm.getError()', function () {
        it('returns vm.saveError value', function () {
          scope.$apply(); // resolve promises
          expect(vm.getError()).toEqual('move application error');
        });
      });

      describe('vm.showIncompatibilities()', function () {
        it('opens info modal with in error mode', function () {
          moveApplicationErrorModal.open.and.returnValue($q.resolve());
          scope.$apply(); // resolve promises
          vm.showIncompatibilities();
          expect(moveApplicationErrorModal.open).toHaveBeenCalledWith(['incompatibility1', 'incompatibility2']);
        });
      });
    });

    describe('when successfully moved application with no warnings', function () {
      beforeEach(function () {
        moveApplicationActionSpy.and.returnValue($q.resolve(['warning1', 'warning2']));

        vm.save();
      });

      it('refreshes nav tree and data, closes the modal and opens info modal', function () {
        spyOn($rootScope, '$broadcast').and.callThrough();
        moveApplicationSuccessModal.open.and.returnValue($q.resolve());
        scope.$apply(); // resolve promises
        expect($rootScope.$broadcast).toHaveBeenCalledWith('reload.owner.tree.data');
        expect(scope.$close).toHaveBeenCalled();
        expect(moveApplicationErrorModal.open).not.toHaveBeenCalled();
        expect(moveApplicationSuccessModal.open).toHaveBeenCalledWith(['warning1', 'warning2']);
        expect(moveApplicationActionSpy).toHaveBeenCalledWith({
          applicationId: 1,
          organizationId: 1,
          organizationName: 'destination org',
        });
      });
    });
  });
});
