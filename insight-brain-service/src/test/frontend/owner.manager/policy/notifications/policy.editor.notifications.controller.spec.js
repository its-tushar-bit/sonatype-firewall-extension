describe("policy.editor.notifications.controller.spec.js", function() {
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

  var initController,
      scope;

  beforeEach(module('owner.manager.module'));

  beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMAppLocations) {
    scope = $rootScope.$new();

    initController = function(notifications) {
      var ctrlFn = $controller('policy.editor.notifications.controller', {
        $scope: scope
      }, true);

      ctrlFn.instance.notifications = notifications;
      var vm = ctrlFn();
      $httpBackend.flush();
      scope.vm = vm; // needed to be able to test scope.$watch
      return vm;
    };

    $httpBackend.whenGET('/rest/policy/stages?context=all').respond([]);
    $httpBackend.whenGET(CLMAppLocations.getRoleMappingUrl()).respond(membershipMapping);

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

    it('handles no notifications', function() {
      var vm = initController({});
      expect(vm.recipients.length).toBe(0);
    });

    it('sorts vm.recipients by email or roleName', function() {

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
        ]
      };

      var vm = initController(notifications);
      expect(vm.recipients[0].roleId).toBe('1');
      expect(vm.recipients[1].emailAddress).toBe('bob@test.com');
      expect(vm.recipients[2].roleId).toBe('2');
      expect(vm.recipients[3].emailAddress).toBe('zoo@test.com');

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
        ]
      };
      vm = initController(notifications);
    });

    it('removes role recipient entry from original notifications and updates vm.recipients array', function() {
      expect(vm.recipients.length).toBe(4);
      expect(vm.recipients[0].roleId).toBe('1');

      vm.removeRecipient(vm.recipients[0]);
      expect(vm.notifications.roleNotifications.length).toBe(1);
      expect(vm.notifications.roleNotifications[0].roleId).toBe('2');

      expect(vm.recipients.length).toBe(3);
      expect(vm.recipients[0].emailAddress).toBe('bob@test.com');
    });

    it('removes email recipient entry from original notifications and updates vm.recipients array', function() {
      expect(vm.recipients.length).toBe(4);
      expect(vm.recipients[1].emailAddress).toBe('bob@test.com');

      vm.removeRecipient(vm.recipients[1]);
      expect(vm.notifications.userNotifications.length).toBe(1);
      expect(vm.notifications.userNotifications[0].emailAddress).toBe('zoo@test.com');

      expect(vm.recipients.length).toBe(3);
      expect(vm.recipients[1].roleId).toBe('2');
    });

    it('maintains the order of vm.recipients', function() {
      vm.addEmailRecipient('aaaaaaa@test.com');
      vm.removeRecipient(vm.recipients[0]);
      expect(vm.recipients.length).toBe(4);
      // should not change the sorted order of remaining entries
      expect(vm.recipients[0].emailAddress).toBe('bob@test.com');
      expect(vm.recipients[1].roleId).toBe('2');
      expect(vm.recipients[2].emailAddress).toBe('zoo@test.com');
      expect(vm.recipients[3].emailAddress).toBe('aaaaaaa@test.com');

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
        expect(vm.recipients.length).toBe(4);

        vm.addEmailRecipient('user-recipient@test.com');

        expect(vm.notifications.userNotifications.length).toBe(3);
        expect(vm.notifications.userNotifications[2].emailAddress).toBe('user-recipient@test.com');
        expect(vm.notifications.userNotifications[2].stageIds).toEqual([]);

        expect(vm.recipients.length).toBe(5);
        expect(vm.recipients[4].emailAddress).toBe('user-recipient@test.com');
      });

      it('adds to the end of vm.recipients array, and does not sort', function() {

        vm.addEmailRecipient('aaaaaaaa@test.com');
        expect(vm.recipients[4].emailAddress).toBe('aaaaaaaa@test.com');
      });

      it('prevents from adding duplicate emails', function() {

        vm.addEmailRecipient('bob@test.com');

        expect(vm.recipients.length).toBe(4);
        expect(vm.notifications.userNotifications.length).toBe(2);
      });
    });

    describe('addRoleRecipient()', function() {
      it('adds roleNotifications to policy notifications and updates vm.recipients', function() {
        expect(vm.recipients.length).toBe(4);
        vm.addRoleRecipient('2');

        expect(vm.notifications.roleNotifications.length).toBe(3);
        expect(vm.notifications.roleNotifications[2].roleId).toBe('2');
        expect(vm.notifications.roleNotifications[2].stageIds).toEqual([]);

        expect(vm.recipients.length).toBe(5);
        expect(vm.recipients[4].roleId).toBe('2');
      });

      it('adds to the end of vm.recipients array, and does not sort', function() {
        vm.addRoleRecipient('2');
        expect(vm.recipients[4].roleId).toBe('2');
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

  });
});
