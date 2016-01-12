describe('owner.editor.service.spec.js', function () {
  beforeEach(module('owner.manager.module', function ($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function ($modal) {
    spyOn($modal, 'open');
  }));

  it('open', inject(function (OwnerEditorService, $modal) {
    var owner = {
      id : 'foo',
      name : 'bar'
    };

    OwnerEditorService.open(owner, 'organization');
    expect($modal.open).toHaveBeenCalled();

    expect($modal.open.mostRecentCall.args[0].resolve.owner()).toEqual(owner);
    expect($modal.open.mostRecentCall.args[0].resolve.ownerType()).toEqual('organization');
  }));
});
