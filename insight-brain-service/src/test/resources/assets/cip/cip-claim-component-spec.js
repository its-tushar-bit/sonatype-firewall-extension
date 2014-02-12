/*global window*/
var dataTableItems = [];
var current = 0;
var InsightDatatable = {
  getActiveTable: function() {
    return {
      dataView: {
        getItems: function() {
          return dataTableItems;
        },
        beginUpdate: function() {
        },
        updateItem: function(id, data) {
          dataTableItems[current++] = data;
        },
        endUpdate: function() {
        }
      }
    };
  }
};

describe('CIP Claim Component tests', function() {
  'use strict';
  var scope, $http;

  beforeEach(module('ClaimComponent'));
  // setup our http backend to return what we want
  beforeEach(inject(function($rootScope, $controller, $httpBackend, $location) {
    dataTableItems = [];
    $http = $httpBackend;
    scope = $rootScope.$new();
    //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
    $location.url('/sonatype-clm-report/');
    $controller('ClaimComponentController', {
      $scope: scope,
      global: {},
      CurrentData: {
        hash: "1",
        createTime: 1
      }
    });
    
    scope.claimForm = {
      $valid: true
    };
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('Test Claim Component', function() {
    expect(scope.formValid()).toEqual(false);
    scope.claimData.groupId = 'groupid';
    expect(scope.formValid()).toEqual(false);
    scope.claimData.artifactId = 'artifactid';
    expect(scope.formValid()).toEqual(false);
    scope.claimData.version = 'version';
    expect(scope.formValid()).toEqual(true);

    $http.expectPOST(SpecUtil.toRegExp('../brain/rest/component/identified')).respond({});
    scope.claimSubmit();
    $http.flush();
  });
  
  it('Test multiple duplicate hashes handled properly', function() {
    dataTableItems = [{
      hash: '1',
      id: '1'
    }, {
      hash: '1',
      id: '1'
    }];
    $http.expectPOST(SpecUtil.toRegExp('../brain/rest/component/identified')).respond({
      groupId: 'testg',
      artifactId: 'testa',
      version: 'testv',
      createTime: 100
    });
    scope.claimSubmit();
    $http.flush();   
    
    var items = InsightDatatable.getActiveTable().dataView.getItems();
    
    expect(items.length).toEqual(2);
    
    function validateItem(item) {
      expect(item.hash).toEqual('1');
      expect(item.id).toEqual('1');
      expect(item.identificationSource).toEqual('Manual');
      expect(item.matchState).toEqual('exact');
      expect(item.groupId).toEqual('testg');
      expect(item.artifactId).toEqual('testa');
      expect(item.version).toEqual('testv');
      expect(item.createTime).toEqual(100);
    }
    
    validateItem(items[0]);
    validateItem(items[1]);
  });

  // Functional tests for the clm datepicker since we do not have a functional tests for the cip components
  describe('clm datepicker test', function() {
    var compiled, element, scope;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      scope.claimData = {};

      scope.claimData.createTimeText = '12/12/2012';

      element = angular.element('<div clm-datepicker><input name="foo" ng-model="claimData.createTimeText"></div>');
      compiled = $compile(element)(scope);
    }));

    it('binds data correctly', function() {
      expect(scope.claimData.createTimeText).toBe('12/12/2012');

      element.find('td.day:contains(1)').first().click();
      expect(scope.claimData.createTimeText).toBe('12/01/2012');
    });
  });
});