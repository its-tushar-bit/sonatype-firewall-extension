/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import utilityModule from '../../../../main/frontend/utility/utility.module';

describe('DeleteModalReduxController', function () {
  var $controller, unsubscribeSpy, $rootScope, $ngRedux, maskDeferred, $q;

  beforeEach(
    angular.mock.module(utilityModule.name, function ($provide) {
      unsubscribeSpy = SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function (_$controller_, _$ngRedux_, _$rootScope_, _$q_) {
    $controller = _$controller_;
    $rootScope = _$rootScope_;
    $ngRedux = _$ngRedux_;
    $q = _$q_;
  }));

  function createController(
    resourceType,
    resourceName,
    headerText,
    bodyText,
    maskText,
    continueAction,
    cancelAction,
    stateMapper
  ) {
    var scope = $rootScope.$new();

    if (!continueAction) {
      continueAction = jasmine.createSpy('continueAction');
    }

    if (!cancelAction) {
      cancelAction = jasmine.createSpy('cancelAction');
    }

    var vm = $controller('DeleteModalReduxController as vm', {
      resourceType: resourceType,
      resourceName: resourceName,
      headerText: headerText,
      bodyText: bodyText,
      maskText: maskText,
      continueAction: continueAction,
      cancelAction: cancelAction,
      stateMapper: stateMapper,
      $scope: scope,
    });

    maskDeferred = $q.defer();

    vm.deleteResourceMask = {
      showSuccessMaskBriefly: jasmine
        .createSpy('showSuccessMaskBriefly')
        .and.returnValue(maskDeferred.promise),
      activateMask: jasmine.createSpy('activateMask'),
      removeMask: jasmine.createSpy('removeMask'),
    };

    scope.$close = jasmine.createSpy('$close');

    scope.$digest();

    return scope;
  }

  it('unsubscribes from the redux store on $destroy', function () {
    var scope = createController();

    expect(unsubscribeSpy).not.toHaveBeenCalled();

    scope.$destroy();

    expect(unsubscribeSpy).toHaveBeenCalled();
  });

  describe('watcher of vm.deleting and vm.success', function () {
    it('calls vm.deleteResourceMask.showSuccessMaskBriefly when vm.success is true and vm.deleting is false', function () {
      var scope = createController(),
        vm = scope.vm;

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);

      vm.success = true;
      vm.deleting = false;
      scope.$digest();

      expect(vm.deleteResourceMask.showSuccessMaskBriefly).toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);
    });

    it('calls scope.$close after the mask promise resolves', function () {
      var scope = createController(),
        vm = scope.vm;

      scope.$close = jasmine.createSpy('$close');

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);

      vm.success = true;
      vm.deleting = false;
      scope.$digest();

      expect(vm.deleteResourceMask.showSuccessMaskBriefly).toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);
      expect(scope.$close).not.toHaveBeenCalled();

      maskDeferred.resolve();
      scope.$digest();

      expect(scope.$close).toHaveBeenCalled();
    });

    it('calls vm.deleteResourceMask.showSuccessMaskBriefly when vm.success and vm.deleting are both true', function () {
      var scope = createController(),
        vm = scope.vm;

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);

      vm.success = true;
      vm.deleting = true;
      scope.$digest();

      expect(vm.deleteResourceMask.showSuccessMaskBriefly).toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);
    });

    it('calls vm.deleteResourceMask.activateMask when vm.success is false and vm.deleting is true', function () {
      var scope = createController(),
        vm = scope.vm;

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);

      vm.deleting = true;
      scope.$digest();

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).toHaveBeenCalled();
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(1);
    });

    it('calls vm.deleteResourceMask.removeMask when vm.success is false and vm.deleting is false', function () {
      var scope = createController(),
        vm = scope.vm;

      // have to first set it to true in order to test setting it to false again afterwards
      vm.deleting = true;
      scope.$digest();

      expect(vm.deleteResourceMask.activateMask).toHaveBeenCalled();

      vm.deleting = false;
      scope.$digest();

      expect(
        vm.deleteResourceMask.showSuccessMaskBriefly
      ).not.toHaveBeenCalled();
      expect(vm.deleteResourceMask.activateMask).toHaveBeenCalledTimes(1);
      expect(vm.deleteResourceMask.removeMask).toHaveBeenCalledTimes(2);
    });
  });

  describe('mapStateToThis', function () {
    it('returns an object with the properties returned by stateMapper', function () {
      var mappedState = { foo: 'bar' },
        stateMapper = jasmine
          .createSpy('stateMapper')
          .and.returnValue(mappedState);

      createController(
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        undefined,
        stateMapper
      );

      var mapStateToThis = $ngRedux.connect.calls.first().args[0],
        inputState = { baz: 'buzz' };

      var result = mapStateToThis(inputState);

      expect(result.foo).toBe('bar');
      expect(stateMapper).toHaveBeenCalledWith(inputState);
    });

    it(
      'sets the error from the errorState property that comes out of the stateMapper, run through ' +
        'Message.getHttpErrorMessage',
      inject(function (Messages) {
        var stateMapper = function (x) {
          return x;
        };

        spyOn(Messages, 'getHttpErrorMessage').and.returnValue('error2');

        createController(
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          undefined,
          stateMapper
        );

        var mapStateToThis = $ngRedux.connect.calls.first().args[0],
          inputState = { errorState: 'error!' };

        var result = mapStateToThis(inputState);

        expect(result.error).toBe('error2');
        expect(Messages.getHttpErrorMessage).toHaveBeenCalledWith('error!');
      })
    );
  });

  it('sets the resourceName, resourceType, headerText, bodyText, and maskText from the injectables', function () {
    var resourceType = 'type',
      resourceName = 'name',
      headerText = 'header',
      bodyText = 'body',
      maskText = 'mask',
      scope = createController(
        resourceType,
        resourceName,
        headerText,
        bodyText,
        maskText
      ),
      vm = scope.vm;

    expect(vm.resourceName).toBe(resourceName);
    expect(vm.resourceType).toBe(resourceType);
    expect(vm.headerText).toBe(headerText);
    expect(vm.bodyText).toBe(bodyText);
    expect(vm.maskText).toBe(maskText);
  });

  it('sets vm.deleteResource to the continueAction', function () {
    var resourceType = 'type',
      resourceName = 'name',
      headerText = 'header',
      bodyText = 'body',
      maskText = 'mask',
      continueAction = jasmine.createSpy('continueAction'),
      scope = createController(
        resourceType,
        resourceName,
        headerText,
        bodyText,
        maskText,
        continueAction
      ),
      vm = scope.vm;

    expect(vm.deleteResource).toBe(continueAction);
  });
});
