/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../../main/frontend/mainHeader/module';

describe('notificationsMenu', function () {
  var notificationScope, vm;

  beforeEach(angular.mock.module(mainHeaderModule.name));

  beforeEach(inject(function($rootScope, $componentController) {
    notificationScope = $rootScope.$new();

    vm = $componentController('notificationsMenu', {
      $scope: notificationScope
    });

    vm.$onInit();

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

    $httpBackend.flush();

    expect(notificationScope.notifications[0].age).toEqual(1);
    expect(notificationScope.notifications[0].ageQualifier).toEqual('day ago');
    expect(notificationScope.notifications[1].age).toEqual(1);
    expect(notificationScope.notifications[1].ageQualifier).toEqual('hour ago');
    expect(notificationScope.notifications[2].age).toEqual(1);
    expect(notificationScope.notifications[2].ageQualifier).toEqual('minute ago');
    expect(notificationScope.notifications[3].age).toEqual('');
    expect(notificationScope.notifications[3].ageQualifier).toEqual('Just now');
    expect(notificationScope.notifications[4].age).toEqual(10);
    expect(notificationScope.notifications[4].ageQualifier).toEqual('days ago');
    expect(notificationScope.notifications[5].age).toEqual(10);
    expect(notificationScope.notifications[5].ageQualifier).toEqual('hours ago');
    expect(notificationScope.notifications[6].age).toEqual(10);
    expect(notificationScope.notifications[6].ageQualifier).toEqual('minutes ago');
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

    $httpBackend.flush();

    expect(notificationScope.unreadNotificationCount).toEqual(1);
    expect(notificationScope.notifications[0].viewed).toEqual(false);
    $httpBackend.expectPOST(CLMLocations.getNotificationViewedUrl(), {id: '1'}).respond(200);
    vm.openDetail(notificationScope.notifications[0]);
    $httpBackend.flush();
    expect(notificationScope.unreadNotificationCount).toEqual(0);
    expect(notificationScope.notifications[0].viewed).toEqual(true);
    expect(notificationScope.selectedNotification).toEqual(notificationScope.notifications[0]);
  }));
});
