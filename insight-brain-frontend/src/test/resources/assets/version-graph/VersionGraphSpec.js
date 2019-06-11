var clmEndpointTemplate = {
      openView: angular.noop,
      type: 'ide',
      viewDetails: true,
      migrate: true
    },
    clmEndpoint = angular.copy(clmEndpointTemplate);

(function() {
  'use strict';

  describe('CIP Tests', function() {
    beforeEach(module('version.graph.app', function($provide) {
      $provide.service('pendoService', function() {
        return jasmine.createSpyObj(['start']);
      });
    }));

    afterEach(function() {
      clmEndpoint = angular.copy(clmEndpointTemplate);
      document.cookie = 'clmAppId=; expires=Thu, 01-Jan-70 00:00:01 GMT;';
    });

    describe('when in NXRM', function() {
      var oldClmEndpointType;

      beforeEach(module(function() {
        oldClmEndpointType = clmEndpoint.type;

        // this needs to be set before the `run` block runs, which is why we set it in a provider function
        clmEndpoint.type = 'rm';
      }));

      afterEach(function() {
        clmEndpoint.type = oldClmEndpointType;
      });

      it('starts pendo immediately', inject(function(pendoService) {
        expect(pendoService.start).toHaveBeenCalled();
      }));
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
            if (f.match(/^get.*Url$/) && f !== "getCurrentReportReevaluateUrl" && typeof(o[f]) === 'function') {
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

    describe('setLogger', function() {
      afterEach(function() {
        Insight.resetLogger();
      });

      it('Exceptions before registration are logged', function(done) {
        inject(function($exceptionHandler) {
          var spy = jasmine.createSpy('logger');
          $exceptionHandler(new Error('foo'));
          Insight.setLogger(spy);

          var interval = setInterval(function() {
            if (spy.calls.count() > 0) {
              clearInterval(interval);
              expect(spy).toHaveBeenCalled();
              expect(spy.calls.first().args[0]).toMatch(/Error\: foo.*/);
              done();
            }
          }, 10);
        });
      });

      it('Exceptions after registration are logged', inject(function($exceptionHandler) {
        var spy = jasmine.createSpy('logger');
        Insight.setLogger(spy);
        $exceptionHandler(new Error('foo'));

        expect(spy).toHaveBeenCalled();
        expect(spy.calls.first().args[0]).toMatch(/Error\: foo.*/);
      }));
    });

    describe('Coordinates', function() {
      it('setCoordinates', inject(function(Coordinates) {
        Coordinates.set('maven', {
          id: 'setCoordinates'
        });

        expect(Coordinates.get()).toEqual({id: 'setCoordinates'});
        expect(Coordinates.getSelected()).toEqual({id: 'setCoordinates'});
        expect(Coordinates.getFormat()).toEqual('maven');
      }));

      it('Insight.setCoordinates', inject(function(Coordinates, Properties) {
        Insight.setCoordinates('maven', {
          id: 'setCoordinates'
        }, {matchState: 'exact', proprietary: true, filename: 'foo.jar', hash: 'abc123'});

        expect(Coordinates.get()).toEqual({id: 'setCoordinates'});
        expect(Coordinates.getSelected()).toEqual({id: 'setCoordinates'});
        expect(Coordinates.getFormat()).toEqual('maven');

        expect(Properties.getMatchState()).toEqual('exact');
        expect(Properties.getProprietary()).toEqual(true);
        expect(Properties.getFilename()).toEqual('foo.jar');
        expect(Properties.getHash()).toEqual('abc123');
      }));

      it('Insight.setCoordinates with unknown', inject(function(Coordinates, Properties) {
        Insight.setCoordinates(null, null,
            {matchState: 'unknown', proprietary: false, filename: 'foo.jar', hash: 'abc123'});

        expect(Coordinates.get()).toEqual({});
        expect(Coordinates.getSelected()).toEqual({});
        expect(Coordinates.getFormat()).toEqual(null);

        expect(Properties.getMatchState()).toEqual('unknown');
        expect(Properties.getProprietary()).toEqual(false);
        expect(Properties.getFilename()).toEqual('foo.jar');
        expect(Properties.getHash()).toEqual('abc123');
      }));

      it('Selected', inject(function(Coordinates) {
        var gav = {
          groupId: 'gid',
          artifactId: 'aid',
          version: '1'
        }, sel = {
          groupId: 'gid',
          artifactId: 'aid',
          version: '2'
        };
        Coordinates.set('maven', gav);

        Coordinates.setSelected(sel);
        expect(Coordinates.get()).toEqual(gav);
        expect(Coordinates.getSelected()).toEqual(sel);

        Coordinates.setSelected(angular.extend({}, sel, {version: gav.version}));
        expect(Coordinates.getSelected()).toEqual(gav);
        expect(Coordinates.getFormat()).toEqual('maven');
      }));

      it('Insight.setGAV', inject(function(Coordinates, Properties) {
        spyOn(Coordinates, 'set').and.callThrough();
        Insight.setGav({
          groupId: 'g1',
          artifactId: 'a1',
          version: 'v1',
          classifier: 'war',
          hash: '01234',
          proprietary: false,
          matchState: 'similar',
          filename: 'foo.war'
        });

        expect(Coordinates.set).toHaveBeenCalledWith('maven',
            {groupId: 'g1', artifactId: 'a1', version: 'v1', classifier: 'war'});
        expect(Coordinates.get()).toEqual({groupId: 'g1', artifactId: 'a1', version: 'v1', classifier: 'war'});
        expect(Coordinates.getFormat()).toEqual('maven');

        expect(Properties.getHash()).toEqual('01234');
        expect(Properties.getFilename()).toEqual('foo.war');
        expect(Properties.getMatchState()).toEqual('similar');
        expect(Properties.getProprietary()).toEqual(false);
      }));

      it('Insight.clearGAV', inject(function(Coordinates, State, Properties) {
        spyOn(Coordinates, 'set').and.callThrough();
        spyOn(State, 'set').and.callThrough();
        Insight.setGav({
          groupId: 'g1',
          artifactId: 'a1',
          version: 'v1',
          classifier: 'war',
          hash: '01234',
          proprietary: false,
          matchState: 'similar',
          filename: 'foo.war'
        });

        expect(Coordinates.set).toHaveBeenCalledWith('maven',
            {groupId: 'g1', artifactId: 'a1', version: 'v1', classifier: 'war'});
        expect(Coordinates.get()).toEqual({groupId: 'g1', artifactId: 'a1', version: 'v1', classifier: 'war'});
        expect(Coordinates.getFormat()).toEqual('maven');

        expect(Properties.getHash()).toEqual('01234');
        expect(Properties.getFilename()).toEqual('foo.war');
        expect(Properties.getMatchState()).toEqual('similar');
        expect(Properties.getProprietary()).toEqual(false);

        Insight.clearGav();

        expect(Coordinates.set).toHaveBeenCalledWith(null);
        expect(Coordinates.get()).toBeFalsy();
        expect(Coordinates.getSelected()).toBeFalsy();
        expect(Coordinates.getFormat()).toEqual(null);
        expect(State.set).toHaveBeenCalledWith(null, undefined);
        expect(Properties.getFilename()).toBeUndefined();
        expect(Properties.getHash()).toBeUndefined();
        expect(Properties.getMatchState()).toBeUndefined();
        expect(Properties.getProprietary()).toBeUndefined();
        expect(Properties.isUnknown()).toBeFalsy();
      }));
    });

    describe('setHeaders', function() {
      var pendoService,
          $httpBackend,
          CLMLocations,
          oldClmEndpointType,
          authHeader = 'Basic asdf';

      function headerExpectation(headers) {
        return headers.Authorization === authHeader;
      }

      function setupLoginExpectations($httpBackend, CLMLocations) {
        $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond(204);
        $httpBackend.expectPOST(CLMLocations.getSessionUrl(), undefined, headerExpectation).respond(200);
      }

      beforeEach(inject(function(_pendoService_, _$httpBackend_, _CLMLocations_) {
        pendoService = _pendoService_;
        $httpBackend = _$httpBackend_;
        CLMLocations = _CLMLocations_;
      }));

      it('adds the specified headers to $http.defaults.headers.common', inject(function($http) {
        setupLoginExpectations($httpBackend, CLMLocations);

        // set up some pre-existing values
        $http.defaults.headers.common = {
          foo: 'bar',
          baz: 'asdf'
        };

        Insight.setHeaders({
          baz: 'qwerty',
          Authorization: authHeader
        });

        expect($http.defaults.headers.common).toEqual({
          foo: 'bar',
          baz: 'qwerty',
          Authorization: authHeader
        });
      }));

      it('creates a login session and then starts pendo', inject(function($rootScope) {
        setupLoginExpectations($httpBackend, CLMLocations);

        Insight.setHeaders({
          Authorization: authHeader
        });

        // process the logout
        $httpBackend.flush(1);

        expect(pendoService.start).not.toHaveBeenCalled();

        // process the login
        $httpBackend.flush();

        expect(pendoService.start).toHaveBeenCalled();
      }));
    });

    describe('State', function() {
      describe('Insight.setError', function() {
        it('Invalid AppID', inject(function(State) {
          Insight.setError({
            errorCode: 404
          });
          expect(State.get()).toEqual('invalid-appid');
          expect(State.getArgs()).toEqual({
            errorCode: 404
          });
        }));
        it('Invalid Credentials', inject(function(State) {
          Insight.setError({
            errorCode: 401
          });
          expect(State.get()).toEqual('invalid-credentials');
          expect(State.getArgs()).toEqual({
            errorCode: 401
          });
        }));
        it('Failure', inject(function(State) {
          Insight.setError({
            errorCode: 444
          });
          expect(State.get()).toEqual('failure');
          expect(State.getArgs()).toEqual({
            errorCode: 444
          });
        }));
      });

      it('Insight.setFiltered', inject(function(State) {
        Insight.setFiltered('foo');
        expect(State.get()).toEqual('filtered');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setPending', inject(function(State) {
        Insight.setPending('foo');
        expect(State.get()).toEqual('pending');
        expect(State.getArgs()).toEqual('foo');
      }));

      it('Insight.setUnassigned', inject(function(State) {
        Insight.setUnassigned('foo');
        expect(State.get()).toEqual('unassigned');
        expect(State.getArgs()).toEqual('foo');
      }));
    });

    describe('OwnerContext', function() {
      describe('IDE Mode', function() {
        it('Retrieves from setGav', inject(function(OwnerContext) {
          Insight.setGav({
            appId: 'foo'
          });
          expect(OwnerContext.ownerId).toEqual('foo');
        }));
        it('Retrieves from setCoordinates', inject(function(OwnerContext) {
          Insight.setCoordinates('maven', {}, {
            appId: 'foo'
          });
          expect(OwnerContext.ownerId).toEqual('foo');
        }));

        it('Doesn\'t Persist', inject(function(OwnerContext) {
          OwnerContext.setApplicationId('foo');
          expect(document.cookie.indexOf('foo')).toEqual(-1);
        }));

        it('Doesn\'t Use Cookie', inject(function(OwnerContext) {
          document.cookie = 'clmAppId=bar';
          expect(OwnerContext.ownerId).toBeFalsy();
        }));
      });

      describe('RM Mode', function() {
        var oldVal = null;
        beforeEach(function() {
          oldVal = clmEndpoint.selectApplication;
          clmEndpoint.selectApplication = true;
        });
        afterEach(function() {
          clmEndpoint.selectApplication = oldVal;
        });
        it('Loads from cookie', inject(function(OwnerContext, $rootScope) {
          $rootScope.$apply(function() {
            document.cookie = 'clmAppId=bar';
          });
          expect(OwnerContext.ownerId).toEqual('bar');
        }));

        it('Saves to cookie', inject(function(OwnerContext) {
          OwnerContext.setApplicationId('save');
          expect(document.cookie).toEqual('clmAppId=save');
        }));
      });
    });

    function createApplicationsTests(type) {
      return function() {
        describe('Applications', function() {
          beforeEach(function() {
            clmEndpoint.type = type;
          });

          it('Success', inject(function(Applications, $httpBackend, $rootScope) {
            var applications = null;

            Applications.get().then(function(data) {
              applications = data;
            });

            $httpBackend.expectGET(Brain.getIntegratorApplicationListUrl()).respond(200, {
              applicationSummaries: [
                {
                  publicId: 'myAppId',
                  name: 'My First App'
                }
              ]
            });
            $httpBackend.flush();
            $rootScope.$apply();
            expect(applications).toEqual([
              {
                publicId: 'myAppId',
                name: 'My First App'
              }
            ]);
          }));

          it('Error', inject(function(Applications, $httpBackend, $rootScope) {
            var error = null;

            Applications.get().then(angular.noop, function(data) {
              error = data;
            });

            $httpBackend.expectGET().respond(404, 'fail');
            $httpBackend.flush();
            $rootScope.$apply();
            expect(error).toBeDefined();
            expect(error.data).toEqual('fail');
            expect(error.status).toEqual(404);
            expect(error.headers).toEqual(jasmine.any(Function));
          }));
        });
      };
    }

    describe('IDE', createApplicationsTests('ide'));

    describe('Nexus', createApplicationsTests('nexus'));

    describe('ComponentController', function() {

      describe('initialization', function() {
        var scope = null;

        beforeEach(inject(function($controller, $rootScope) {
          clmEndpoint.selectApplication = true;
          scope = $rootScope.$new();
          $controller('ComponentController', {
            $scope: scope,
            'proprietary.matchers.modal': {},
            SelectedComponent: {}
          });
        }));

        it('sets componentDetailsList', inject(function($httpBackend, $rootScope) {
          var gav = {
            groupId: 'foo',
            artifactId: 'bar',
            version: '1',
            proprietary: true
          };

          Insight.setGav(gav);

          $httpBackend.verifyNoOutstandingRequest();

          Insight.clearGav();
          $rootScope.$apply(function() {
            document.cookie = 'clmAppId=myFirstApp';
          });
          $httpBackend.verifyNoOutstandingRequest();

          spyOn(Brain[clmEndpoint.type], 'getComponentListUrl').and.returnValue('foo');
          $httpBackend.expectGET('foo').respond([{}]);
          Insight.setGav(gav);
          $httpBackend.flush();
          expect(scope.componentDetailsList).not.toBeNull();
          expect(scope.componentDetailsList.length).toEqual(1);
        }));
      });

      describe('Add Proprietary Component Matchers', function() {
        var scope = null,
            properties,
            proprietaryModal,
            selectedComponent;

        beforeEach(function() {
          clmEndpoint.selectApplication = true;
          proprietaryModal = jasmine.createSpyObj('proprietaryModal', ['open']);
          selectedComponent = jasmine.createSpyObj('selectedComponent', ['get']);

          module('version.graph', function($provide) {
            $provide.value('proprietary.matchers.modal', proprietaryModal);
            $provide.value('SelectedComponent', selectedComponent);
          });

          inject(function($controller, $rootScope, Properties) {
            properties = Properties;
            scope = $rootScope.$new();
            $controller('ComponentController', {
              $scope: scope,
              OwnerContext: {
                ownerId: 'testParentApplication'
              }
            });
          });
        });

        describe('canShowAddProprietary()', function() {

          describe('when non maven pathnames present', function() {
            beforeEach(function() {
              // mock selected component with non maven pathnames present
              selectedComponent.get.and.returnValue({
                pathnames: ['foo', 'dependency:/baz', 'bar']
              });
            });

            describe('when clmEndpoint.canAddProprietary is undefined', function() {
              beforeEach(function() {
                clmEndpoint.canAddProprietary = undefined;
              });

              it('returns false if component is marked as proprietary', function() {
                properties.setProprietary(true);
                expect(scope.canShowAddProprietary()).toBe(false);
              });

              it('returns false if component is not marked as proprietary', function() {
                properties.setProprietary(false);
                expect(scope.canShowAddProprietary()).toBe(false);
              });
            });

            describe('when clmEndpoint.canAddProprietary is true', function() {
              beforeEach(function() {
                clmEndpoint.canAddProprietary = true;
              });

              it('returns false if component is marked as proprietary', function() {
                properties.setProprietary(true);
                expect(scope.canShowAddProprietary()).toBe(false);
              });

              it('returns  true if component is not marked as proprietary', function() {
                properties.setProprietary(false);
                expect(scope.canShowAddProprietary()).toBe(true);
              });
            });
          });

          describe('when non maven pathnames are not present', function() {
            beforeEach(function() {
              // mock selected component with non maven pathnames present
              selectedComponent.get.and.returnValue({
                pathnames: ['dependency:/foo', 'dependency:/baz', 'dependency:/bar']
              });
            });

            describe('when clmEndpoint.canAddProprietary is undefined', function() {
              beforeEach(function() {
                clmEndpoint.canAddProprietary = undefined;
              });

              it('returns false if component is marked as proprietary', function() {
                properties.setProprietary(true);
                expect(scope.canShowAddProprietary()).toBe(false);
              });

              it('returns false if component is not marked as proprietary', function() {
                properties.setProprietary(false);
                expect(scope.canShowAddProprietary()).toBe(false);
              });
            });

            describe('and clmEndpoint.canAddProprietary is true', function() {
              beforeEach(function() {
                clmEndpoint.canAddProprietary = true;
              });

              it('returns false  if component is marked as proprietary', function() {
                properties.setProprietary(true);
                expect(scope.canShowAddProprietary()).toBe(false);
              });

              it('returns false if component is not marked as proprietary', function() {
                properties.setProprietary(false);
                expect(scope.canShowAddProprietary()).toBe(false);
              });
            });
          });

        });

        describe('showAddProprietary()', function() {
          it('calls modal with owner Id and filtered pathnames', function() {

            // mock selected component
            selectedComponent.get.and.returnValue({
              pathnames: ['foo', 'dependency:/baz', 'bar']
            });

            scope.showAddProprietary();
            expect(proprietaryModal.open).toHaveBeenCalledWith('testParentApplication', ['foo', 'bar']);
          });
        })
      });

      describe('When coordinates changed', function (){
        var scope = null;

        beforeEach(inject(function($controller, $rootScope) {
          clmEndpoint.selectApplication = true;
          scope = $rootScope.$new();
          $controller('ComponentController', {
            $scope: scope,
            SelectedComponent: {}
          });
        }));

        var coords = {
          groupId: 'foo',
          artifactId: 'bar',
          version: '1',
          extension: 'jar',
          classifier: ''
        };

        var properties = {
          matchState: 'exact"',
          proprietary: 'false',
          filename: 'filename',
          hash: 'hash',
          appId: 'myFirstApp'
        };

        it('retrieves the application internal ID and suggested remediations',
            inject(function($httpBackend, $rootScope) {
          $httpBackend.verifyNoOutstandingRequest();
          spyOn(Brain, 'getInternalApplicationIdUrlForApplicationId').and.returnValue('bar');
          $httpBackend.expectGET('bar').respond({
            "applications": [
              {
                "id": "InternalAppId"
              }
            ]
          });

          spyOn(Brain, 'getSuggestedRemediationUrlForApplication').and.returnValue('foo');
          $httpBackend.expectPOST('foo').respond({
            "remediation" : {
              "versionChanges" : [ {
                "type" : "next-no-violations",
                "data" : {
                  "component" : {
                    "componentIdentifier" : "C1"
                  }
                }
              }, {
                "type": "next-non-failing",
                "data": {
                  "component": {
                    "componentIdentifier": "C2"
                  }
                }
              }]
            }
          });

          Insight.setCoordinates('maven', coords, properties);
          $httpBackend.flush();
          expect(Brain.getInternalApplicationIdUrlForApplicationId).toHaveBeenCalledWith('myFirstApp');
          expect(Brain.getSuggestedRemediationUrlForApplication).toHaveBeenCalledWith('InternalAppId');
          expect(scope.applicationInternalIds).not.toBeNull();
          expect(scope.applicationInternalIds.size).toEqual(1);
          expect(scope.applicationInternalIds.get('myFirstApp')).toEqual('InternalAppId');
          expect(scope.suggestedRemediations.size).toEqual(2);
          expect(scope.suggestedRemediations.has('next-non-failing'));
          expect(scope.suggestedRemediations.get('next-non-failing')).toEqual("C2");
          expect(scope.suggestedRemediations.has('next-no-violations'));
          expect(scope.suggestedRemediations.get('next-no-violations')).toEqual("C1");
        }));

        it('does not retrieve internal app ids for know apps', inject(function($httpBackend, $rootScope) {
          $httpBackend.verifyNoOutstandingRequest();
          spyOn(Brain, 'getInternalApplicationIdUrlForApplicationId');

          scope.applicationInternalIds = new Map();
          scope.applicationInternalIds.set('myFirstApp', 'InternalAppId')

          spyOn(Brain, 'getSuggestedRemediationUrlForApplication').and.returnValue('foo');
          $httpBackend.expectPOST('foo').respond({});

          Insight.setCoordinates('maven', coords, properties);
          $httpBackend.flush();
          expect(Brain.getInternalApplicationIdUrlForApplicationId).not.toHaveBeenCalled();
          expect(Brain.getSuggestedRemediationUrlForApplication).toHaveBeenCalledWith('InternalAppId');
          expect(scope.applicationInternalIds).not.toBeNull();
          expect(scope.applicationInternalIds.size).toEqual(1);
          expect(scope.applicationInternalIds.get('myFirstApp')).toEqual('InternalAppId');
        }));

        it('does not retrieve remediations when app id is invalid', inject(function($httpBackend, $rootScope) {
          $httpBackend.verifyNoOutstandingRequest();
          spyOn(Brain, 'getInternalApplicationIdUrlForApplicationId').and.returnValue('bar');
          $httpBackend.expectGET('bar').respond({
            "applications": []
          });
          spyOn(Brain, 'getSuggestedRemediationUrlForApplication');

          Insight.setCoordinates('maven', coords, properties);
          $httpBackend.flush();
          expect(Brain.getInternalApplicationIdUrlForApplicationId).toHaveBeenCalledWith('myFirstApp');
          expect(Brain.getSuggestedRemediationUrlForApplication).not.toHaveBeenCalled();
          expect(scope.applicationInternalIds).not.toBeNull();
          expect(scope.applicationInternalIds.size).toEqual(0);
        }));
      });

      describe('Suggested Remediations', function() {
        var scope = null;

        beforeEach(inject(function($controller, $rootScope) {
          clmEndpoint.selectApplication = true;
          scope = $rootScope.$new();
          $controller('ComponentController', {
            $scope: scope,
          });
          $controller;
        }));

        describe('Validate that remediation type exists and is valid', function (){
          it('Returns true when valid version of type exists', inject(function() {
            scope.suggestedRemediations = new Map();
            scope.suggestedRemediations.set('next-no-violations', {coordinates:{version:'3'}});
            scope.suggestedRemediations.set('next-non-failing', {coordinates:{version:'2'}});
            scope.coordinates = {coordinates:{version:'1'}};
            expect(scope.nextNoViolationAvailableAndNotCurrent()).toBeTruthy();
            expect(scope.nextNoFailAvailableAndNotCurrent()).toBeTruthy();
          }));

          it('Returns false when same version of type exists', inject(function() {
            scope.suggestedRemediations = new Map();
            scope.suggestedRemediations.set('next-no-violations', {coordinates:{version:'1'}});
            scope.suggestedRemediations.set('next-non-failing', {coordinates:{version:'1'}});
            scope.coordinates = {coordinates:{version:'1'}};
            expect(scope.nextNoViolationAvailableAndNotCurrent()).toBeFalsy();
            expect(scope.nextNoFailAvailableAndNotCurrent()).toBeFalsy();
          }));

          it('Returns false when no version of type exists', inject(function() {
            scope.suggestedRemediations = new Map();
            scope.suggestedRemediations.set('invalid-type', {coordinates:{version:'2'}});
            scope.coordinates = {coordinates:{version:'1'}};
            expect(scope.nextNoViolationAvailableAndNotCurrent()).toBeFalsy();
            expect(scope.nextNoFailAvailableAndNotCurrent()).toBeFalsy();
          }));
        });

        describe('When marking next suggested version', function () {
          describe('If suggested version exists in component details list', function(){

            beforeEach(function() {
              scope.suggestedRemediations = new Map();
              scope.suggestedRemediations.set('next-no-violations', {coordinates:{version:'2'}});
              scope.suggestedRemediations.set('next-non-failing', {coordinates:{version:'3'}});
              scope.coordinates = {coordinates:{version:'1'}};
              scope.componentDetailsList = [
                {componentIdentifier: {coordinates: {version: '1'}}},
                {componentIdentifier: {coordinates: {version: '2'}}},
                {componentIdentifier: {coordinates: {version: '3'}}}
              ];
            });

            describe('For next-no-violations', function(){

              beforeEach(function() {
                spyOn(Insight, 'updateBars');
                scope.markNextNoViolation();
              });

              it('Coordinates are updated', inject(function(Coordinates) {
                expect(Coordinates.getSelected().version).toEqual('2');
              }));

              it('Bars are updated', function () {
                expect(Insight.updateBars).toHaveBeenCalledWith(1);
              });
            });

            describe('For next-no-fail', function(){

              beforeEach(function() {
                spyOn(Insight, 'updateBars');
                scope.markNextNoFail();
              });

              it('updates the coordinates', inject(function(Coordinates) {
                expect(Coordinates.getSelected().version).toEqual('3');
              }));

              it('updates the bars', function() {
                expect(Insight.updateBars).toHaveBeenCalledWith(2);
              });
            });
          });

          describe('If suggested version does not exist in component details list', function(){

            beforeEach(inject(function(Coordinates) {
              Coordinates.setSelected({version:'1'});
              scope.suggestedRemediations = new Map();
              scope.suggestedRemediations.set('next-no-violations', {coordinates:{version:'2'}});
              scope.suggestedRemediations.set('next-non-failing', {coordinates:{version:'3'}});
              scope.coordinates = {coordinates:{version:'1'}};
              scope.componentDetailsList = [
                {componentIdentifier: {coordinates: {version: '1'}}},
                {componentIdentifier: {coordinates: {version: '4'}}}
              ];
            }));

            describe('For next-no-violations', function(){

              beforeEach(function() {
                spyOn(Insight, 'updateBars');
                scope.markNextNoViolation();
              });

              it('does not update coordinates', inject(function(Coordinates) {
                expect(Coordinates.getSelected().version).toEqual('1');
              }));

              it('does not update bars', function() {
                expect(Insight.updateBars).not.toHaveBeenCalled;
              });
            })

            describe('For next-no-fail', function(){

              beforeEach(function() {
                spyOn(Insight, 'updateBars');
                scope.markNextNoFail();
              });

              it('does not not update coordinates', inject(function(Coordinates) {
                expect(Coordinates.getSelected().version).toEqual('1');
              }));

              it('does not update bars', function() {
                expect(Insight.updateBars).not.toHaveBeenCalled;
              });
            })
          });
        });

        describe('When fetching next suggested version', function () {
          describe('If suggested version type is available', function() {
            beforeEach(function() {
              scope.suggestedRemediations = new Map();
              scope.suggestedRemediations.set('next-no-violations', {coordinates:{version:'2'}});
              scope.suggestedRemediations.set('next-non-failing', {coordinates:{version:'3'}});
              scope.coordinates = {coordinates:{version:'1'}};
            });

            describe('For next-no-violations', function(){
              it('returns the correct version (2)', inject(function() {
                expect(scope.getNoViolationVersion()).toEqual('2');
              }));
            });

            describe('For next-no-fail', function(){
              it('returns the correct version (3)', inject(function() {
                expect(scope.getNoFailVersion()).toEqual('3');
              }));
            })
          });

          describe('If suggested version type is not available', function() {
            beforeEach(function() {
              scope.suggestedRemediations = new Map();
              scope.suggestedRemediations.set('incompatible-type', {coordinates:{version:'2'}});
              scope.coordinates = {coordinates:{version:'1'}};
            });

            describe('For next-no-violations', function(){
              it('the correct version is returned', inject(function() {
                expect(scope.getNoViolationVersion()).toEqual(null);
              }));
            });

            describe('For next-no-fail', function(){
              it('the correct version is returned', inject(function() {
                expect(scope.getNoFailVersion()).toEqual(null);
              }));
            })
          });
        });
      });
    });

    describe('DetailsController', function() {
      var scope = null;

      beforeEach(inject(function($controller, $rootScope) {
        scope = $rootScope.$new();
        clmEndpoint.selectApplication = true;
        $controller('DetailsController', {
          $scope: scope
        });
      }));

      afterEach(function() {
        scope.$destroy();
      });

      it('Http Requests', inject(function($httpBackend, Coordinates, Properties) {
        var gav = {
          groupId: 'foo',
          artifactId: 'bar',
          version: '1',
          hash: '01234',
          proprietary: true,
          matchState: 'similar'
        };

        Insight.setGav(gav);

        $httpBackend.verifyNoOutstandingRequest();

        Insight.clearGav();
        scope.$apply(function() {
          document.cookie = 'clmAppId=myFirstApp';
        });
        $httpBackend.verifyNoOutstandingRequest();

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').and.returnValue('foo');
        $httpBackend.expectGET('foo').respond({securityVulnerabilities: [], policyAlerts: []});
        Insight.setGav(angular.extend({matchState: 'similar'}, gav));
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getComponentUrl).toHaveBeenCalledWith('application', 'myFirstApp', 'maven',
            '01234', 'similar', true, {
              groupId: 'foo',
              artifactId: 'bar',
              version: '1'
            }, undefined);

        // Another version selected
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities: [],
          policyAlerts: []
        });
        scope.$apply(function() {
          Coordinates.setSelected({groupId: 'foo', artifactId: 'bar', version: '2'});
          Properties.setHash('abcd');
        });
        $httpBackend.flush();
        expect(Brain[clmEndpoint.type].getComponentUrl).toHaveBeenCalledWith('application', 'myFirstApp', 'maven',
            'abcd', null, true, {
              groupId: 'foo',
              artifactId: 'bar',
              version: '2'
            }, undefined);

        // Unknown GAV
        scope.$apply(function() {
          Coordinates.set('maven', {groupId: 'foo', artifactId: 'bar', version: 1});
          Properties.setMatchState('unknown');
        });
        $httpBackend.verifyNoOutstandingRequest();
      }));

      it('isManual', inject(function($httpBackend) {
        expect(scope.isManual()).toBeFalsy();

        scope.componentDetails = {
          identificationSource: 'Manual'
        };
        expect(scope.isManual()).toBeTruthy();

        scope.componentDetails = {
          identificationSource: 'Sonatype'
        };
        expect(scope.isManual()).toBeFalsy();
      }));

      it('canMigrate', inject(function(Coordinates) {
        var gav = {
              groupId: 'foo',
              artifactId: 'bar',
              version: '1'
            },
            selected = angular.copy(gav);

        expect(scope.canMigrate()).toBeFalsy();

        spyOn(Coordinates, 'get').and.returnValue(gav);
        spyOn(Coordinates, 'getSelected').and.returnValue(selected);

        expect(scope.canMigrate()).toBeFalsy();

        selected.version = '2';
        expect(scope.canMigrate()).toBeTruthy();
      }));

      it('getMaximumSeverity', inject(function($httpBackend, Coordinates, OwnerContext) {
        scope.componentDetails = {
          securityVulnerabilities: []
        };
        expect(scope.getMaximumSeverity()).toEqual('NA');

        scope.componentDetails = {
          securityVulnerabilities: [{severity: null}]
        };
        expect(scope.getMaximumSeverity()).toEqual('Unscored');

        var gav = {
          groupId: 'groupId',
          artifactId: 'artifactId',
          version: 'version'
        };

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').and.returnValue('foo');
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities: [{severity: null}, {severity: 2}, {severity: 9}, {severity: 8}],
          policyAlerts: []
        });

        scope.$apply(function() {
          Coordinates.set('maven', gav);
          OwnerContext.setApplicationId('appId');
        });
        $httpBackend.flush();

        expect(scope.getMaximumSeverity()).toEqual(9);
      }));

      it('calculates highestPolicyThreat', inject(function($httpBackend, Coordinates, OwnerContext) {
        var gav = {
          groupId: 'groupId',
          artifactId: 'artifactId',
          version: 'version'
        };
        expect(scope.highestPolicyThreat).toBeFalsy();

        spyOn(Brain[clmEndpoint.type], 'getComponentUrl').and.returnValue('foo');
        $httpBackend.expectGET('foo').respond({
          securityVulnerabilities: [],
          policyAlerts: [
            {
              trigger: {
                policyName: 'foo',
                threatLevel: 1
              }
            }, {
              trigger: {
                policyName: 'bar',
                threatLevel: 10
              }
            }
          ]
        });

        scope.$apply(function() {
          Coordinates.set('maven', gav);
          OwnerContext.setApplicationId('appId');
        });
        $httpBackend.flush();

        expect(scope.highestPolicyThreat.level).toEqual(10);
        expect(scope.highestPolicyThreat.violatedPolicies).toEqual(2);
      }));
    });

    describe('Insight.registerViewDetailsListener', function() {
      var listener, scope;
      beforeEach(inject(function($rootScope) {
        listener = jasmine.createSpy('listener');
        scope = $rootScope.$new();
        Insight.registerViewDetailsListener(listener);
        Insight.setCoordinates('maven',
            {groupId: 'org.group', artifactId: 'stuff', classifier: 'sources', extension: 'jar', version: '1.0.0'},
            {hash: 'abcd', proprietary: true, matchState: 'similar', appId: 'myapp'});
      }));

      it('same version selected', function() {
        scope.$emit('viewDetails', '1.0.0');
        expect(listener).toHaveBeenCalledWith('myapp', 'org.group', 'stuff', '1.0.0', 'sources', 'jar', 'abcd',
            'similar', true);
      });

      it('different version selected', inject(function(Coordinates, Properties) {
        Coordinates.setSelected({version: '2.0.0'});
        Properties.setHash('1234');
        scope.$emit('viewDetails', '2.0.0');
        expect(listener).toHaveBeenCalledWith('myapp', 'org.group', 'stuff', '2.0.0', 'sources', 'jar', '1234', null,
            true);
      }));
    });

    describe('Insight.registerCoordsViewDetailsListener', function() {
      var listener, scope;
      beforeEach(inject(function($rootScope) {
        listener = jasmine.createSpy('listener');
        scope = $rootScope.$new();
        Insight.registerCoordsViewDetailsListener(listener);
        Insight.setCoordinates('maven',
            {groupId: 'org.group', artifactId: 'stuff', classifier: 'sources', extension: 'jar', version: '1.0.0'},
            {hash: 'abcd', proprietary: true, matchState: 'similar', appId: 'myapp'});
      }));

      it('same version selected', inject(function($rootScope) {
        scope.$emit('viewDetails', '1.0.0');
        expect(listener).toHaveBeenCalledWith('myapp', 'maven', [
          'groupId', 'org.group', 'artifactId', 'stuff', 'classifier', 'sources', 'extension', 'jar', 'version', '1.0.0'
        ], 'abcd', 'similar', true);
      }));

      it('different version selected', inject(function(Coordinates, Properties) {
        Coordinates.setSelected({version: '2.0.0'});
        Properties.setHash('1234');
        scope.$emit('viewDetails', '2.0.0');
        expect(listener).toHaveBeenCalledWith('myapp', 'maven', [
          'groupId', 'org.group', 'artifactId', 'stuff', 'classifier', 'sources', 'extension', 'jar', 'version', '2.0.0'
        ], '1234', null, true);
      }));
    });

    describe('Insight.registerOpenViewListener', function() {
      var listener, scope;
      beforeEach(inject(function($rootScope) {
        scope = $rootScope.$new();
        listener = jasmine.createSpy('listener');
        Insight.registerOpenViewListener(listener);
      }));

      it('openView emitted with action calls listener with action', inject(function($rootScope) {
        $rootScope.$emit('openView', 'action');
        expect(listener).toHaveBeenCalledWith('action');
      }));
    });

    describe('Insight.registerCoordsMarkUpgradeListener', function() {
      var listener, scope;
      beforeEach(inject(function($rootScope) {
        scope = $rootScope.$new();
        listener = jasmine.createSpy('listener');
        Insight.registerCoordsMarkUpgradeListener(listener);
      }));

      it('emits migrate with entityframework 4.3.0 nuget package and calls listener',
          inject(function(Coordinates, $rootScope) {
        var coordinates = {
          packageId: "EntityFramework",
          version: "4.3.0"
        };
        Coordinates.set("nuget", coordinates);
        $rootScope.$emit('markUpgrade', coordinates);
        expect(listener).toHaveBeenCalledWith("nuget", coordinates);
      }));
    });

    describe('graph', function() {
      var scope = null,
          parentScope = null;

      beforeEach(inject(function($compile, $rootScope, Coordinates, Properties) {
        spyOn(Insight, 'ComponentInformation').and.returnValue(undefined);

        parentScope = $rootScope.$new();
        parentScope.componentDetails = [
          {
            "componentIdentifier": {"coordinates": {"version": "sources"}},
            "popularity": 1,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["LIBERAL"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "LIBERAL",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.0.4"}},
            "popularity": 4,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.0.6"}},
            "popularity": 2,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.1.9"}},
            "popularity": 13,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.1.31"}},
            "popularity": 2,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.1.34"}},
            "popularity": 0,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "4.1.36"}},
            "popularity": 1,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.0.16"}},
            "popularity": 3,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.0.18"}},
            "popularity": 1,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.0.28"}},
            "hash": "caee9b1866f734373bdb",
            "popularity": 67,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.4"}},
            "popularity": 3,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["LIBERAL"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "LIBERAL",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.7-alpha"}},
            "popularity": 2,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.7"}},
            "popularity": 2,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.8-alpha"}},
            "popularity": 1,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.9-alpha"}},
            "popularity": 2,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.9"}},
            "popularity": 8,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.12"}},
            "popularity": 3,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["NOT-PROVIDED"],
            "effectiveLicenseThreat": "NOT-PROVIDED",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.15"}},
            "popularity": 14,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["NOT-PROVIDED"],
            "declaredLicenseThreats": ["LIBERAL"],
            "effectiveLicenseThreat": "LIBERAL",
            "securityThreats": ["Moderate", "Severe"]
          }, {
            "componentIdentifier": {"coordinates": {"version": "5.5.23"}},
            "hash": "b98a1711908a4641301a",
            "popularity": 100,
            "majorRevisionStep": false,
            "observedLicenseThreats": ["LIBERAL"],
            "declaredLicenseThreats": ["LIBERAL"],
            "effectiveLicenseThreat": "LIBERAL",
            "securityThreats": ["Moderate", "Severe"],
            "website": "http://tomcat.apache.org/"
          }
        ];

        Coordinates.set('maven', {
          version: '5.0.28'
        });
        Properties.setHash('abcd');

        $compile('<div graph="componentDetails"></div>')(parentScope);
        parentScope.$apply();
      }));

      afterEach(function() {
        parentScope.$destroy();
      });

      it('Version Click', inject(function(Coordinates, Properties) {
        Insight.ComponentInformation.calls.mostRecent().args[0].versionClick('5.5.23');
        expect(Coordinates.getSelected()).toEqual({"version": "5.5.23"});
        expect(Properties.getHash()).toEqual("b98a1711908a4641301a");

        Insight.ComponentInformation.calls.mostRecent().args[0].versionClick('5.0.28');
        expect(Coordinates.getSelected()).toEqual({"version": "5.0.28"});
        expect(Properties.getHash()).toEqual("abcd");
      }));

      it('Double Version Click', inject(function($rootScope) {
        var version = null;
        parentScope.$on('viewDetails', function(event, v) {
          version = v;
        });
        Insight.ComponentInformation.calls.mostRecent().args[0].versionDblClick('5.5.23');

        expect(version).toEqual('5.5.23');
      }));
    });

    describe('StatusController', function(){
      var scope = null, eventName = null;
      describe('initialization', function() {
        beforeEach(inject(function($controller, $rootScope) {
          scope = $rootScope.$new();
          clmEndpoint.selectApplication = true;
          clmEndpoint.openView = function (scope, event){
            eventName = event;
          };
          $controller('StatusController', {
            $scope: scope
          });
        }));
        describe('$scope.openView()', function () {
          it('calls clmEndpoint.openView() and prevents default', function(){
            var mockEvent = new Event('ng-click');
            spyOn(mockEvent, 'preventDefault');
            scope.openView(mockEvent, 'event');
            expect(eventName).toBe('event');
            expect(mockEvent.preventDefault).toHaveBeenCalled();
          });
        });
      });
    })

    describe('Insight.setCapabilities', function (){
      var scope;
      beforeEach(inject(function($rootScope) {
        scope = $rootScope.$new();
        scope.migrateSupported = clmEndpoint.migrate;
        scope.viewDetailsSupported = clmEndpoint.viewDetails;
      }));

      it('overwrites capabilities when called without quotes', inject(function($rootScope) {
        Insight.setCapabilities({viewDetails: false, migrate: false});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(false);
      }));

      it('does not overwrite capabilities when called with null', inject(function($rootScope) {
        Insight.setCapabilities({viewDetails: false, migrate: null});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(true);
      }));

      it('does not overwrite capabilities when called with missing value', inject(function($rootScope) {
        Insight.setCapabilities({migrate: false});
        expect($rootScope.viewDetailsSupported).toEqual(true);
        expect($rootScope.migrateSupported).toEqual(false);
      }));

      it('overwrites capabilities when called with quotes', inject(function($rootScope) {
        Insight.setCapabilities({'viewDetails': false, 'migrate': false});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(false);
      }));

      it('does not overwrite capabilities when called with quotes and null', inject(function($rootScope) {
        Insight.setCapabilities({'viewDetails': false, 'migrate': null});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(true);
      }));

      it('does not overwrite capabilities when called with quotes and missing value', inject(function($rootScope) {
        Insight.setCapabilities({'migrate': false});
        expect($rootScope.viewDetailsSupported).toEqual(true);
        expect($rootScope.migrateSupported).toEqual(false);
      }));

      it('overwrites capabilities when called without double quotes', inject(function($rootScope) {
        Insight.setCapabilities({"viewDetails": false, "migrate": false});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(false);
      }));

      it('does not overwrite capabilities when called with double quotes and null', inject(function($rootScope) {
        Insight.setCapabilities({'viewDetails': false, 'migrate': null});
        expect($rootScope.viewDetailsSupported).toEqual(false);
        expect($rootScope.migrateSupported).toEqual(true);
      }));

      it('does not overwrite capabilities when called with double quotes and missing value',
          inject(function($rootScope) {
            Insight.setCapabilities({'migrate': false});
            expect($rootScope.viewDetailsSupported).toEqual(true);
            expect($rootScope.migrateSupported).toEqual(false);
          }));
    });
  });
}());
