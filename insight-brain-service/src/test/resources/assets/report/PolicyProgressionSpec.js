/*
 Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  describe('Controller: PolicyProgressionCtrl', function() {
    var PolicyprogressionCtrl, scope;
    beforeEach(module('ReportTrending'));
    PolicyprogressionCtrl = {};
    scope = {};
    beforeEach(inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      scope.data = angular.extend(angular.copy(TrendingReportMockData.get()), ChartMockData.getDiffData()); 
      PolicyprogressionCtrl = $controller('PolicyProgressionCtrl', {
        $scope: scope
      });
      scope.$digest();
    }));
    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      return $httpBackend.verifyNoOutstandingRequest();
    }));
    it('should have policies available', function() {
      return expect(scope.policies.length).toBe(13);
    });
    it('should be able to translate a threat level into a css class to apply', function() {
      return expect(scope.threatLevelClass(10)).toBe('threat-chiclet-critical');
    });
    describe('Display of threat level number in table', function() {
      var testCase, threatLevelTestCases, _i, _len, _results;
      threatLevelTestCases = [
        {
          input: 0,
          expected: 10
        }, {
          input: 1,
          expected: ''
        }, {
          input: 2,
          expected: ''
        }, {
          input: 3,
          expected: 9
        }, {
          input: 4,
          expected: ''
        }, {
          input: 5,
          expected: 7
        }, {
          input: 6,
          expected: 6
        }, {
          input: 7,
          expected: 5
        }, {
          input: 8,
          expected: 3
        }, {
          input: 9,
          expected: 1
        }, {
          input: 10,
          expected: ''
        }, {
          input: 11,
          expected: ''
        }, {
          input: 12,
          expected: 0
        }
      ];
      _results = [];
      for (_i = 0, _len = threatLevelTestCases.length; _i < _len; _i++) {
        testCase = threatLevelTestCases[_i];
        _results.push((function(input, expected) {
          return it("should show the threat level number: " + expected + " for the # " + input + " policy", function() {
            return expect(scope.threatLevel(input)).toEqual(expected);
          });
        })(testCase.input, testCase.expected));
      }
      return _results;
    });
    return describe('Should show only absolute values for difference of violations from first evaluation to last', function() {
      var absTestCases, testCase, _i, _len, _results;
      absTestCases = [
        {
          input: 0,
          expected: ''
        }, {
          input: 1,
          expected: 1
        }, {
          input: -1,
          expected: 1
        }, {
          input: '',
          expected: ''
        }, {
          input: null,
          expected: ''
        }
      ];
      _results = [];
      for (_i = 0, _len = absTestCases.length; _i < _len; _i++) {
        testCase = absTestCases[_i];
        _results.push((function(input, expected) {
          return it("should show this value: " + expected + " for this difference between policy evaluations results: " + input, function() {
            return expect(scope.abs(input)).toEqual(expected);
          });
        })(testCase.input, testCase.expected));
      }
      return _results;
    });
  });

}).call(this);
