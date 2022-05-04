/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import * as orgsAndPoliciesRootSelectors from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { mapStateToThis } from 'MainRoot/owner.manager/license.threat.group/license.threat.group.tile.controller';

describe('license.threat.group.tile.controller.js', function () {
  var vm, $httpBackend, CLMContextLocations, EventNameConstant, $rootScope, scope;

  var responseMock = {
    licenseThreatGroupsByOwner: [{ ownerName: 'Foo' }],
  };

  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);
      $provide.value('$cookies', {
        get: angular.noop,
      });
    })
  );

  beforeEach(inject(function (_$rootScope_, $injector, $controller, _$httpBackend_, _CLMContextLocations_) {
    $rootScope = _$rootScope_;
    $httpBackend = _$httpBackend_;
    scope = $rootScope.$new();
    CLMContextLocations = _CLMContextLocations_;
    EventNameConstant = $injector.get('event.name.constant');

    vm = $controller('LicenseThreatGroupTileController', {
      $scope: scope,
    });
  }));

  afterEach(function () {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });
  describe('passes true as the waitForLogin http config if no parameter is passed to it', () => {
    beforeEach(() => {
      $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    });

    afterEach(() => {
      $httpBackend.flush();
    });

    describe('on component init', () => {
      it('subscribes to the redux store', () => {
        expect(vm.unsubscribe).toBeDefined();
      });
    });

    describe('on $destroy()', () => {
      it('unsubscribes from the redux store', () => {
        expect(vm.unsubscribe).not.toHaveBeenCalled();
        scope.$destroy();
        expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
      });
    });

    describe('mapStateToThis', () => {
      it('maps redux properties to component', () => {
        spyOn(orgsAndPoliciesRootSelectors, 'selectSelectedOwnerName').and.returnValue('OwnerName');

        const output = mapStateToThis({});

        expect(output.ownerName).toBe('OwnerName');
      });
    });
  });

  it('Reloads on broadcasted owner summary reload event', function () {
    $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    expect($httpBackend.flush).not.toThrow();

    $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA);

    $httpBackend.expectGET(CLMContextLocations.getApplicableLicenseGroupsUrl()).respond(responseMock);
    expect($httpBackend.flush).not.toThrow();
  });
});
