/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import '../SpecUtil';
import { ClaimComponentModule } from '../../../main/frontend/cip/cip-claim-component';

/*global InsightDatatable, window*/
var dataTableItems = [];
var current = 0;

var componentIdentifier = {
  format: 'maven',
  coordinates: {
    groupId: 'g',
    artifactId: 'a',
    version: 'v',
    extension: 'e',
  },
};
var component = {
  componentIdentifier: componentIdentifier,
  hash: 'abcdefghij0123456789',
  //TODO remove once we can claim other formats https://issues.sonatype.org/browse/CLM-3719
  groupId: componentIdentifier.coordinates.groupId,
  artifactId: componentIdentifier.coordinates.artifactId,
  version: componentIdentifier.coordinates.version,
  extension: componentIdentifier.coordinates.extension,
  createTime: 100,
  comment: 'testc',
};

describe('CIP Claim Component tests', function () {
  var scope, $http, formSetPristineSpy;

  beforeEach(
    angular.mock.module(ClaimComponentModule.name, function ($provide) {
      $provide.value('ComponentUtil', {
        setDisplayNameAndCoordinates: function () {},
      });
    })
  );
  // setup our http backend to return what we want
  beforeEach(inject(function (
    $rootScope,
    $controller,
    $httpBackend,
    $location
  ) {
    window.InsightDatatable = {
      getActiveTable: function () {
        return {
          dataView: {
            getItems: function () {
              return dataTableItems;
            },
            beginUpdate: function () {},
            updateItem: function (id, data) {
              dataTableItems[current++] = data;
            },
            endUpdate: function () {},
          },
        };
      },
    };

    window.CLM = {
      path: '../brain/',
    };

    dataTableItems = [];
    current = 0;
    $http = $httpBackend;
    scope = $rootScope.$new();
    //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
    $location.url('/sonatype-clm-report/');
    $controller('ClaimComponentController', {
      $scope: scope,
      global: {},
      CurrentData: {
        hash: '1',
        createTime: 1,
      },
    });

    scope.claimForm = {
      $valid: true,
      $setPristine: angular.noop,
    };
    formSetPristineSpy = spyOn(scope.claimForm, '$setPristine');
  }));

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Test Claim Component', function () {
    expect(scope.formValid()).toEqual(false);
    scope.claimData.groupId = 'groupid';
    expect(scope.formValid()).toEqual(false);
    scope.claimData.artifactId = 'artifactid';
    expect(scope.formValid()).toEqual(false);
    scope.claimData.version = 'version';
    expect(scope.formValid()).toEqual(false);
    scope.claimData.extension = 'e';
    expect(scope.formValid()).toEqual(true);

    $http
      .expectPOST(
        SpecUtil.toRegExp('../brain/rest/component/identified'),
        function (data) {
          var obj = JSON.parse(data);
          expect(obj.componentIdentifier.coordinates.groupId).toEqual(
            'groupid'
          );
          expect(obj.componentIdentifier.coordinates.artifactId).toEqual(
            'artifactid'
          );
          expect(obj.componentIdentifier.coordinates.version).toEqual(
            'version'
          );
          expect(obj.componentIdentifier.coordinates.classifier).toEqual('');
          expect(obj.componentIdentifier.coordinates.extension).toEqual('e');
          return true;
        }
      )
      .respond(component);
    scope.claimSubmit();
    $http.flush();
    expect(formSetPristineSpy).toHaveBeenCalled();
  });

  it('Test multiple duplicate hashes handled properly', function () {
    dataTableItems = [
      {
        hash: '1',
        id: '1',
      },
      {
        hash: '1',
        id: '1',
      },
    ];
    $http
      .expectPOST(SpecUtil.toRegExp('../brain/rest/component/identified'))
      .respond(component);
    scope.claimSubmit();
    $http.flush();

    confirmDataViewUpdated(2);
    expect(formSetPristineSpy).toHaveBeenCalled();
  });

  it('Can revoke a claim on a component', inject(function (Dialog) {
    spyOn(Dialog, 'open');
    $http
      .expectDELETE(SpecUtil.toRegExp('../brain/rest/component/identified/1'))
      .respond(204);

    scope.revokeClaimSubmit();

    expect(Dialog.open).toHaveBeenCalledWith({
      title: 'Revoke Claim',
      body:
        'Are you sure you want to revoke the claim on this component?' +
        ' This change will not be reflected until a new policy evaluation is triggered.',
      buttons: [
        {
          name: 'Revoke',
          type: 'primary',
          click: jasmine.any(Function),
        },
        {
          name: 'Cancel',
          type: 'cancel',
        },
      ],
      windowClass: null,
      backdropClass: null,
    });
    Dialog.open.calls.mostRecent().args[0].buttons[0].click();
    $http.flush();
  }));

  it('Can update a claim on a component', function () {
    dataTableItems = [
      {
        hash: '1',
        id: '1',
      },
    ];

    $http
      .expectPUT(SpecUtil.toRegExp('../brain/rest/component/identified'))
      .respond(component);
    scope.claimUpdateSubmit();
    $http.flush();
    confirmDataViewUpdated(1);
  });

  // Functional tests for the clm datepicker since we do not have a functional tests for the cip components
  describe('clm datepicker test', function () {
    var element, scope, formSetDirtySpy;

    beforeEach(inject(function ($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.claimData = {};

      scope.claimData.createTimeText = '12/12/2012';

      scope.claimForm = { $setDirty: angular.noop };
      formSetDirtySpy = spyOn(scope.claimForm, '$setDirty');

      element = angular.element(
        '<div clm-datepicker><input name="foo" ng-model="claimData.createTimeText"></div>'
      );
      $compile(element)(scope);
    }));

    it('binds data correctly', function () {
      expect(scope.claimData.createTimeText).toBe('12/12/2012');

      element.find('td.day:contains(1)').first().click();
      expect(scope.claimData.createTimeText).toBe('12/01/2012');
      expect(formSetDirtySpy).toHaveBeenCalled();
    });
  });

  function confirmDataViewUpdated(number) {
    var items = InsightDatatable.getActiveTable().dataView.getItems();

    expect(items.length).toEqual(number);

    function validateItem(item) {
      expect(item.hash).toEqual('1');
      expect(item.id).toEqual('1');
      expect(item.identificationSource).toEqual('Manual');
      expect(item.matchState).toEqual('exact');
      expect(item.groupId).toEqual(component.groupId);
      expect(item.artifactId).toEqual(component.artifactId);
      expect(item.version).toEqual(component.version);
      expect(item.extension).toEqual(component.extension);
      expect(item.createTime).toEqual(component.createTime);
      expect(item.comment).toEqual(component.comment);
    }

    for (var i = 0; i < number; i++) {
      validateItem(items[i]);
    }
  }
});
