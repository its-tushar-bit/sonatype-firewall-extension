/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('owner.image.directive.spec.js', function () {
  var scope, ownerImageScope, CLMContextLocations;

  beforeEach(angular.mock.module(ownerManagerModule.name));

  beforeEach(inject(function ($compile, $rootScope, _CLMContextLocations_) {
    CLMContextLocations = _CLMContextLocations_;
    scope = $rootScope.$new();

    spyOn(CLMContextLocations, 'getOwnerImageUrl').and.callThrough();

    scope.owner = { id: '123' };
    var element = $compile('<div owner-image="owner"></div>')(scope);
    ownerImageScope = element.isolateScope();

    scope.$digest();
  }));

  it('Properly requests for owner image url', function () {
    expect(CLMContextLocations.getOwnerImageUrl).toHaveBeenCalledWith(
      ownerImageScope.owner
    );
    expect(ownerImageScope.ownerUrl).toBeDefined();
  });

  it('Updates image url owner after changes to owner', function () {
    var previousUrl = ownerImageScope.ownerUrl;

    scope.owner = { id: '111' };
    scope.$digest();

    expect(CLMContextLocations.getOwnerImageUrl).toHaveBeenCalledWith(
      ownerImageScope.owner
    );
    expect(ownerImageScope.ownerUrl).not.toEqual(previousUrl);
  });

  it('Refreshes image after owner update event', inject([
    'event.name.constant',
    function (EventNameConstant) {
      var previousUrl = ownerImageScope.ownerUrl;

      scope.$broadcast(EventNameConstant.OWNER_UPDATED, scope.owner);
      scope.$digest();

      expect(ownerImageScope.ownerUrl).not.toEqual(previousUrl);
      expect(ownerImageScope.ownerUrl).toMatch(/^.*\?timestamp=.*/);
    },
  ]));
});
