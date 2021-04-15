/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userModule from '../../../../main/frontend/security/UserModule';
import LegacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

/* global describe, beforeEach, it, expect, spyOn */
describe('userForm', function () {
  var $componentController, $rootScope, UserStore, Dialog;

  function mkController(bindings, scope) {
    return $componentController(
      'userForm',
      {
        UserStore: UserStore,
        Dialog: Dialog,
        $scope: scope,
      },
      bindings
    );
  }

  beforeEach(
    angular.mock.module(userModule.name, LegacyConfigurationModule.name)
  );
  beforeEach(inject(function (
    _$componentController_,
    _$rootScope_,
    _UserStore_,
    _Dialog_
  ) {
    $componentController = _$componentController_;
    $rootScope = _$rootScope_;
    UserStore = _UserStore_;
    Dialog = _Dialog_;
  }));

  it('sets its user property to a clone of whatever it is passed in as', function () {
    var originalUser = UserStore.create();

    originalUser.$updateOriginal({
      id: '123456',
      firstName: 'Alan',
    });

    var controller = mkController({ user: originalUser });
    var user = controller.user;

    expect(user).not.toBe(originalUser);
    expect(user.id).toBe(originalUser.id);
    expect(user.firstName).toBe(originalUser.firstName);
  });

  it("sets the user property to a fresh UserStore model if it is not defined or doesn't have an id", function () {
    var noUserController = mkController();

    expect(noUserController.user).toBeDefined();
    expect(noUserController.user.id).toBe(null);

    var noIdUser = UserStore.create();
    noIdUser.$updateOriginal({ firstName: 'Alan' });

    var noIdUserController = mkController({ user: noIdUser });

    expect(noIdUserController.user).toBeDefined();
    expect(noIdUserController.user.id).toBe(null);
    expect(noIdUserController.user.firstName).toBe(null);
  });

  it('prevents page navigation when the user model is dirty', function () {
    var noUserParentScope = $rootScope.$new(),
      noUserScope = noUserParentScope.$new(),
      cleanUserParentScope = $rootScope.$new(),
      cleanUserScope = cleanUserParentScope.$new(),
      dirtyUser = UserStore.create(),
      dirtyUserParentScope = $rootScope.$new(),
      dirtyUserScope = dirtyUserParentScope.$new();

    mkController({}, noUserScope);
    mkController({ user: UserStore.create() }, cleanUserScope);
    var dirtyUserController = mkController({ user: dirtyUser }, dirtyUserScope);

    dirtyUserController.user.firstName = 'Alan';

    expect(
      noUserParentScope.$broadcast('pageChangeStarted').defaultPrevented
    ).toEqual(false);
    expect(
      cleanUserParentScope.$broadcast('pageChangeStarted').defaultPrevented
    ).toEqual(false);
    expect(
      dirtyUserParentScope.$broadcast('pageChangeStarted').defaultPrevented
    ).toEqual(true);
  });

  describe('saveClick', function () {
    var userId = '123556',
      username = 'alan',
      otherUsername = 'bruce',
      user,
      saveDeferred,
      controller;

    beforeEach(inject(function ($q) {
      var userEditMap = {},
        otherUser,
        otherUserId = 'asdfasdffbhweg';

      saveDeferred = $q.defer();

      user = UserStore.create();
      user.$updateOriginal({
        id: userId,
        usernameLowercase: username,
      });

      otherUser = UserStore.create();
      otherUser.$updateOriginal({
        id: otherUserId,
        usernameLowercase: otherUsername,
      });

      userEditMap[userId] = user;
      userEditMap[otherUserId] = otherUser;

      controller = mkController({
        user: user,
        context: {
          userEditMap: userEditMap,
          users: [user, otherUser],
        },
      });

      // mock $save method
      spyOn(controller.user, '$save').and.callFake(function () {
        return saveDeferred.promise;
      });
    }));

    it('sets vm.saving while the $save promise is unresolved', function (done) {
      controller.onSave = function () {};

      expect(controller.saving).toBeFalsy();

      controller.saveClick();
      expect(controller.saving).toBe(true);

      saveDeferred.promise.then(function () {
        expect(controller.saving).toBe(false);

        done();
      });

      saveDeferred.resolve();
      $rootScope.$digest();
    });

    it('clears alerts when saving', function () {
      controller.alerts = [1, 2, 3];

      controller.saveClick();
      expect(controller.alerts).toBeFalsy();
    });

    it('does not attempt to save if vm.saving is true', function () {
      controller.saving = true;

      controller.saveClick();

      expect(controller.user.$save).not.toHaveBeenCalled();
    });

    it('sets an alert and clears the saving flag on save failure', function (done) {
      var error = { data: 'error message!' };

      controller.saveClick();

      saveDeferred.promise.then(null, function () {
        expect(controller.alerts.length).toBeGreaterThan(0);
        expect(controller.alerts[0].msg).toContain('error message!');

        expect(controller.saving).toEqual(false);

        done();
      });

      saveDeferred.reject(error);
      $rootScope.$digest();
    });

    it('calls vm.onSave after saving if it exists', function (done) {
      controller.onSave = function () {};
      spyOn(controller, 'onSave');

      controller.saveClick();
      expect(controller.onSave).not.toHaveBeenCalled();

      saveDeferred.promise.then(function () {
        expect(controller.onSave).toHaveBeenCalled();

        done();
      });

      saveDeferred.resolve();
      $rootScope.$digest();
    });
  });

  describe('cancelClick', function () {
    it('immediately calls vm.onCancel if the user is not dirty', function () {
      var controller = mkController({ onCancel: function () {} });
      spyOn(controller, 'onCancel');

      controller.cancelClick();

      expect(controller.onCancel).toHaveBeenCalled();
    });

    it('creates a dialog which prompts before reverting the model and calling onCancel, if the model is dirty', function () {
      var user = UserStore.create();
      user.$updateOriginal({ id: '12345', firstName: 'Alan' });

      var controller = mkController({
        onCancel: function () {},
        user: user,
      });

      spyOn(Dialog, 'open');
      spyOn(controller, 'onCancel');

      // dirty the user model
      controller.user.firstName = 'Bruce';

      controller.cancelClick();

      expect(Dialog.open).toHaveBeenCalled();

      // verify that model is not reverted yet
      expect(controller.user.firstName).toBe('Bruce');

      var dialogBtnConfigs = Dialog.open.calls.argsFor(0)[0].buttons,
        dialogBtnNames = dialogBtnConfigs.map(function (btn) {
          return btn.name;
        }),
        continueBtnIndex = dialogBtnNames.indexOf('Continue'),
        continueBtn = dialogBtnConfigs[continueBtnIndex],
        continueBtnCallback = continueBtn.click;

      // simulate user clicking through the dialog
      continueBtnCallback();

      expect(controller.onCancel).toHaveBeenCalled();

      // verify that model is reverted
      expect(controller.user.firstName).toBe('Alan');
    });
  });
});
