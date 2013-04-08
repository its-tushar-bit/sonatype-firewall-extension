describe('notification-management tests', function() {
    var scope,
        emailList = [ 'email1@email.com', 'email2@email.com', 'email3@email.com' ];

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
        var emails = angular.copy(emailList);
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);
        scope.currentNotificationEmail = 'aaa@email.com';
        scope.addNotificationEmail();
        expect(scope.notificationEmailList.length).toEqual(4);

        scope.removeNotificationEmail(2);
        expect(scope.notificationEmailList.length).toEqual(3);

        scope.cancelNotificationEmail();
        expect(emails).toEqual(emailList);
    }));

    it('Test add notification to list', inject(function($rootScope) {
        var emails = ['email1@email.com'];
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com' ]);

        scope.currentNotificationEmail = 'email2@email.com';
        scope.addNotificationEmail();
        expect(emails.length).toEqual(1); // verify list is not modified until submission

        scope.currentNotificationEmail = 'aaa@email.com';
        scope.addNotificationEmail();
        expect(emails.length).toEqual(1); // verify list is not modified until submission

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'aaa@email.com', 'email1@email.com', 'email2@email.com' ]);

        scope.doneNotificationEmail();

        expect(emails).toEqual([ 'aaa@email.com', 'email1@email.com', 'email2@email.com' ]);
    }));

    it('Test remove notification from list', inject(function($rootScope) {
        var emails = angular.copy(emailList);
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.currentNotificationEmail).toBeUndefined();
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);

        scope.removeNotificationEmail(1);
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email3@email.com' ]);
        scope.removeNotificationEmail(1);
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com' ]);
        scope.removeNotificationEmail(0);
        expect(scope.notificationEmailList).toEqual([]);

        scope.doneNotificationEmail();

        expect(emails).toEqual([]);
    }));
});