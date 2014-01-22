/*global window*/
var InsightDatatable = {
  getActiveTable: function() {
    return {
      dataView: {
        getItems: function() {
          return [];
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
    $http = $httpBackend;
    scope = $rootScope.$new();
    //simply so we don't have to worry about comparing urls against ../../../../.././ etc etc
    $location.url('/sonatype-clm-report/');
    $controller('ClaimComponentController', {
      $scope: scope,
      global: {},
      CurrentData: function() {
        return {
          hash: "1",
          createTime: 1
        };
      }
    });
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

    scope.claimForm = {
      $valid: true
    };

    $http.expectPOST('../brain/rest/component/identified').respond({});
    scope.claimSubmit();
    $http.flush();
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