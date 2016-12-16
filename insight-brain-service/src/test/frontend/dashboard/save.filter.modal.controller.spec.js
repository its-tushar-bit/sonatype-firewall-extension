/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('save.filter.modal.controller.js', function() {
  function createController(filter, name, existingFilters) {
    var scope;

    inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      scopes.push(scope);
      $controller('save.filter.modal.controller as vm', {
        $scope: scope,
        filterJson: filter,
        filterName: name,
        existingFilters: existingFilters
      });
      scope.$close = jasmine.createSpy('$close').and.callFake(angular.noop);
      scope.vm.formMask = { wrap : jasmine.createSpy('formMask').and.callFake(function (x) { return x; }) };
      scope.$digest();
    });

    return scope;
  }
  var scopes;

  beforeEach(module('dashboard.module'));

  beforeEach(function() {
    scopes = [];
  });

  afterEach(function () {
    scopes.forEach(function (scope) {
      scope.$destroy();
    });
  });

  it('Passed in name is set', function() {
    var scope = createController({ x : 1 }, 'foo', []);
    expect(scope.vm.filterName).toEqual('foo');
  });

  it('saves filter', inject(function($httpBackend, $timeout, CLMLocations) {
    var scope = createController({ x : 1 }, 'foo', []);
    $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
      name: 'foo',
      filter: {
        x: 1
      }
    }).respond(204);
    scope.vm.saveFilter();
    expect(scope.vm.confirm).toBeFalsy()
    $timeout.flush();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalledWith('foo');
  }));

  it('saves filter', inject(function($httpBackend, $timeout, CLMLocations) {
    var scope = createController({ x : 1 }, 'foo', []);
    $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
      name: 'foo',
      filter: {
        x: 1
      }
    }).respond(204);
    scope.vm.saveFilter();
    expect(scope.vm.confirm).toBeFalsy();
    $timeout.flush();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalledWith('foo');
  }));

  it('overwrites filter', inject(function($httpBackend, $timeout, CLMLocations) {
    var scope = createController({ x : 1 }, 'foo', [{ name: 'foo'}]);

    scope.vm.saveFilter();
    expect(scope.vm.confirm).toBeTruthy();
    $httpBackend.verifyNoOutstandingRequest();

    // User confirms
    scope.vm.saveFilter(true);

    $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
      name: 'foo',
      filter: {
        x: 1
      }
    }).respond(404);
    $timeout.flush();
    $httpBackend.flush();

    // user shouldn't get another confirmation
    scope.vm.saveFilter();

    $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
      name: 'foo',
      filter: {
        x: 1
      }
    }).respond(204);
    $timeout.flush();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalledWith('foo');
  }));
});
