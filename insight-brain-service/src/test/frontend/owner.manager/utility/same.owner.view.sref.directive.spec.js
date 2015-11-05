describe('same.owner.view.sref.directive.spec.js', function() {
  var $compile,
      $rootScope,
      SameOwnerStateNavigationService;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(inject(function(_$compile_, _$rootScope_, _SameOwnerStateNavigationService_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    SameOwnerStateNavigationService = _SameOwnerStateNavigationService_;
  }));

  it('Properly wrapping around ui-sref', function() {
    spyOn(SameOwnerStateNavigationService.refactorStateParams, 'view').andReturn({
      to: 'management.view.organization',
      params: {organizationId: '123'}
    });

    var element = $compile('<button same-owner-view-sref></button>')($rootScope);

    expect(element.attr('same-owner-view-sref')).toBeUndefined();
    expect(element.attr('ui-sref')).toEqual('management.view.organization({"organizationId":"123"})');
  });
});
