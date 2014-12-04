var clmEndpointTemplate = {
        openView : angular.noop,
        type : 'ide'
    },
    clmEndpoint = angular.copy(clmEndpointTemplate);

(function () {
  'use strict';

  describe('CIP Tests', function () {
    beforeEach(module('CIP', 'ComponentName'));

    afterEach(function () {
      clmEndpoint = angular.copy(clmEndpointTemplate);
      document.cookie = 'clmAppId=; expires=Thu, 01-Jan-70 00:00:01 GMT;';
    });

    describe('Rest endpoints use base url', function() {

      var gav = {
        groupId: 'gid',
        artifactId: 'aid',
        version: '1',
        hash: '12345678901234567890',
        matchState: 'similar'
      };

      var bacon = /^\/bacon\//;

      function shouldStartWithBacon(f) {
        if (f.length === 0) {
          expect(f()).toMatch(bacon)
        }
        else if (f.length === 1) {
          expect(f(gav)).toMatch(bacon)
        }
        else {
          console.log("Not yet programmed to deal with the args of: " + f);
        }
      }

      function checkObjectAndSubObjectsForEnoughBacon(o) {
        for (var f in o) {
          if (o.hasOwnProperty(f)) {
            if (f.match(/^get/) && f !== "getVersion" && typeof(o[f]) === 'function') {
              shouldStartWithBacon(o[f]);
            }
            else if (typeof(o[f]) === "object") {
              checkObjectAndSubObjectsForEnoughBacon(o[f]);
            }
          }
        }
      }

      it('All getters should start with the base path of bacon', inject(function($httpBackend) {
        Brain.setBasePath('/bacon/');
        checkObjectAndSubObjectsForEnoughBacon(Brain);
      }));

    });

    describe('setLogger', function () {
      afterEach(function () {
        Insight.resetLogger();
      });

      it('Exceptions before registration are logged', inject(function($exceptionHandler) {
        var spy = jasmine.createSpy('logger');
        $exceptionHandler(new Error('foo'));
        Insight.setLogger(spy);

        // wait for an async call
        waitsFor(function () {
          return spy.callCount > 0;
        }, "log to have been called", 10);

        runs(function () {
          expect(spy).toHaveBeenCalled();
          expect(spy.argsForCall[0][0]).toMatch(/Error\: foo.*/);
        });
      }));

      it('Exceptions after registration are logged', inject(function($exceptionHandler) {
        var spy = jasmine.createSpy('logger');
        Insight.setLogger(spy);
        $exceptionHandler(new Error('foo'));

        expect(spy).toHaveBeenCalled();
        expect(spy.argsForCall[0][0]).toMatch(/Error\: foo.*/);
      }));
    });

    describe('Coordinates', function () {
      it('setCoordinates', inject(function (Coordinates) {
        Coordinates.set('maven', {
          id : 'setCoordinates'
        });

        expect(Coordinates.get()).toEqual({ id : 'setCoordinates' });
        expect(Coordinates.getSelected()).toEqual({ id : 'setCoordinates' });
        expect(Coordinates.getFormat()).toEqual('maven');
      }));

      it('Insight.setCoordinates', inject(function (Coordinates, Properties) {
        Insight.setCoordinates('maven', {
          id : 'setCoordinates'
        }, { matchState : 'exact', proprietary : true , filename : 'foo.jar', hash : 'abc123'});

        expect(Coordinates.get()).toEqual({ id : 'setCoordinates' });
        expect(Coordinates.getSelected()).toEqual({ id : 'setCoordinates' });
        expect(Coordinates.getFormat()).toEqual('maven');

        expect(Properties.getMatchState()).toEqual('exact');
        expect(Properties.getProprietary()).toEqual(true);
        expect(Properties.getFilename()).toEqual('foo.jar');
        expect(Properties.getHash()).toEqual('abc123');
      }));

      it('Insight.setCoordinates with unknown', inject(function (Coordinates, Properties) {
        Insight.setCoordinates(null, null,
          { matchState : 'unknown', proprietary : false , filename : 'foo.jar', hash : 'abc123'});

        expect(Coordinates.get()).toEqual({ });
        expect(Coordinates.getSelected()).toEqual({  });
        expect(Coordinates.getFormat()).toEqual(null);

        expect(Properties.getMatchState()).toEqual('unknown');
        expect(Properties.getProprietary()).toEqual(false);
        expect(Properties.getFilename()).toEqual('foo.jar');
        expect(Properties.getHash()).toEqual('abc123');
      }));

      it('Selected', inject(function (Coordinates) {
        var gav = {
          groupId : 'gid',
          artifactId : 'aid',
          version : '1',
          hash : '12345678901234567890',
          matchState : 'similar'
        }, sel = {
          groupId : 'gid',
          artifactId : 'aid',
          version : '2'
        };
        Coordinates.set('maven', gav);
        Coordinates.setSelected(sel);
        expect(Coordinates.get()).toEqual(gav);
        expect(Coordinates.getSelected()).toEqual(sel);
        Coordinates.setSelected(angular.extend({}, sel, { version : gav.version }));
        expect(Coordinates.getSelected()).toEqual(gav);
        expect(Coordinates.getFormat()).toEqual('maven');
      }));

      it('Insight.setGAV', inject(function (Coordinates, Properties) {
        spyOn(Coordinates, 'set').andCallThrough();
        Insight.setGav({
          groupId : 'g1',
          artifactId : 'a1',
          version : 'v1',
          classifier : 'war',
          hash : '01234',
          proprietary : false,
          matchState : 'similar',
          filename : 'foo.war'
        });

        expect(Coordinates.set).toHaveBeenCalledWith('maven', { groupId : 'g1', artifactId : 'a1', version : 'v1', classifier : 'war' });
        expect(Coordinates.get()).toEqual({ groupId : 'g1', artifactId : 'a1', version : 'v1', classifier : 'war' });
        expect(Coordinates.getFormat()).toEqual('maven');

        expect(Properties.getHash()).toEqual('01234');
        expect(Properties.getFilename()).toEqual('foo.war');
        expect(Properties.getMatchState()).toEqual('similar');
        expect(Properties.getProprietary()).toEqual(false);
      }));

      it('Insight.clearGAV', inject(function (Coordinates, State) {
        Coordinates.set({
          id : 'clearGAV'
        });
        spyOn(Coordinates, 'set').andCallThrough();
        spyOn(State, 'set').andCallThrough();

        Insight.clearGav();

        expect(Coordinates.set).toHaveBeenCalledWith(null);
        expect(Coordinates.get()).toEqual(null);
        expect(Coordinates.getSelected()).toEqual(null);
        expect(Coordinates.getFormat()).toEqual(null);
        expect(State.set).toHaveBeenCalledWith(null, undefined);
      }));
    });

    describe('State', function () {
      describe('Insight.setError', function () {
        it('Invalid AppID', inject(function (State) {
          Insight.setError({
            errorCode : 404
          });
          expect(State.get()).toEqual('invalid-appid');
          expect(State.getArgs()).toEqual({
            errorCode : 404
          });
        }));
        it('Invalid Credentials', inject(function (State) {
          Insight.setError({
            errorCode : 401
          });
          expect(State.get()).toEqual('invalid-credentials');
          expect(State.getArgs()).toEqual({
            errorCode : 401
          });
        }));
        it('Failure', inject(function (State) {
          Insight.setError({
            errorCode : 444
          });
          expect(State.get()).toEqual('failure');
          expect(State.getArgs()).toEqual({
            errorCode : 444
          });
        }));
      });

      it('Insight.setFiltered', inject(function (State) {
        Insight.setFiltered('foo');
        expect(State.get()).toEqual('filtered');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setPending', inject(function (State) {
        Insight.setPending('foo');
        expect(State.get()).toEqual('pending');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setUnassigned', inject(function (State) {
        Insight.setUnassigned('foo');
        expect(State.get()).toEqual('unassigned');
        expect(State.getArgs()).toEqual('foo');
      }));
    });

    describe('SelectedApp', function () {
      describe('IDE Mode', function () {
        it ('Retrieves from setGav', inject(function (SelectedApp) {
          Insight.setGav({
            appId : 'foo'
          });
          expect(SelectedApp.get()).toEqual('foo');
        }));
        it ('Retrieves from setCoordinates', inject(function (SelectedApp) {
          Insight.setCoordinates('maven', {}, {
            appId : 'foo'
          });
          expect(SelectedApp.get()).toEqual('foo');
        }));

        it ('Doesn\'t Persist', inject(function (SelectedApp) {
          SelectedApp.set('foo');
          expect(document.cookie.indexOf('foo')).toEqual(-1);
        }));

        it ('Doesn\'t Use Cookie', inject(function (SelectedApp) {
          document.cookie = 'clmAppId=bar';
          expect(SelectedApp.get()).toBeFalsy();
        }));
      });

      describe('RM Mode', function () {
        var oldVal = null;
        beforeEach(function () {
          oldVal = clmEndpoint.selectApplication;
          clmEndpoint.selectApplication = true;
        });
        afterEach(function () {
          clmEndpoint.selectApplication = oldVal;
        });
        it ('Loads from cookie', inject(function (SelectedApp) {
          document.cookie = 'clmAppId=bar';
          expect(SelectedApp.get()).toEqual('bar');
        }));

        it ('Saves to cookie', inject(function (SelectedApp) {
          SelectedApp.set('save');
          expect(document.cookie).toEqual('clmAppId=save');
        }));
      });
    });

    function createApplicationsTests(type) {
      return function () {
        describe('Applications', function () {
          beforeEach(function () {
            clmEndpoint.type = type;
          });

          it('Success', inject(function (Applications, $httpBackend, $rootScope) {
            var applications = null;

            Applications.get().then(function (data) {
              applications = data;
            });

            $httpBackend.expectGET().respond({
              'myAppId' : 'My First App'
            });
            $httpBackend.flush();
            $rootScope.$apply();
            expect(applications).toEqual({ 'myAppId' : 'My First App' });
          }));

          it('Error', inject(function (Applications, $httpBackend, $rootScope) {
            var error = null;

            Applications.get().then(angular.noop, function (data) {
              error = data;
            });

            $httpBackend.expectGET().respond(404, 'fail');
            $httpBackend.flush();
            $rootScope.$apply();
            expect(error).toEqual(['fail', 404, jasmine.any(Function), jasmine.any(Object)]);
          }));
        });
      };
    }

    describe('IDE', createApplicationsTests('ide'));

    describe('Nexus', createApplicationsTests('nexus'));

    describe('ComponentController', function () {
      var scope = null;

      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('ComponentController', {
          $scope : scope
        });
      }));

      it('Http Requests', inject(function ($httpBackend) {
        var gav = {
          groupId : 'foo',
          artifactId : 'bar',
          version : '1',
          proprietary : true
        };
        clmEndpoint.selectApplication = true;

        Insight.setGav(gav);

        $httpBackend.verifyNoOutstandingRequest();

        Insight.clearGav();
        scope.$apply(function () {
          document.cookie = 'clmAppId=myFirstApp';
        });
        $httpBackend.verifyNoOutstandingRequest();

        spyOn(Brain[clmEndpoint.type], 'getComponentListUrl').andReturn('foo');
        $httpBackend.expectGET('foo').respond({ list: [ {} ] });
        Insight.setGav(gav);
        $httpBackend.flush();
        expect(scope.componentDetailsList).not.toBeNull();
        expect(scope.componentDetailsList.length).toEqual(1);
      }));
    });

    describe('DetailsController', function () {
      var scope = null;

      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('DetailsController', {
          $scope : scope
        });
      }));

      afterEach(function () {
        scope.$destroy();
      });

      it('Http Requests', inject(function ($httpBackend, Coordinates, Properties) {
        var gav = {
          groupId : 'foo',
          artifactId : 'bar',
          version : '1',
          hash : '01234',
          proprietary : true,
          matchState : 'similar'
        };
        clmEndpoint.selectApplication = true;

        Insight.setGav(gav);

        $httpBackend.verifyNoOutstandingRequest();

        Insight.clearGav();
        scope.$apply(function () {
          document.cookie = 'clmAppId=myFirstApp';
        });
        $httpBackend.verifyNoOutstandingRequest();

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').andReturn('foo');
        $httpBackend.expectGET('foo').respond({ securityVulnerabilities : [], policyAlerts: [] });
        Insight.setGav(angular.extend({ matchState : 'similar' }, gav));
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getComponentUrl).toHaveBeenCalledWith('myFirstApp', 'maven', '01234', 'similar', true, {
          groupId : 'foo',
          artifactId : 'bar',
          version : '1'
        });

        // Another version selected
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities: [],
          policyAlerts: []
        });
        scope.$apply(function () {
          Coordinates.setSelected({ groupId : 'foo', artifactId : 'bar', version : '2' });
        });
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getComponentUrl).toHaveBeenCalledWith('myFirstApp', 'maven', '01234', 'similar', true, {
          groupId : 'foo',
          artifactId : 'bar',
          version : '2'
        });

        // Unknown GAV
        scope.$apply(function () {
          Coordinates.set('maven', { groupId : 'foo', artifactId : 'bar', version : 1});
          Properties.setMatchState('unknown');
        });
        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('isManual', inject(function ($httpBackend) {
        expect(scope.isManual()).toBeFalsy();

        scope.componentDetails = {
          identificationSource : 'Manual'
        };
        expect(scope.isManual()).toBeTruthy();

        scope.componentDetails = {
          identificationSource : 'Sonatype'
        };
        expect(scope.isManual()).toBeFalsy();
      }));

      it('canMigrate', inject(function (Coordinates) {
        var gav = {
              groupId : 'foo',
              artifactId : 'bar',
              version : '1'
            },
            selected = angular.copy(gav);

        expect(scope.canMigrate()).toBeFalsy();

        spyOn(Coordinates, 'get').andReturn(gav);
        spyOn(Coordinates, 'getSelected').andReturn(selected);

        expect(scope.canMigrate()).toBeFalsy();

        selected.version = '2';
        expect(scope.canMigrate()).toBeTruthy();
      }));

      it('getMaximumSeverity', inject(function($httpBackend, Coordinates, SelectedApp) {
        scope.componentDetails = {
          securityVulnerabilities : []
        };
        expect(scope.getMaximumSeverity()).toEqual('NA');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : null }]
        };
        expect(scope.getMaximumSeverity()).toEqual('Unscored');

        var gav = {
           groupId : 'groupId',
           artifactId : 'artifactId',
           version : 'version'
        };

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').andReturn('foo');
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities : [{ severity : null }, { severity : 2 }, { severity : 9 }, { severity : 8 }],
          policyAlerts: []
        });

        scope.$apply(function () {
          Coordinates.set('maven', gav);
          SelectedApp.set('appId');
        });
        $httpBackend.flush();

        expect(scope.getMaximumSeverity()).toEqual(9);
      }));

      it('getColorClass', inject(function($httpBackend, Coordinates) {
        scope.componentDetails = {
          securityVulnerabilities : []
        };
        expect(scope.getColorClass()).toEqual(' unspecified');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : null }]
        };
        expect(scope.getColorClass()).toEqual(' moderate');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 0 }]
        };
        expect(scope.getColorClass()).toEqual(' moderate');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 4 }]
        };
        expect(scope.getColorClass()).toEqual(' severe');

        scope.componentDetails = {
          securityVulnerabilities : [{ severity : 7 }]
        };
        expect(scope.getColorClass()).toEqual(' critical');
      }));

      it('calculates highestPolicyThreat', inject(function($httpBackend, Coordinates, SelectedApp) {
        var gav = {
          groupId : 'groupId',
          artifactId : 'artifactId',
          version : 'version'
        };
        expect(scope.highestPolicyThreat).toEqual(null);

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').andReturn('foo');
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities : [],
          policyAlerts: [{
            trigger: {
              policyName: 'foo',
              threatLevel: 1
            }
           }, {
            trigger: {
              policyName: 'bar',
              threatLevel: 10
            }
          }]
        });

        scope.$apply(function () {
          Coordinates.set('maven', gav);
          SelectedApp.set('appId');
        });
        $httpBackend.flush();

        expect(scope.highestPolicyThreat.level).toEqual(10);
        expect(scope.highestPolicyThreat.violatedPolicies).toEqual(2);
      }));
    });

    describe('graph', function () {
      var scope = null,
          parentScope = null;

      beforeEach(inject(function ($compile, $rootScope, Coordinates) {
        spyOn(Insight, 'ComponentInformation').andReturn(undefined);

        parentScope = $rootScope.$new();
        parentScope.componentDetails = [{
          "componentIdentifier" : { "coordinates" : { "version" : "sources" } },
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.0.4" } },
          "popularity": 4,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.0.6" } },
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.1.9" } },
          "popularity": 13,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.1.31" } },
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.1.34" } },
          "popularity": 0,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "4.1.36" } },
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.0.16" } },
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.0.18" } },
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.0.28" } },
          "popularity": 67,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.4" } },
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.7-alpha" } },
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.7" } },
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.8-alpha" } },
          "popularity": 1,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.9-alpha" } },
          "popularity": 2,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.9" } },
          "popularity": 8,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.12" } },
          "popularity": 3,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["NOT-PROVIDED"],
          "effectiveLicenseThreat": "NOT-PROVIDED",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.15" } },
          "popularity": 14,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["NOT-PROVIDED"],
          "declaredLicenseThreats": ["LIBERAL"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"]
        }, {
          "componentIdentifier" : { "coordinates" : { "version" : "5.5.23" } },
          "popularity": 100,
          "majorRevisionStep": false,
          "observedLicenseThreats": ["LIBERAL"],
          "declaredLicenseThreats": ["LIBERAL"],
          "effectiveLicenseThreat": "LIBERAL",
          "securityThreats": ["Moderate", "Severe"],
          "website": "http://tomcat.apache.org/"
        }];

        Coordinates.set('maven', {
          version : '5.0.24'
        });

        $compile('<div graph="componentDetails"></div>')(parentScope);
        parentScope.$apply();
      }));

      afterEach(function () {
        parentScope.$destroy();
      });

      it('Version Click', inject(function (Coordinates) {
         Insight.ComponentInformation.mostRecentCall.args[0].versionClick('5.5.23');
         expect(Coordinates.getSelected()).toEqual({ "version" : "5.5.23" });
      }));

      it('Double Version Click', inject(function ($rootScope) {
        var version = null;
        parentScope.$on('viewDetails', function (event, v) {
          version = v;
        });
        Insight.ComponentInformation.mostRecentCall.args[0].versionDblClick('5.5.23');

        expect(version).toEqual('5.5.23');
      }));
    });
  });
}());
