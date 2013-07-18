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
        expect(scope.notificationEmailList).toBeUndefined();
    });

    it('Test open edit notification modal', inject(function($rootScope) {
        var emails = angular.copy(emailList);
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);
    }));

    it('Test add notification to list', inject(function($rootScope) {
        var emails = ['email1@email.com'];
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.notificationEmailList).toEqual([ 'email1@email.com' ]);

        scope.notificationEmailList.push('email2@email.com');
        
        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com' ]);
        
        var rcvd = false;
        scope.$on('editNotificationDone',function(event,emails){
            rcvd = true;
            expect(emails).toEqual([ 'email1@email.com', 'email2@email.com' ]);
        });

        scope.doneNotificationEmail();

        expect(rcvd).toEqual(true);
    }));

    it('Test remove notification from list', inject(function($rootScope) {
        var emails = angular.copy(emailList);
        $rootScope.$broadcast('editNotification', emails);

        expect(scope.notificationEmailList).toEqual([ 'email1@email.com', 'email2@email.com', 'email3@email.com' ]);

        scope.notificationEmailList.splice(0,1);
        
        expect(scope.notificationEmailList).toEqual([ 'email2@email.com', 'email3@email.com' ]);
        
        var rcvd = false;
        scope.$on('editNotificationDone',function(event,emails){
            rcvd = true;
            expect(emails).toEqual([ 'email2@email.com', 'email3@email.com' ]);
        });

        scope.doneNotificationEmail();

        expect(rcvd).toEqual(true);
    }));
});