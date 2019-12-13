/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import componentModule from '../../../main/frontend/dashboard/ComponentController';

describe('ComponentController tests', function() {
  beforeEach(angular.mock.module(componentModule.name));

  var applicationComponents = [
    {
      application: {
        id: 'appId',
        publicId: 'appPublicId',
        name: 'appName',
        organizationId: 'appOrgId',
        organizationName: 'appOrgName'
      },
      policyViolations: [
        {
          policyId: 'policy1Id',
          policyName: 'policy1Name',
          threatLevel: 7,
          time: 1,
          stageTypeIds: [
            'build'
          ]
        }, {
          policyId: 'policy2Id',
          policyName: 'policy2Name',
          threatLevel: 9,
          time: 2,
          stageTypeIds: [
            'build'
          ]
        }
      ]
    }
  ];

  describe('ComponentController', function() {
    var scope, displayName = [
      {field: 'Group', value: 'foo'},
      {value: ':'},
      {field: 'Artifact', value: 'bar'},
      {value: ':'},
      {field: 'Version', value: '1.0'}
    ];
    beforeEach(inject(function($rootScope, $controller, $httpBackend, $timeout, $q, CLMLocations, StageTypeStore) {
      scope = $rootScope.$new();
      var stageTypeStoreDefer = $q.defer();
      spyOn(StageTypeStore, 'getDashboardStages').and.returnValue(stageTypeStoreDefer.promise);
      stageTypeStoreDefer.resolve([]);
      $httpBackend.expectGET(CLMLocations.getComponentDetailsUrl()).respond(applicationComponents);
      $httpBackend.expectGET(CLMLocations.getComponentNameUrl()).respond(displayName);
      $controller('componentController', { $scope: scope });
      $httpBackend.flush();
      $timeout.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    it('loads application components', function() {
      expect(scope.applicationComponents.length).toBe(1);
      expect(scope.applicationComponents[0].application.id).toBe('appId');
      expect(scope.applicationComponents[0].policyViolations.length).toBe(2);
      expect(scope.applicationComponents[0].policyViolations[0].policyId).toBe('policy1Id');
    });

    it('calculates the total risk', function() {
      expect(scope.totalRisk).toBe(16);
    });

    it('loads a component name', function() {
      expect(scope.component.displayName).toEqual(displayName);
    });
  });

  describe('riskPie', function() {
    var element,
        scope;

    beforeEach(inject(function($rootScope, $compile) {
      scope = $rootScope.$new();
      element = angular.element('<span risk-pie risk="0.33" height="24" width="24"></span>');
      element = $compile(element)(scope);
    }));

    it('should render to the specified dimensions', function() {
      var svg = element.find('svg');
      expect(svg.length).toBe(1);
      expect(+svg.attr('width')).toBe(24);
      expect(+svg.attr('height')).toBe(24);
    });

    it('should apply the correct arc class', inject(function($compile) {
      var g = element.find('g');
      expect(g.attr('class')).toBeUndefined();

      element = angular.element('<span risk-pie risk="0.66" height="24" width="24" clazz="foo"></span>');
      element = $compile(element)(scope);
      g = element.find('g');
      expect(g.attr('class')).toBe('foo');
    }));
  });
});
