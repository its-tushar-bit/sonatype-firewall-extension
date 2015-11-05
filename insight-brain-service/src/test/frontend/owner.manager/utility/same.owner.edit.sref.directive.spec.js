describe('same.owner.edit.sref.directive.spec.js', function() {
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
    spyOn(SameOwnerStateNavigationService.refactorStateParams, 'edit').andReturn({
      to: 'management.edit.organization.label',
      params: {organizationId: '123', labelId: '123'}
    });

    var element = $compile('<button same-owner-edit-sref=label({&quot;labelId&quot;:&quot;123&quot;})></button>')($rootScope);

    expect(element.attr('same-owner-edit-sref')).toBeUndefined();
    expect(element.attr('ui-sref')).toEqual('management.edit.organization.label({"organizationId":"123","labelId":"123"})');
  });

});
