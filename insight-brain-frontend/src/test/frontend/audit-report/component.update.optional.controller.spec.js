/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('component.update.optional.controller', function () {
  var scope, vm;

  beforeEach(
    angular.mock.module('audit', function ($provide) {
      $provide.value('OwnerContext', {
        ownerId: 'foo',
        ownerType: 'repository',
      });
      $provide.value('pendoService', {
        start: angular.noop,
      });
    })
  );

  beforeEach(inject(function ($rootScope, $controller) {
    scope = $rootScope.$new();

    vm = $controller('component.update.optional.controller', {
      $scope: scope,
    });
    scope.$close = jasmine.createSpy('$close');
  }));

  it('sets immediate reevaluate to false', function () {
    expect(vm.immediateReevaluate).toBeFalsy();
  });

  it('performs a reevaluations', inject(function ($httpBackend) {
    $httpBackend
      .expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate'))
      .respond(204);
    vm.forceReevaluation();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalled();
  }));

  it('handles reevaluation failures', inject(function ($httpBackend) {
    $httpBackend
      .expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate'))
      .respond(404, 'failure');
    vm.forceReevaluation();
    $httpBackend.flush();

    expect(vm.error).toEqual('failure');

    // Recovers on retry success
    $httpBackend
      .expectPOST(SpecUtil.toRegExp('rest/repositories/foo/evaluate'))
      .respond(204);
    vm.forceReevaluation();
    $httpBackend.flush();

    expect(scope.$close).toHaveBeenCalled();
  }));
});
