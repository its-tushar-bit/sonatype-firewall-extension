describe('mainHeader', function() {
  var scope, state,
    parentScope = null,
    dialogScope = null,
    currentUserSuccess = null,
    currentUserFail = null;

  beforeEach(module('MainHeader', function($provide) {
    $provide.value('$window', {
      location: {
        href: 'http://blah/index.html',
        replace: jasmine.createSpy()
      },
      navigator: {
        userAgent: {}
      },
      document: {
        createElement: function() {
          return null;
        }
      }
    });

    $provide.value('CurrentUser', {
      then : function (success, fail) {
        currentUserSuccess = success;
        currentUserFail = fail;
        return this;
      }
    });

    $provide.value('ProductFeatures', {
      isDashboardLicensed : function() {
        return true;
      }
    });

    $provide.factory('PermissionService', ['$q', function ($q) {
      var deferred = $q.defer();
      deferred.resolve();
      function fn() {
        return deferred.promise;
      }
      return {
        isAuthorized: fn,
        requireAuthorization: fn,
        requireAuthorizationIf: fn,
        getValidPermissions: fn
      };
    }]);

    $provide.value('$modal', {
      open: function(config) {
        dialogScope = scope.$new();
        dialogScope.$close = jasmine.createSpy('dialogClose');
        inject(function($controller) {
          $controller(config.controller, {
            $scope: dialogScope
          });
        });
        return {
          result: {
            then: function(success, failure) {
              success();
            }
          }
        };
      }
    });
  }));

  beforeEach(inject(function($rootScope, $state, $controller) {
    scope = $rootScope.$new();
    state = $state;

    window.clmServerVersion = '1.2.3-4';

    $controller('mainHeaderController', {
      $scope: scope,
      $state: state
    });
  }));

  afterEach(inject(function($httpBackend) {
    if (parentScope) {
      parentScope.$destroy();
    } else if (scope) {
      scope.$destroy();
    }
    delete window.clmServerVersion;
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Main Header Notification Controller', function () {
    var notificationScope;
    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
      notificationScope = $rootScope.$new();

      $httpBackend.expectGET(CLMLocations.getNotificationUrl()).respond({
        notifications: []
      });

      $controller('Notifications', {
        $scope: notificationScope
      });

      $httpBackend.flush();
    }));

    afterEach(function() {
      if (notificationScope) {
        notificationScope.$destroy();
      }
    });

    it('test loading data', inject(function($httpBackend, CLMLocations) {
      var tenDaysAgo = new Date().getTime() - 10 * 24 * 60 * 60 * 1000;

      $httpBackend.expectGET(CLMLocations.getNotificationUrl()).respond({
        notifications: [{
          id: '1234',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail',
          dateCreated: tenDaysAgo,
          viewed: false
        }]
      });

      notificationScope.getNotifications();

      expect(notificationScope.loading).toEqual(true);

      $httpBackend.flush();

      expect(notificationScope.loading).toEqual(false);
      expect(notificationScope.unreadNotificationCount).toEqual(1);
      expect(notificationScope.notifications.length).toEqual(1);
      expect(notificationScope.notifications[0].id).toEqual('1234');
      expect(notificationScope.notifications[0].type).toEqual('default');
      expect(notificationScope.notifications[0].summaryText).toEqual('summary');
      expect(notificationScope.notifications[0].detailHtml.toString()).toEqual('detail');
      expect(notificationScope.notifications[0].dateCreated).toEqual(tenDaysAgo);
      expect(notificationScope.notifications[0].viewed).toEqual(false);
      expect(notificationScope.notifications[0].age).toEqual(10);
      expect(notificationScope.notifications[0].ageQualifier).toEqual('days ago');
    }));

    it('validate age calculations', inject(function($httpBackend, CLMLocations) {
      var oneDayAgo = new Date().getTime() - 24 * 60 * 60 * 1000 - 1,
          oneHourAgo = new Date().getTime() - 60 * 60 * 1000 - 1,
          oneMinuteAgo = new Date().getTime() - 60 * 1000 - 1,
          oneSecondAgo = new Date().getTime() - 1000 - 1,
          tenDaysAgo = new Date().getTime() - 10 * 24 * 60 * 60 * 1000 - 1,
          tenHoursAgo = new Date().getTime() - 10 * 60 * 60 * 1000 - 1,
          tenMinutesAgo = new Date().getTime() - 10 * 60 * 1000 - 1,
          tenSecondsAgo = new Date().getTime() - 10 * 1000 - 1;

      $httpBackend.expectGET(CLMLocations.getNotificationUrl()).respond({
        notifications: [{
          id: '1',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: oneDayAgo,
          viewed: true
        }, {
          id: '2',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: oneHourAgo,
          viewed: true
        }, {
          id: '3',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: oneMinuteAgo,
          viewed: true
        }, {
          id: '4',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: oneSecondAgo,
          viewed: true
        }, {
          id: '5',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: tenDaysAgo,
          viewed: true
        }, {
          id: '6',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: tenHoursAgo,
          viewed: true
        }, {
          id: '7',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: tenMinutesAgo,
          viewed: true
        }, {
          id: '8',
          type: 'default',
          summaryText: 'summary',
          detailHtml: 'detail http://something',
          dateCreated: tenSecondsAgo,
          viewed: true
        }]
      });

      notificationScope.getNotifications();

      $httpBackend.flush();

      expect(notificationScope.notifications[0].age).toEqual(1);
      expect(notificationScope.notifications[0].ageQualifier).toEqual('day ago');
      expect(notificationScope.notifications[1].age).toEqual(1);
      expect(notificationScope.notifications[1].ageQualifier).toEqual('hour ago');
      expect(notificationScope.notifications[2].age).toEqual(1);
      expect(notificationScope.notifications[2].ageQualifier).toEqual('min ago');
      expect(notificationScope.notifications[3].age).toEqual('');
      expect(notificationScope.notifications[3].ageQualifier).toEqual('Just now');
      expect(notificationScope.notifications[4].age).toEqual(10);
      expect(notificationScope.notifications[4].ageQualifier).toEqual('days ago');
      expect(notificationScope.notifications[5].age).toEqual(10);
      expect(notificationScope.notifications[5].ageQualifier).toEqual('hours ago');
      expect(notificationScope.notifications[6].age).toEqual(10);
      expect(notificationScope.notifications[6].ageQualifier).toEqual('mins ago');
      expect(notificationScope.notifications[7].age).toEqual('');
      expect(notificationScope.notifications[7].ageQualifier).toEqual('Just now');
    }));

    it('validate mark as read', inject(function($httpBackend, CLMLocations) {
      var notification = {
        id: '1',
        type: 'default',
        summaryText: 'summary',
        detailHtml: 'detail http://something',
        dateCreated: new Date().getTime(),
        viewed: false
      };

      $httpBackend.expectGET(CLMLocations.getNotificationUrl()).respond({
        notifications: [notification]
      });

      notificationScope.getNotifications();

      $httpBackend.flush();

      expect(notificationScope.unreadNotificationCount).toEqual(1);
      expect(notificationScope.notifications[0].viewed).toEqual(false);
      $httpBackend.expectPOST(CLMLocations.getNotificationViewedUrl(), {id:'1'}).respond(200);
      notificationScope.openDetail(notificationScope.notifications[0]);
      $httpBackend.flush();
      expect(notificationScope.unreadNotificationCount).toEqual(0);
      expect(notificationScope.notifications[0].viewed).toEqual(true);
      expect(notificationScope.selectedNotification).toEqual(notificationScope.notifications[0]);
    }));
  });

  describe('Main Header User Controls', function () {
    it('Major Minor Version', function () {
      expect(scope.majorMinorVersion).toEqual("1.2");
    });
    describe('LogoutController', function () {
      beforeEach(inject(function ($controller, $rootScope) {
        parentScope = $rootScope.$new();

        scope = parentScope.$new();

        $controller('LogoutController', {
          $scope : scope
        });
      }));

      it('provides the ability to log out', inject(function($httpBackend, CLMLocations){
        var spy = jasmine.createSpy();
        parentScope.$on('logout', spy);

        expect(scope.logout).not.toBeUndefined();
        $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

        scope.logout();
        $httpBackend.flush();

        expect(spy).toHaveBeenCalled();
      }));
    });

    describe('ChangePasswordController', function () {
      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();

        $controller('ChangePassword', {
          $scope : scope
        });
      }));

      describe('canChangePassword', function () {
        it('Not Loaded', function () {
          expect(scope.canChangePassword()).toBeFalsy();
        });
        it('CLM User', function () {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : true
          });
          expect(scope.canChangePassword()).toBeTruthy();
        });
        it('Not CLM User', function () {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : false
          });
          expect(scope.canChangePassword()).toBeFalsy();
        });
      });

      describe('Dialog', function () {
        beforeEach(inject(function () {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : true
          });
          scope.change();

          dialogScope.result = {
            originalPassword : 'bar',
            newPassword : 'xxx',
            confirmPassword : 'xxx'
          };
          dialogScope.passwordForm = {
            $valid : true // form validation
          };
        }));

        it('With Valid Auth', inject(function ($httpBackend, CLMLocations) {
          $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);
          dialogScope.save();
          expect(dialogScope.submitActive).toBeTruthy();
          $httpBackend.flush();

          expect(dialogScope.$close).toHaveBeenCalled();
        }));

        it('With Invalid Auth', inject(function ($httpBackend, CLMLocations, Messages) {
          $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400);

          dialogScope.save();
          expect(dialogScope.submitActive).toBeTruthy();

          $httpBackend.flush();

          expect(dialogScope.submitActive).toBeFalsy();
          expect(dialogScope.$close).not.toHaveBeenCalled();
          expect(dialogScope.error[0]).toEqual({msg:Messages.getHttpErrorMessage([undefined, 400]), type: 'error'});
        }));
      });
    });
  });
});