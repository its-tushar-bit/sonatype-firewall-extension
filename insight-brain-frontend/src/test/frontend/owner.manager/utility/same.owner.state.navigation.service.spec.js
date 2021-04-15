/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('same.owner.state.navigation.service.spec.js', function () {
  var SameOwnerStateNavigationService, mockState;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(function () {
    mockState = {
      go: function () {},
      current: { name: 'management.view.organization' },
      params: { organizationId: '123' },
    };

    angular.mock.module(function ($provide) {
      $provide.value('$state', mockState);
    });

    inject(function (_SameOwnerStateNavigationService_) {
      SameOwnerStateNavigationService = _SameOwnerStateNavigationService_;
      spyOn(mockState, 'go');
    });
  });

  it('Properly Refactoring Edit States', function () {
    var mockTo = 'create-label',
      mockParams = { labelId: 'foo' },
      newState = SameOwnerStateNavigationService.refactorStateParams.edit(mockTo);

    expect(newState.to).toEqual('management.edit.organization.' + mockTo);
    expect(newState.params).toEqual(mockState.params);

    newState = SameOwnerStateNavigationService.refactorStateParams.edit(mockTo, mockParams);
    expect(newState.to).toEqual('management.edit.organization.' + mockTo);
    expect(newState.params).toEqual({ organizationId: '123', labelId: 'foo' });
  });

  it('Properly Refactoring View State', function () {
    mockState.current = { name: 'management.edit.organization' };

    var newState = SameOwnerStateNavigationService.refactorStateParams.view();

    expect(newState.to).toEqual('management.view.organization');
    expect(newState.params).toEqual(mockState.params);
  });

  it('Properly Refactoring States with no input', function () {
    var newState = SameOwnerStateNavigationService.refactorStateParams.edit('');

    expect(newState.to).toEqual('management.edit.organization');
    expect(newState.params).toEqual(mockState.params);

    newState = SameOwnerStateNavigationService.refactorStateParams.edit();

    expect(newState.to).toEqual('management.edit.organization');
    expect(newState.params).toEqual(mockState.params);
  });

  it('Properly Calling state.go', function () {
    var mockTo = 'create-label';
    SameOwnerStateNavigationService.goEdit(mockTo);

    expect(mockState.go).toHaveBeenCalledWith('management.edit.organization.' + mockTo, mockState.params);
  });

  it('Properly Calling state.go with no input', function () {
    SameOwnerStateNavigationService.goEdit('');
    expect(mockState.go).toHaveBeenCalledWith('management.edit.organization', mockState.params);

    SameOwnerStateNavigationService.goEdit();
    expect(mockState.go).toHaveBeenCalledWith('management.edit.organization', mockState.params);
  });
});
