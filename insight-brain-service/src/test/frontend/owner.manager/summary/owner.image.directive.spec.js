describe('owner.image.directive.spec.js', function() {
  var scope,
      ownerImageScope,
      CLMAppLocations;

  beforeEach(module('owner.manager.module'));

  beforeEach(inject(function($compile, $rootScope, _CLMAppLocations_) {
    CLMAppLocations = _CLMAppLocations_;
    scope = $rootScope.$new();

    spyOn(CLMAppLocations, 'getOwnerImageUrl').and.callThrough();

    scope.owner = {id: '123'};
    var element = $compile('<div owner-image="owner"></div>')(scope);
    ownerImageScope = element.isolateScope();

    scope.$digest();
  }));

  it('Properly requests for owner image url', function() {
    expect(CLMAppLocations.getOwnerImageUrl).toHaveBeenCalledWith(ownerImageScope.owner);
    expect(ownerImageScope.ownerUrl).toBeDefined();
  });

  it('Updates image url owner after changes to owner', function() {
    var previousUrl = ownerImageScope.ownerUrl;

    scope.owner = {id: '111'};
    scope.$digest();

    expect(CLMAppLocations.getOwnerImageUrl).toHaveBeenCalledWith(ownerImageScope.owner);
    expect(ownerImageScope.ownerUrl).not.toEqual(previousUrl);
  });

  it('Refreshes image after owner update event', inject(['event.name.constant', function(EventNameConstant) {
      var previousUrl = ownerImageScope.ownerUrl;

      scope.$broadcast(EventNameConstant.OWNER_UPDATED, scope.owner);
      scope.$digest();

      expect(ownerImageScope.ownerUrl).not.toEqual(previousUrl);
      expect(ownerImageScope.ownerUrl).toMatch(/^.*\?timestamp=.*/);
    }
  ]));
});
