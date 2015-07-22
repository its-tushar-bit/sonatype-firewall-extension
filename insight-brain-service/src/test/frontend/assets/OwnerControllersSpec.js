describe('OwnerControllers', function () {
  beforeEach(module('OwnerModule', function ($provide) {
    $provide.value('$cookies', {});
  }));

  function createTests(type, storeName, owner) {
    var deferred,
        refreshDeferred,
        controllerScope;

    function flushTimeouts() {
      inject(function ($timeout) {
        $timeout.flush();
      });
    }

    function callPromiseError() {
      deferred.reject.apply(deferred, arguments);
      flushTimeouts();
    }

    function callPromiseSuccess() {
      deferred.resolve.apply(deferred, arguments);
      flushTimeouts();
    }

    beforeEach(inject(function ($q) {
      deferred = $q.defer();
      spyOn(deferred.promise, 'then').andCallThrough();
      refreshDeferred = $q.defer();
      spyOn(refreshDeferred.promise, 'then').andCallThrough();
    }));

    afterEach(function () {
      controllerScope.$destroy();
    });

    describe('scrollspy handling', function(){
      var element, spy;
      beforeEach(inject(function($rootScope){
        controllerScope = $rootScope.$new();
        element = angular.element('<div><div id="pills"><ul class="nav nav-pills">' +
        '<li><a data-toggle="pill" data-target="#pill1" ng-href="">1</a></li></ul></div>' +
        '<div id="scroller" scrollspy="#pills"><div id="pill1"></div></div>');
      }));
      
      it('Validate scrollspy is initialized prpoerly', inject(function($compile) {
        spy = spyOn($.fn, 'scrollspy');
        expect(spy).not.toHaveBeenCalled();
        $compile(element)(controllerScope);
        expect(spy).toHaveBeenCalled();
      }));

      it('Validate pill click causes scroll', inject(function($compile, $timeout) {
        //have to add to dom for click events to be processed
        angular.element('body').append(element);
        $compile(element)(controllerScope);
        spy = spyOn($.fn, 'scrollTop');
        expect(spy).not.toHaveBeenCalled();
        element.find('#pills .nav li > a').click();
        $timeout.flush();
        //once to get and once to set
        expect(spy.callCount).toBe(2);

        //remove the element we added to the dom
        element.remove();
      }));
    });

    describe('OwnerSummaryController', function () {
      beforeEach(inject(['$controller', '$rootScope', storeName, function ($controller, $rootScope, store) {
        spyOn(store, 'get').andReturn(deferred.promise);
        spyOn(store, 'refresh').andReturn(refreshDeferred.promise);

        controllerScope = $rootScope.$new();
        $controller('OwnerSummaryController', {
          $scope : controllerScope,
          $state : {
            current : {
              name : 'management.' + type +  '-view'
            },
            params : type === 'application' ? { applicationPublicId : 'abcd' } : { organizationId : 'abcd' }
          }
        });
      }]));

      it('Typical', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseSuccess([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Missing', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseSuccess([{},{}]);
        expect(controllerScope.error).toEqual('Unable to locate ' + type);
        expect(controllerScope.type).toEqual(type);
      }));

      it('Error', inject(function () {
        expect(deferred.promise.then).toHaveBeenCalled();

        callPromiseError('error');
        expect(controllerScope.owner).toBeUndefined();
        expect(controllerScope.error).toEqual(['error']);
        expect(controllerScope.type).toEqual(type);

        // reload successfully
        controllerScope.doLoad();
        expect(refreshDeferred.promise.then).toHaveBeenCalled();
        refreshDeferred.promise.then.mostRecentCall.args[0]([owner]);
        expect(controllerScope.owner).toEqual(owner);
        expect(controllerScope.type).toEqual(type);
        expect(controllerScope.error).toBeUndefined();
      }));
    });

    describe('OwnerEditorController', function () {
      describe('New Owner', function () {
        var ownerResource;
        beforeEach(inject(['$controller', '$rootScope', function ($controller, $rootScope) {
          ownerResource = {
            $new : true,
            $save : angular.noop,
            isDirty : angular.noop,
            $clone : angular.noop
          };

          controllerScope = $rootScope.$new();
          controllerScope.$dismiss = jasmine.createSpy('dismiss');
          controllerScope.$close = jasmine.createSpy('close');

          $controller('OwnerEditorController', {
            $scope : controllerScope,
            owner : ownerResource,
            ownerType : type,
            siblings : []
          });
        }]));

        describe('Page Changes', function () {
          it('clean', inject(function ($rootScope) {
            spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(false);
            var event = $rootScope.$broadcast('pageChangeStarted');

            expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
            expect(event.defaultPrevented).toBeFalsy();
          }));

          it('dirty', inject(function ($rootScope) {
            spyOn(controllerScope.dirtyOwner, 'isDirty').andReturn(true);
            var event = $rootScope.$broadcast('pageChangeStarted');

            expect(controllerScope.dirtyOwner.isDirty).toHaveBeenCalled();
            expect(event.defaultPrevented).toBeTruthy();
          }));

          it('Closes', inject(function ($rootScope) {
            $rootScope.$broadcast('pageChangeAccepted');
            expect(controllerScope.$dismiss).toHaveBeenCalled();
          }));
        });

        describe('Save', function () {
          beforeEach(function () {
            spyOn(controllerScope.dirtyOwner, '$save').andReturn(deferred.promise);

            controllerScope.$apply(function () {
              controllerScope.dirtyOwner.name = 'My new ' + type;
              if (type === 'application') {
                controllerScope.dirtyOwner.publicId = 'my-new';
              }
            });
            expect(ownerResource.name).toEqual('My new ' + type); // new objects work with the original

            controllerScope.save();
          });

          it('Error on Owner', function () {
            callPromiseError('foobar');
            expect(controllerScope.error).toEqual('foobar');

            // retry clears error
            controllerScope.save();
            expect(controllerScope.error).toBeFalsy();
          });

          it('Error on Icon', inject(function ($state, $httpBackend) {
            $httpBackend.expectPOST('/rest/' + type + '/icon').respond(500, 'Server Error');
            callPromiseSuccess(angular.extend({ id : 'abcd' }, angular.copy(controllerScope.dirtyOwner)));
            $httpBackend.flush();
            expect(controllerScope.error).toEqual('Server Error');

            // retry clears error
            controllerScope.save();
            expect(controllerScope.error).toBeFalsy();
          }));

          it('Success', inject(function ($state, $httpBackend) {
            spyOn($state, 'go');

            $httpBackend.expectPOST('/rest/' + type + '/icon').respond('');
            callPromiseSuccess(angular.extend({ id : 'abcd' }, angular.copy(controllerScope.dirtyOwner)));
            $httpBackend.flush();
            flushTimeouts();

            expect($state.go).toHaveBeenCalledWith('management.' + type + '-view', type === 'application' ? {
              applicationPublicId: controllerScope.dirtyOwner.publicId
            } : {
              organizationId: 'abcd'
            });
            expect(controllerScope.$close).toHaveBeenCalled();
          }));
        });
      });
    });
  }

  describe('Organization', function () {
    createTests('organization', 'OrganizationStore', { id : 'abcd', name : 'My Org' });
  });

  describe('Application', function () {
    createTests('application', 'ApplicationStore', { publicId : 'abcd', id : '0000abcd', name : 'My App' })
  });

  describe('OwnerEditor', function () {
    beforeEach(inject(function ($modal) {
      spyOn($modal, 'open');
    }));

    it('open', inject(function (OwnerEditor, $modal) {
      var owner = {
         id : 'foo',
         name : 'bar'
      };

      OwnerEditor.open(owner, 'organization');
      expect($modal.open).toHaveBeenCalled();

      expect($modal.open.mostRecentCall.args[0].resolve.owner()).toEqual(owner);
      expect($modal.open.mostRecentCall.args[0].resolve.ownerType()).toEqual('organization');
    }));
  });
});
