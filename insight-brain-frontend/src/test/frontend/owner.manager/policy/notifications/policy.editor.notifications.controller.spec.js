/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../../main/frontend/owner.manager/owner.manager.module';

describe('policy.editor.notifications.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name));

  var membershipMapping = {
    membersByRole: [
      {
        roleId: '1',
        roleName: 'Application Evaluator'
      },
      {
        roleId: '2',
        roleName: 'Developer'
      },
      {
        roleId: '3',
        roleName: 'Zoologist'
      }
    ]
  };

  var webhooks = [
    {
      id: 'webhook1',
      url: 'url1'
    },
    {
      id: 'webhook2',
      url: 'url2',
      description: 'description2'
    }
  ];

  var createJiraServiceResolver = function() {
    var enabledDefer,
        getProjectsDefer,
        jiraService,
        $q;

    beforeEach(inject([
      '$q', 'jira.service', function(_$q_, _jiraService_) {
        $q = _$q_;
        jiraService = _jiraService_;
        spyOn(jiraService, 'isEnabled');
        spyOn(jiraService, 'getJiraProjects');
        reset();
      }
    ]));

    function reset() {
      enabledDefer = $q.defer();
      getProjectsDefer = $q.defer();

      jiraService.isEnabled.and.returnValue(enabledDefer.promise);
      jiraService.getJiraProjects.and.returnValue(getProjectsDefer.promise);
    }

    return {
      reset: reset,
      resolveIsEnabled: function(isEnabled) {
        enabledDefer.resolve(isEnabled);
      },
      resolveGetJiraProjects: function(projects) {
        getProjectsDefer.resolve(projects);
      },
      rejectGetJiraProjects: function(error) {
        getProjectsDefer.reject(error);
      }
    };
  };

  var initController,
      scope,
      CLMLocations,
      getWebhooks,
      jiraProjects = JiraServiceMockData.getJiraProjectsUrl();

  var jiraServiceResolver = createJiraServiceResolver();

  beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMContextLocations, _CLMLocations_) {
    scope = $rootScope.$new();
    CLMLocations = _CLMLocations_;

    initController = function(notifications, jiraEnabled) {
      var ctrlFn = $controller('policy.editor.notifications.controller', {
        $scope: scope
      }, true);

      ctrlFn.instance.notifications = notifications;
      var vm = ctrlFn();
      jiraServiceResolver.resolveIsEnabled(jiraEnabled);
      jiraServiceResolver.resolveGetJiraProjects(jiraProjects);
      $httpBackend.flush();
      scope.vm = vm; // needed to be able to test scope.$watch
      return vm;
    };

    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['policy-monitoring',
      'webhooks-for-applications']);

    $httpBackend.whenGET('/rest/policy/stages?context=all').respond([]);
    $httpBackend.whenGET(CLMContextLocations.getRoleMappingUrl()).respond(membershipMapping);
    getWebhooks = $httpBackend.whenGET(CLMContextLocations.getNotificationWebhooksUrl());
    getWebhooks.respond(webhooks);
  }));

  describe('controller init', function() {
    it('populates recipients from userNotifications', function() {
      var notifications = {
        userNotifications: [
          {
            emailAddress: 'test1@test.com',
            stageIds: ['proxy', 'build']
          },
          {
            emailAddress: 'test2@test.com',
            stageIds: ['develop']
          }
        ]
      };

      var vm = initController(notifications);

      expect(vm.recipients.length).toBe(2);
      expect(vm.recipients[0].emailAddress).toBe('test1@test.com');
      expect(vm.recipients[1].emailAddress).toBe('test2@test.com');
      expect(vm.isMonitoringSupported).toBe(true);
    });

    it('populates recipients from roleNotifications', function() {
      var notifications = {
        roleNotifications: [
          {
            roleId: '1',
            stageIds: ['proxy', 'build']
          },
          {
            roleId: '2',
            stageIds: ['develop']
          }
        ]
      };

      var vm = initController(notifications);

      expect(vm.recipients.length).toBe(2);
      expect(vm.recipients[0].roleId).toBe('1');
      expect(vm.recipients[1].roleId).toBe('2');
    });

    it('populates recipients from jiraNotifications', function() {
      var notifications = {
        jiraNotifications: [
          {
            projectKey: 'key1',
            issueTypeId: 1,
            stageIds: ['proxy', 'build']
          },
          {
            projectKey: 'key2',
            issueTypeId: 2,
            stageIds: ['develop']
          }
        ]
      };

      var vm = initController(notifications, true);

      expect(vm.recipients.length).toBe(2);
      expect(vm.recipients[0].projectKey).toBe('key1');
      expect(vm.recipients[0].issueTypeId).toBe(1);
      expect(vm.recipients[1].projectKey).toBe('key2');
      expect(vm.recipients[1].issueTypeId).toBe(2);
    });

    it('populates recipients from webhookNotifications', function() {
      var notifications = {
        webhookNotifications: [
          {
            webhookId: 'key1',
            stageIds: ['proxy', 'build']
          },
          {
            webhookId: 'key2',
            stageIds: ['develop']
          }
        ]
      };

      var vm = initController(notifications, true);

      expect(vm.recipients.length).toBe(2);
      expect(vm.recipients[0].webhookId).toBe('key1');
      expect(vm.recipients[1].webhookId).toBe('key2');
    });

    it('still shows editor if jira projects fails', inject(function(CLMContextLocations, $controller, $httpBackend) {
      var error = 'error';

      jiraServiceResolver.reset();
      jiraServiceResolver.resolveIsEnabled(true);
      jiraServiceResolver.rejectGetJiraProjects(error);

      var ctlFn = $controller('policy.editor.notifications.controller', {
        $scope: scope
      }, true);
      ctlFn.instance.notifications = [];
      var vm = ctlFn();

      $httpBackend.flush();

      expect(vm.loadError).toBeUndefined();
      expect(vm.jiraError).toEqual(error);
    }));

    it('handles no notifications', function() {
      var vm = initController({});
      expect(vm.recipients.length).toBe(0);
    });

    it('sorts vm.recipients by email, roleName or projectName', function() {

      var notifications = {
        userNotifications: [
          {
            emailAddress: 'zoo@test.com',
            stageIds: ['proxy', 'build']
          },
          {
            emailAddress: 'bob@test.com',
            stageIds: ['develop']
          }
        ],
        roleNotifications: [
          {
            roleId: '1',
            stageIds: ['proxy', 'build']
          },
          {
            roleId: '2',
            stageIds: ['develop']
          }
        ],
        jiraNotifications: [
          {
            projectKey: 'key2',
            issueTypeId: 2,
            stageIds: ['develop']
          },
          {
            projectKey: 'key1',
            issueTypeId: 1,
            stageIds: ['proxy', 'build']
          }
        ]
      };

      var vm = initController(notifications);
      expect(vm.recipients[0].roleId).toBe('1');
      expect(vm.recipients[1].emailAddress).toBe('bob@test.com');
      expect(vm.recipients[2].roleId).toBe('2');
      expect(vm.recipients[3].projectKey).toBe('key1');
      expect(vm.recipients[4].projectKey).toBe('key2');
      expect(vm.recipients[5].emailAddress).toBe('zoo@test.com');
    });

    it('sets watcher to reload recipients when model changes', function() {
      var notifications = {
        userNotifications: [
          {
            emailAddress: 'bob@test.com',
            stageIds: ['develop']
          }
        ],
        roleNotifications: []
      };

      var vm = initController(notifications);
      expect(vm.recipients.length).toBe(1);

      vm.notifications = {};
      scope.$digest();

      expect(vm.recipients.length).toBe(0);
    });

  });

  describe('hasRecipients()', function() {
    it('returns true if there are notifications', function() {
      var notifications = {
        roleNotifications: [
          {
            roleId: '1',
            stageIds: ['proxy', 'build']
          },
          {
            roleId: '2',
            stageIds: ['develop']
          }
        ]
      };

      var vm = initController(notifications);
      expect(vm.hasRecipients()).toBe(true);
    });

    it('returns false if there are notifications', function() {
      var vm = initController({});
      expect(vm.hasRecipients()).toBe(false);
    });
  });

  describe('removeRecipient()', function() {
    var vm;

    beforeEach(function() {
      var notifications = {
        userNotifications: [
          {
            emailAddress: 'zoo@test.com',
            stageIds: ['proxy', 'build']
          },
          {
            emailAddress: 'bob@test.com',
            stageIds: ['develop']
          }
        ],
        roleNotifications: [
          {
            roleId: '1', // Application Evaluator
            stageIds: ['proxy', 'build']
          },
          {
            roleId: '2', // Developer
            stageIds: ['develop']
          }
        ],
        jiraNotifications: [
          {
            projectKey: 'key1',
            issueTypeId: 1,
            stageIds: ['proxy', 'build']
          },
          {
            projectKey: 'key2',
            issueTypeId: 2,
            stageIds: ['develop']
          }
        ]
      };
      vm = initController(notifications);
    });

    it('removes role recipient entry from original notifications and updates vm.recipients array', function() {
      expect(vm.recipients.length).toBe(6);
      expect(vm.recipients[0].roleId).toBe('1');

      vm.removeRecipient(vm.recipients[0]);
      expect(vm.notifications.roleNotifications.length).toBe(1);
      expect(vm.notifications.roleNotifications[0].roleId).toBe('2');

      expect(vm.recipients.length).toBe(5);
      expect(vm.recipients[0].emailAddress).toBe('bob@test.com');
    });

    it('removes email recipient entry from original notifications and updates vm.recipients array', function() {
      expect(vm.recipients.length).toBe(6);
      expect(vm.recipients[1].emailAddress).toBe('bob@test.com');

      vm.removeRecipient(vm.recipients[1]);
      expect(vm.notifications.userNotifications.length).toBe(1);
      expect(vm.notifications.userNotifications[0].emailAddress).toBe('zoo@test.com');

      expect(vm.recipients.length).toBe(5);
      expect(vm.recipients[1].roleId).toBe('2');
    });

    it('removes jira recipient entry from original notifications and updates vm.recipients array', function() {
      expect(vm.recipients.length).toBe(6);
      expect(vm.recipients[3].projectKey).toBe('key1');

      vm.removeRecipient(vm.recipients[3]);
      expect(vm.notifications.jiraNotifications.length).toBe(1);
      expect(vm.notifications.jiraNotifications[0].projectKey).toBe('key2');

      expect(vm.recipients.length).toBe(5);
      expect(vm.recipients[3].projectKey).toBe('key2');
    });

    it('maintains the order of vm.recipients', function() {
      vm.addEmailRecipient('aaaaaaa@test.com');
      vm.removeRecipient(vm.recipients[0]);
      expect(vm.recipients.length).toBe(6);
      // should not change the sorted order of remaining entries
      expect(vm.recipients[0].emailAddress).toBe('bob@test.com');
      expect(vm.recipients[1].roleId).toBe('2');
      expect(vm.recipients[2].projectKey).toBe('key1');
      expect(vm.recipients[3].projectKey).toBe('key2');
      expect(vm.recipients[4].emailAddress).toBe('zoo@test.com');
      expect(vm.recipients[5].emailAddress).toBe('aaaaaaa@test.com');

    });
  });

  describe('addRecipient()', function() {
    var vm,
        keypressEvent;
    beforeEach(function() {
      var notifications = {
        userNotifications: [
          {
            emailAddress: 'zed@test.com',
            stageIds: ['proxy', 'build']
          },
          {
            emailAddress: 'bob@test.com',
            stageIds: ['develop']
          }
        ],
        roleNotifications: [
          {
            roleId: '1',
            stageIds: ['proxy', 'build']
          },
          {
            roleId: '3',
            stageIds: ['develop']
          }
        ],
        jiraNotifications: [
          {
            projectKey: 'key1',
            issueTypeId: 1,
            stageIds: ['proxy', 'build']
          },
          {
            projectKey: 'key2',
            issueTypeId: 2,
            stageIds: ['develop']
          }
        ]
      };
      vm = initController(notifications);
      vm.addRecipientForm = jasmine.createSpyObj('addRecipientForm', ['$setPristine']);
      keypressEvent = jasmine.createSpyObj('keypressEvent', ['preventDefault']);
    });

    it('handles keypress event and prevents default if input is invalid', function() {
      expect(vm.notifications.userNotifications.length).toBe(2);
      vm.recipientType = vm.recipientTypes.EMAIL;
      vm.addRecipient(keypressEvent);
      expect(keypressEvent.preventDefault).toHaveBeenCalled();
      expect(vm.notifications.userNotifications.length).toBe(2);
    });

    it('handles keypress event and prevents default if input is valid', function() {
      expect(vm.notifications.userNotifications.length).toBe(2);
      vm.recipientToAdd = 'user-recipient@test.com';
      vm.recipientType = vm.recipientTypes.EMAIL;
      vm.addRecipient(keypressEvent);
      expect(keypressEvent.preventDefault).toHaveBeenCalled();
      expect(vm.notifications.userNotifications.length).toBe(3);
    });

    it('ignores invalid input', function() {
      expect(vm.notifications.userNotifications.length).toBe(2);
      vm.recipientType = vm.recipientTypes.EMAIL;
      vm.recipientToAdd = undefined;
      vm.addRecipient();
      expect(vm.notifications.userNotifications.length).toBe(2);
    });

    it('clears email input', function() {
      vm.recipientType = vm.recipientTypes.EMAIL;
      vm.recipientToAdd = 'test-recipient@test.com';
      vm.addRecipient();
      expect(vm.recipientToAdd).toBeUndefined();
    });

    it('sets form to pristine state', function() {
      vm.recipientType = vm.recipientTypes.EMAIL;
      vm.recipientToAdd = 'test-recipient@test.com';
      vm.addRecipient();
      expect(vm.addRecipientForm.$setPristine).toHaveBeenCalled();
    });

    describe('addEmailRecipient()', function() {
      it('adds userNotifications to policy notifications and updates vm.recipients', function() {
        expect(vm.recipients.length).toBe(6);

        vm.addEmailRecipient('user-recipient@test.com');

        expect(vm.notifications.userNotifications.length).toBe(3);
        expect(vm.notifications.userNotifications[2].emailAddress).toBe('user-recipient@test.com');
        expect(vm.notifications.userNotifications[2].stageIds).toEqual([]);

        expect(vm.recipients.length).toBe(7);
        expect(vm.recipients[6].emailAddress).toBe('user-recipient@test.com');
      });

      it('adds to the end of vm.recipients array, and does not sort', function() {

        vm.addEmailRecipient('aaaaaaaa@test.com');
        expect(vm.recipients[6].emailAddress).toBe('aaaaaaaa@test.com');
      });

      it('prevents from adding duplicate emails', function() {

        vm.addEmailRecipient('bob@test.com');

        expect(vm.recipients.length).toBe(6);
        expect(vm.notifications.userNotifications.length).toBe(2);
      });
    });

    describe('addRoleRecipient()', function() {
      it('adds roleNotifications to policy notifications and updates vm.recipients', function() {
        expect(vm.recipients.length).toBe(6);
        vm.addRoleRecipient('2');

        expect(vm.notifications.roleNotifications.length).toBe(3);
        expect(vm.notifications.roleNotifications[2].roleId).toBe('2');
        expect(vm.notifications.roleNotifications[2].stageIds).toEqual([]);

        expect(vm.recipients.length).toBe(7);
        expect(vm.recipients[6].roleId).toBe('2');
      });

      it('adds to the end of vm.recipients array, and does not sort', function() {
        vm.addRoleRecipient('2');
        expect(vm.recipients[6].roleId).toBe('2');
      });
    });

    describe('addJiraRecipient()', function() {
      it('adds jiraNotifications to policy notifications and updates vm.recipients', function() {
        vm.recipientType = vm.recipientTypes.JIRA;
        vm.recipientToAdd = {
          key: 'key3'
        };
        vm.recipientToAddIssueType = {
          id: 3
        };

        vm.addRecipient();

        expect(vm.notifications.jiraNotifications.length).toBe(3);
        expect(vm.notifications.jiraNotifications[2].projectKey).toBe('key3');
        expect(vm.notifications.jiraNotifications[2].issueTypeId).toBe(3);

        expect(vm.recipients.length).toBe(7);
        expect(vm.recipients[6].projectKey).toBe('key3');
        expect(vm.recipients[6].issueTypeId).toBe(3);
      });
    });
  });

  describe('getAvailableRoles()', function() {
    it('returns only roles that are not present in policy notifications', function() {
      var notifications = {
        roleNotifications: []
      };
      var vm = initController(notifications);

      expect(vm.getAvailableRoles()).toEqual(membershipMapping.membersByRole);

      // add
      vm.addRoleRecipient('1');
      expect(vm.getAvailableRoles()).toEqual(membershipMapping.membersByRole.slice(1));

      vm.addRoleRecipient('2');
      expect(vm.getAvailableRoles()).toEqual(membershipMapping.membersByRole.slice(2));

      vm.addRoleRecipient('3');
      expect(vm.getAvailableRoles()).toEqual([]);

      // remove
      vm.removeRecipient(vm.recipients[2]);
      expect(vm.getAvailableRoles()).toEqual(membershipMapping.membersByRole.slice(2));
    });
  });

  describe('availableJiraProjects', function() {
    it('returns only projects that are not present in policy notifications', function() {
      var notifications = {
        jiraNotifications: []
      };
      var vm = initController(notifications, true);
      vm.addRecipientForm = jasmine.createSpyObj('addRecipientForm', ['$setPristine']);

      expect(vm.availableJiraProjects).toEqual(jiraProjects);

      vm.recipientType = vm.recipientTypes.JIRA;
      vm.recipientToAdd = {
        key: 'key1'
      };
      vm.recipientToAddIssueType = {
        id: 1
      };

      vm.addRecipient();
      expect(vm.availableJiraProjects).toEqual(jiraProjects.slice(1));

      vm.removeRecipient(vm.recipients[0]);
      expect(vm.availableJiraProjects).toEqual(jiraProjects);
    });
  });

  describe('getEmails()', function() {
    it('returns empty array when there are no email recipients', function() {
      var notifications = {
        userNotifications: []
      };
      var vm = initController(notifications);
      expect(vm.getEmails()).toEqual([]);
    });

    it('returns emails of all email recipients', function() {
      var notifications = {
        userNotifications: [
          {
            emailAddress: 'zed@test.com',
            stageIds: ['proxy', 'build']
          },
          {
            emailAddress: 'bob@test.com',
            stageIds: ['develop']
          }
        ]
      };
      var vm = initController(notifications);

      expect(vm.getEmails()).toEqual(['zed@test.com', 'bob@test.com']);

      vm.removeRecipient(vm.recipients[0]);
      expect(vm.getEmails()).toEqual(['zed@test.com']);

      vm.removeRecipient(vm.recipients[0]);
      expect(vm.getEmails()).toEqual([]);
    });
  });

  describe('toggleStage()', function() {
    it('updates recipient', function() {
      var recipient = {
        emailAddress: 'zed@test.com',
        stageIds: ['proxy', 'build']
      };
      var notifications = {
        userNotifications: [recipient]
      };
      var vm = initController(notifications);

      vm.toggleStage(recipient, 'proxy');
      expect(recipient.stageIds.length).toBe(1);
      expect(recipient.stageIds[0]).toBe('build');

      vm.toggleStage(recipient, 'develop');
      expect(recipient.stageIds.length).toBe(2);
      expect(recipient.stageIds).toContain('build');
      expect(recipient.stageIds).toContain('develop');
    });
  });

  describe('isStageApplicable()', function() {
    it('disables proxy stage for jira notifications', function() {
      var vm = initController({});
      var isApplicable = vm.isStageApplicable({
        projectKey: 'key',
        issueTypeId: 'type'
      }, 'proxy');
      expect(isApplicable).toBe(false);

      isApplicable = vm.isStageApplicable({
        projectKey: 'key',
        issueTypeId: 'type'
      }, 'develop');
      expect(isApplicable).toBe(true);
    });

    it('enables proxy stage for other notification types', function() {
      var vm = initController({});
      var isApplicable = vm.isStageApplicable({
        emailAddress: 'foo@sonatype.com'
      }, 'proxy');
      expect(isApplicable).toBe(true);

      isApplicable = vm.isStageApplicable({
        roleId: 'foo'
      }, 'proxy');
      expect(isApplicable).toBe(true);
    });
  });

  describe('getDisplayName()', function() {
    it('returns recipient email for userNotifications', function() {
      var recipient = {
        emailAddress: 'test1@test.com',
        stageIds: []
      };
      var notifications = {
        userNotifications: [recipient]
      };
      var vm = initController(notifications);

      expect(vm.getDisplayName(recipient)).toBe('test1@test.com');
    });

    it('returns role name for roleNotifications', function() {
      var recipient = {
        roleId: '1',
        stageIds: []
      };
      var notifications = {
        roleNotifications: [recipient]
      };
      var vm = initController(notifications);

      expect(vm.getDisplayName(recipient)).toBe('Application Evaluator');
    });

    it('returns jira project name and issue type for jiraNotifications', function() {
      var recipient = {
        projectKey: 'key1',
        issueTypeId: 1,
        stageIds: []
      };
      var notifications = {
        jiraNotifications: [recipient]
      };
      var vm = initController(notifications, true);

      expect(vm.getDisplayName(recipient)).toBe('Project One (Bug)');
    });

    it('returns webhook url for webhookNotifications if not having a description', function() {
      var recipient = {
        webhookId: 'webhook1'
      };
      var notifications = {
        webhookNotifications: [recipient]
      };
      var vm = initController(notifications, true);

      expect(vm.getDisplayName(recipient)).toBe('Webhook: url1');
    });

    it('returns webhook description for webhookNotifications if having a description', function() {
      var recipient = {
        webhookId: 'webhook2'
      };
      var notifications = {
        webhookNotifications: [recipient]
      };
      var vm = initController(notifications, true);

      expect(vm.getDisplayName(recipient)).toBe('Webhook: description2');
    });

    it('returns webhook id for webhookNotifications if unable to find matching webhook', function() {
      var recipient = {
        webhookId: 'unknown-webhook'
      };
      var notifications = {
        webhookNotifications: [recipient]
      };
      var vm = initController(notifications, true);

      expect(vm.getDisplayName(recipient)).toBe('Undefined webhook: unknown-webhook');
    });

    it('returns webhook id for webhookNotifications if unable to load webhooks', function() {
      getWebhooks.respond(500, 'error');
      var recipient = {
        webhookId: 'webhook1'
      };
      var notifications = {
        webhookNotifications: [recipient]
      };
      var vm = initController(notifications, true);

      expect(vm.getDisplayName(recipient)).toBe('Undefined webhook: webhook1');
    });
  });
});
