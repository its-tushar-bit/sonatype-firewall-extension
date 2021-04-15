/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('same.owner.view.sref.directive.spec.js', function () {
  var $compile, $rootScope, SameOwnerStateNavigationService;

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (
    _$compile_,
    _$rootScope_,
    _SameOwnerStateNavigationService_
  ) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    SameOwnerStateNavigationService = _SameOwnerStateNavigationService_;
  }));

  it('Properly wrapping around ui-sref', function () {
    spyOn(
      SameOwnerStateNavigationService.refactorStateParams,
      'view'
    ).and.returnValue({
      to: 'management.view.organization',
      params: { organizationId: '123' },
    });

    var element = $compile('<button same-owner-view-sref></button>')(
      $rootScope
    );

    expect(element.attr('same-owner-view-sref')).toBeUndefined();
    expect(element.attr('ui-sref')).toEqual(
      'management.view.organization({"organizationId":"123"})'
    );
  });
});
