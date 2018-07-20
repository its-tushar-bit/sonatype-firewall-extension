describe('proprietary.matchers.modal.spec', function() {

  var Modal,
      proprietaryMatchersModal;

  beforeEach(module('proprietary.matchers'));

  beforeEach(function() {
    Modal = {
      open: function(conf) {
        this.conf = conf;
      }
    };
    module(function ($provide) {
      $provide.value('Modal', Modal);
    });
  });

  beforeEach(inject(function($injector) {
    CLM = {
      assetsPath: '../test/path/'
    };
    proprietaryMatchersModal = $injector.get('proprietary.matchers.modal');
  }));

  afterEach(function() {
    CLM = {};
  });

  it('uses CLM.assetsPath in template URL', function() {
    proprietaryMatchersModal.open();
    expect(Modal.conf.templateUrl).toBe('../test/path/cip/proprietary.matchers.modal.html');

    CLM.assetsPath = '../new/path/';
    proprietaryMatchersModal.open();
    expect(Modal.conf.templateUrl).toBe('../new/path/cip/proprietary.matchers.modal.html');
  });
});
