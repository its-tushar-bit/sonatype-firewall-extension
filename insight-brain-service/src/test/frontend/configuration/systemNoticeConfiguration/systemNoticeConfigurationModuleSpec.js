describe('systemNoticeConfigurationModuleSpec.js', function() {
  beforeEach(function() {
    module('ui.router');
    module('systemNoticeConfigurationModule', function($provide) {
      $provide.value('$cookies', {
        get: angular.noop
      });
    });
  });

  var $state;

  beforeEach(inject(function(_$state_) {
    $state = _$state_;
  }));

  it('sets up the ui router state', function() {
    expect($state.get('systemNotice').url).toEqual('/systemNotice');
    expect($state.get('systemNotice').controller).toEqual('systemNoticeConfigurationController');
    expect($state.get('systemNotice').controllerAs).toEqual('vm');
    expect($state.get('systemNotice').templateUrl).toEqual(
        'configuration/systemNoticeConfiguration/systemNoticeConfiguration.html');
    expect($state.get('systemNotice').data.title).toEqual('System Notice');
    expect($state.get('systemNotice').resolve.isAuthorized[0]).toEqual('PermissionService');
    var permissionServiceFunction = $state.get('systemNotice').resolve.isAuthorized[1];
    var permissionServiceMock = {
      isAuthorized: function() {
      }
    };
    spyOn(permissionServiceMock, 'isAuthorized');
    permissionServiceFunction(permissionServiceMock);
    expect(permissionServiceMock.isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM'], true);
  });
});
