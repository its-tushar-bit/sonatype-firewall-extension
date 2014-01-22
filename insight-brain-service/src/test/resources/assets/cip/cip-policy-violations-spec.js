describe('CIP Policy Waiver tests', function() {
  var _scope, _viewScope;

  beforeEach(module('PolicyViolations'));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  afterEach(function() {
    _scope.$destroy();
  });

  describe('PolicyViolationsController', function () {
    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend) {
      _scope = $rootScope.$new();

      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/policy/actionType')).respond({});
      $httpBackend.expectGET(SpecUtil.toRegExp('policyalerts.json')).respond({
        aaData: [
          {
            trigger: {
              policyId: "policyId",
              policyName: "name",
              threatLevel: 5,
              componentFacts: [
                {
                  groupId: "bsh",
                  artifactId: "bsh",
                  version: "1.3.0",
                  hash: "1fed35193d56470f46c0",
                  constraintFacts: [
                    {
                      constraintId: "c7ad07e00c4948c59651cce82163e50a",
                      constraintName: "test3",
                      operatorName: "AND",
                      conditionFacts: [
                        {
                          conditionTypeId: "AgeInDays",
                          summary: "Age older than 1825",
                          reason: "Age was 7 years, 8 months and 17 days"
                        },
                        {
                          conditionTypeId: "AgeInDays",
                          summary: "Age older than 730",
                          reason: "Age was 7 years, 8 months and 17 days"
                        }
                      ]
                    }
                  ]
                }
              ]
            },
            actions: []
          }
        ]
      });

      $controller('PolicyViolationsController', {
        $scope: _scope,
        PolicyViolationData: {
          hash: "1",
          appId: "appId"
        }
      });

      $httpBackend.flush();
    }));

    it('Open Add Waiver', inject(function ($modal) {
      var modalSpy = spyOn($modal, 'open');
      _scope.waiveComponent(_scope.policyAlerts[0]);

      expect(modalSpy).toHaveBeenCalledWith({
        templateUrl : 'add-waiver-modal-tmpl',
        controller : 'AddWaiverController',
        backdrop : 'static',
        keyboard : false,
        resolve : {
          policy : jasmine.any(Function)
        }
      });
    }));

    it('Open View Waiver', inject(function ($modal) {
      var modalSpy = spyOn($modal, 'open');
      _scope.viewWaivers();

      expect(modalSpy).toHaveBeenCalledWith({
        templateUrl : 'view-waivers-modal-tmpl',
        controller : 'ViewWaiverController',
        backdrop : 'static',
        keyboard : false
      });
    }));
  });

  describe('AddWaiverController', function () {

    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend) {
      _scope = $rootScope.$new();
      _scope.$close = angular.noop;
      _scope.$dismiss = angular.noop;

      $httpBackend.expectGET(SpecUtil.toRegExp('../brain/rest/policyWaiver/application/appId/applicable/context/policyId')).respond({
        id: 'orgId',
        name: 'org',
        type: 'organization',
        children: [
          {
            id: 'appId',
            name: 'app',
            type: 'application',
            children: null
          }
        ]
      });

      $controller('AddWaiverController', {
        $scope: _scope,
        PolicyViolationData: {
          hash: "1",
          appId: "appId"
        },
        policy : {
          id: 'policyId'
        }
      });
      $httpBackend.flush();
    }));

    it('Test waive policy at org level', inject(function($httpBackend) {
      var modalSpy = spyOn(_scope, '$close');

      _scope.$apply(function () {
        _scope.waiver.ownerId = 'orgId';
        _scope.waiver.comment = 'this is my comment!';
        _scope.owner.type = 'organization';
      });

      $httpBackend.expectPOST('../brain/rest/policyWaiver/organization/orgId', {
        hash: "1",
        ownerId : 'orgId',
        policyId: "policyId",
        comment: "this is my comment!"
      }).respond({});

      _scope.acceptWaiveComponent();

      $httpBackend.flush();
      expect(modalSpy).toHaveBeenCalled();
    }));

    it('Test waive policy at app level', inject(function($httpBackend) {
      var modalSpy = spyOn(_scope, '$close');

      _scope.$apply(function () {
        _scope.waiver.ownerId = 'appId';
        _scope.waiver.comment = 'this is my comment!';
        _scope.owner.type = 'application';
      });

      $httpBackend.expectPOST('../brain/rest/policyWaiver/application/appId', {
        hash: "1",
        ownerId : 'appId',
        policyId: "policyId",
        comment: "this is my comment!"
      }).respond({});

      _scope.acceptWaiveComponent();

      $httpBackend.flush();
      expect(modalSpy).toHaveBeenCalled();
    }));
  });

  describe('ViewWaiverController', function () {

    // setup our http backend to return what we want
    beforeEach(inject(function($rootScope, $controller, $httpBackend) {
      _viewScope = $rootScope.$new();
      $controller('ViewWaiverController', {
        $scope: _viewScope,
        global: {},
        PolicyViolationData: {
          hash: "1",
          appId: "appId"
        }
      });
    }));

    it('Validate data in scope', inject(function($httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/appId/component/1')).respond({
        waiversByOwner: [
          {
            ownerId: "appId",
            ownerName: "ownerName",
            ownerType: "application",
            waivers: [
              {
                id: "id",
                hash: "1234",
                policyId: "policyId",
                policyName: "policyName",
                constraintId: null,
                ownerId: "appId",
                comment: "some comment",
                createTime: 1375366539817
              }
            ]
          }
        ]
      });
      $httpBackend.flush();

      expect(_viewScope.waivers.length).toEqual(1);
      expect(_viewScope.waivers[0].id).toEqual("id");
      expect(_viewScope.waivers[0].hash).toEqual("1234");
      expect(_viewScope.waivers[0].policyId).toEqual("policyId");
      expect(_viewScope.waivers[0].policyName).toEqual("policyName");
      expect(_viewScope.waivers[0].constraintId).toEqual(null);
      expect(_viewScope.waivers[0].ownerId).toEqual("appId");
      expect(_viewScope.waivers[0].comment).toEqual("some comment");
      expect(_viewScope.waivers[0].createTime).toEqual(1375366539817);
    }));

    it('Validate delete waiver', inject(function($httpBackend) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLM.path + 'rest/policyWaiver/application/appId/component/1')).respond({
        waiversByOwner: [
          {
            ownerId: "appId",
            ownerName: "ownerName",
            ownerType: "application",
            waivers: [
              {
                id: "id",
                hash: "1234",
                policyId: "policyId",
                policyName: "policyName",
                constraintId: null,
                ownerId: "appId",
                comment: "some comment",
                createTime: 1375366539817
              }
            ]
          }
        ]
      });
      $httpBackend.flush();

      _viewScope.remove(_viewScope.waivers[0]);
      expect(_viewScope.confirmDelete).toEqual(_viewScope.waivers[0]);

      $httpBackend.expectDELETE(CLM.path + 'rest/policyWaiver/application/appId/id').respond(200);
      _viewScope.removeWaiver();
      $httpBackend.flush();
      expect(_viewScope.confirmDelete).toEqual(null);
      expect(_viewScope.waivers.length).toEqual(0);
    }));
  });

});