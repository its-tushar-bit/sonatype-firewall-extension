/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('save.filter.modal.controller.js', function() {
  function createController(filter, name) {
    var scope;

    inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      scopes.push(scope);
      $controller('save.filter.modal.controller as vm', { $scope: scope, filterJson: filter, filterName: name });
      scope.$close = jasmine.createSpy('$close');
      scope.vm.formMask = { wrap : jasmine.createSpy('formMask').andCallFake(function (x) { return x; }) };
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
    var scope = createController({ x : 1 }, 'foo');
    expect(scope.vm.filterName).toEqual('foo');
  });

  it('saves filter', inject(function($httpBackend, CLMLocations) {
    var scope = createController({ x : 1 }, 'foo');
    $httpBackend.expectPUT(CLMLocations.getDashboardSavedFilters(), {
      name: 'foo',
      filter: {
        x: 1
      }
    }).respond(204);
    scope.vm.saveFilter();
    $httpBackend.flush();
    
    expect(scope.$close).toHaveBeenCalledWith('foo');
  }));
});
