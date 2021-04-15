/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import systemNoticeConfigurationModule from '../../../../main/frontend/configuration/systemNoticeConfiguration/systemNoticeConfigurationModule';

describe('systemNoticeConfigurationModuleSpec.js', function () {
  beforeEach(
    angular.mock.module('ui.router', systemNoticeConfigurationModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  var $state;

  beforeEach(inject(function (_$state_) {
    $state = _$state_;
  }));

  it('sets up the ui router state', function () {
    expect($state.get('systemNoticeConfiguration').url).toEqual('/systemNoticeConfiguration');
    expect($state.get('systemNoticeConfiguration').data.title).toEqual('System Notice');
    expect($state.get('systemNoticeConfiguration').resolve.isAuthorized[0]).toEqual('PermissionService');
    var permissionServiceFunction = $state.get('systemNoticeConfiguration').resolve.isAuthorized[1];
    var permissionServiceMock = {
      isAuthorized: function () {},
    };
    spyOn(permissionServiceMock, 'isAuthorized');
    permissionServiceFunction(permissionServiceMock);
    expect(permissionServiceMock.isAuthorized).toHaveBeenCalledWith(['CONFIGURE_SYSTEM'], true);
  });
});
