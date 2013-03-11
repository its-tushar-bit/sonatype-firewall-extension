describe('notification-management tests', function() {
    var scope;

    beforeEach(module('NotificationManagement'));
    beforeEach(inject(function($rootScope, $controller) {
        // inject the controller
        scope = $rootScope.$new();
        scope.neditor = {
            $valid : true
        };
        $controller('NotificationManagementController', {
            $scope : scope,
            global : {}
        });
    }));

    it('Test initial data state', function() {
        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toBeUndefined();
        expect(scope.currentActionStep).toBeUndefined();
    });

    it('Test open edit notification modal', inject(function($rootScope) {
        $rootScope.$broadcast('editNotification', {
            id : 'stageId',
            name : 'stageName',
            notifyCount : 3,
            actions : [ {
                action : 'notify',
                target : 'email3@email.com'
            }, {
                action : 'notify',
                target : 'email2@email.com'
            }, {
                action : 'notify',
                target : 'email1@email.com'
            } ],
            action : 'none'
        });

        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);
        expect(scope.currentActionStep).toEqual({
            id : 'stageId',
            name : 'stageName',
            notifyCount : 3,
            actions : [ {
                action : 'notify',
                target : 'email3@email.com'
            }, {
                action : 'notify',
                target : 'email2@email.com'
            }, {
                action : 'notify',
                target : 'email1@email.com'
            } ],
            action : 'none'
        });

        scope.cancelNotificationEmail();
    }));

    it('Test add notification to list', inject(function($rootScope) {
        $rootScope.$broadcast('editNotification', {
            id : 'stageId',
            name : 'stageName',
            notifyCount : 1,
            actions : [ {
                action : 'notify',
                target : 'email1@email.com'
            } ],
            action : 'none'
        });

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com' ]);
        expect(scope.currentActionStep).toEqual({
            id : 'stageId',
            name : 'stageName',
            notifyCount : 1,
            actions : [ {
                action : 'notify',
                target : 'email1@email.com'
            } ],
            action : 'none'
        });

        scope.currentNotificationEmail = 'email2@email.com';
        scope.addNotificationEmail();
        scope.currentNotificationEmail = 'aaa@email.com';
        scope.addNotificationEmail();

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'aaa@email.com', 'email1@email.com', 'email2@email.com' ]);

        scope.doneNotificationEmail();

        expect(scope.currentActionStep).toEqual({
            id : 'stageId',
            name : 'stageName',
            notifyCount : 3,
            actions : [ {
                action : 'notify',
                target : 'aaa@email.com'
            }, {
                action : 'notify',
                target : 'email1@email.com'
            }, {
                action : 'notify',
                target : 'email2@email.com'
            } ],
            action : 'none'
        });
    }));

    it('Test remove notification from list', inject(function($rootScope) {
        $rootScope.$broadcast('editNotification', {
            id : 'stageId',
            name : 'stageName',
            notifyCount : 3,
            actions : [ {
                action : 'notify',
                target : 'email1@email.com'
            }, {
                action : 'notify',
                target : 'email2@email.com'
            }, {
                action : 'notify',
                target : 'email3@email.com'
            } ],
            action : 'none'
        });

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);
        expect(scope.currentActionStep).toEqual({
            id : 'stageId',
            name : 'stageName',
            notifyCount : 3,
            actions : [ {
                action : 'notify',
                target : 'email1@email.com'
            }, {
                action : 'notify',
                target : 'email2@email.com'
            }, {
                action : 'notify',
                target : 'email3@email.com'
            } ],
            action : 'none'
        });

        scope.removeNotificationEmail(1);
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email3@email.com' ]);
        scope.removeNotificationEmail(1);
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com' ]);
        scope.removeNotificationEmail(0);
        expect(scope.notificationEmailList).toEqual([]);

        scope.doneNotificationEmail();

        expect(scope.currentActionStep).toEqual({
            id : 'stageId',
            name : 'stageName',
            notifyCount : 0,
            actions : [],
            action : 'none'
        });
    }));
});