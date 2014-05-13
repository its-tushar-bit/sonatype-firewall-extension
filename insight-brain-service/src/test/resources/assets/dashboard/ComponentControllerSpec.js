describe('ComponentController tests', function() {
  beforeEach(module('ComponentModule'));

  var applicationComponents = [
    {
      application: {
        id: 'appId',
        publicId: 'appPublicId',
        name: 'appName',
        organizationId: 'appOrgId',
        organizationName: 'appOrgName',
        contact: {
          internalName: 'admin',
          displayName: 'Admin BuiltIn',
          email: 'admin@localhost',
          realm: 'CLM',
          error: null
        }
      },
      policyViolations: [
        {
          policyId: 'policy1Id',
          policyName: 'policy1Name',
          threatLevel: 7,
          time: 1,
          reasons: [
            {
              constraintName: 'policy1ConstraintName',
              reasons: [
                'policy1Reason'
              ]
            }
          ],
          stageTypeIds: [
            'build'
          ]
        }, {
          policyId: 'policy2Id',
          policyName: 'policy2Name',
          threatLevel: 9,
          time: 2,
          reasons: [
            {
              constraintName: 'policy2ConstraintName',
              reasons: [
                'policy2ConstraintReason'
              ]
            }
          ],
          stageTypeIds: [
            'build'
          ]
        }
      ]
    }
  ];

  describe('ComponentController', function() {
    var scope;

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(CLMLocations.getComponentDetailsUrl()).respond(applicationComponents);
      $controller('componentController', { $scope: scope });
      $httpBackend.flush();
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