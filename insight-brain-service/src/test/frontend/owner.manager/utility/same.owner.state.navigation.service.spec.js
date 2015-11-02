describe('same.owner.state.navigation.service.spec.js', function() {
  var SameOwnerStateNavigationService,
      mockState = {
        go: function(to, params) {
        },
        current: {name: "management.view.organization"},
        params: {organizationId: "123"}
      };

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(function() {
    module(function($provide) {
      $provide.value('$state', mockState);
    });

    inject(function(_SameOwnerStateNavigationService_) {
      SameOwnerStateNavigationService = _SameOwnerStateNavigationService_;
      spyOn(mockState, 'go');
    })
  });

  it('Properly Refactoring States', function() {
    var mockTo = "create-label",
        mockParams = {labelId: "foo"},
        newState = SameOwnerStateNavigationService.refactorStateParams(mockTo);

    expect(newState.to).toEqual("management.edit.organization." + mockTo);
    expect(newState.params).toEqual(mockState.params);

    newState = SameOwnerStateNavigationService.refactorStateParams(mockTo, mockParams);
    expect(newState.to).toEqual("management.edit.organization." + mockTo);
    expect(newState.params).toEqual({organizationId: "123", labelId: "foo"});
  });

  it('Properly Refactoring States with no input', function() {
    var newState = SameOwnerStateNavigationService.refactorStateParams("");

    expect(newState.to).toEqual("management.edit.organization");
    expect(newState.params).toEqual(mockState.params);

    newState = SameOwnerStateNavigationService.refactorStateParams();

    expect(newState.to).toEqual("management.edit.organization");
    expect(newState.params).toEqual(mockState.params);
  });

  it('Properly Calling state.go', function() {
    var mockTo = "create-label";
    SameOwnerStateNavigationService.goEdit(mockTo);

    expect(mockState.go).toHaveBeenCalledWith("management.edit.organization." + mockTo, mockState.params);
  });

  it('Properly Calling state.go with no input', function() {
    SameOwnerStateNavigationService.goEdit("");
    expect(mockState.go).toHaveBeenCalledWith("management.edit.organization", mockState.params);

    SameOwnerStateNavigationService.goEdit();
    expect(mockState.go).toHaveBeenCalledWith("management.edit.organization", mockState.params);
  });
});
