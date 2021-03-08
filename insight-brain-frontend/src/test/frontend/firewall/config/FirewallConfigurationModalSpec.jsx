/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {NxForm, NxLoadError, NxModal, NxSubmitMask} from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../enzymeUtils';
import FirewallConfigurationModal from '../../../../main/frontend/firewall/config/FirewallConfigurationModal';

describe('FirewallConfigurationModal', function() {
  let minimalProps,
      toggleAutoUnquarantineEnabledSpy,
      saveConfigurationSpy,
      loadConfigurationSpy,
      closeConfigurationModalSpy,
      getShallowComponent,
      getMountedComponent;

  beforeEach(function() {
    toggleAutoUnquarantineEnabledSpy = jasmine.createSpy('toggleAutoUnquarantineEnabled');
    saveConfigurationSpy = jasmine.createSpy('saveConfiguration');
    loadConfigurationSpy = jasmine.createSpy('loadConfiguration');
    closeConfigurationModalSpy = jasmine.createSpy('closeConfigurationModal');

    minimalProps = {
      loadedConfiguration: true,
      loadConfigurationError: null,
      submitMaskSuccessState: null,
      saveConfigurationError: null,
      isDirty: false,
      autoUnquarantineEnabled: true,
      toggleAutoUnquarantineEnabled: toggleAutoUnquarantineEnabledSpy,
      saveConfiguration: saveConfigurationSpy,
      loadConfiguration: loadConfigurationSpy,
      closeConfigurationModal: closeConfigurationModalSpy
    };

    getShallowComponent = enzymeUtils.getShallowComponent(FirewallConfigurationModal, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(FirewallConfigurationModal, minimalProps);
  });

  it('renders an NxModal', function() {
    const component = getShallowComponent(),
        modal = component.find(NxModal);

    expect(modal).toExist();
    expect(modal).toHaveProp('id', 'firewall-configuration-modal');
    expect(modal).toHaveProp('onClose');

    const form = modal.find(NxForm);
    expect(form).toHaveProp('onSubmit', saveConfigurationSpy);
    expect(form).toHaveProp('loadError', null);
    expect(form).toHaveProp('loading', false);
    expect(form).toHaveProp('doLoad', loadConfigurationSpy);
    expect(form).toHaveProp('submitMaskMessage', 'Saving…');
    expect(form).toHaveProp('submitError', null);
    expect(form).toHaveProp('submitMaskState', null);
    expect(form).toHaveProp('submitBtnText', 'Save Changes');
    expect(form).toHaveProp('validationErrors', 'There are no changes to save.');
    expect(form).toHaveProp('onCancel', closeConfigurationModalSpy);

    const modalTitle = component.find('.nx-modal-header');
    expect(modalTitle).toExist();
    expect(modalTitle).toIncludeText('Auto Release From Quarantine Configuration');

    const modalContent = component.find('.nx-modal-content');
    expect(modalContent).toExist();

    const releaseIntegrityToggle = modalContent.find('#auto-unquarantine-toggle');
    expect(releaseIntegrityToggle).toExist();
    expect(releaseIntegrityToggle).toHaveProp('isChecked', true);
    expect(releaseIntegrityToggle).toHaveProp('onChange', toggleAutoUnquarantineEnabledSpy);
    expect(releaseIntegrityToggle).toHaveText('Release Integrity Policy');
  });

  it('calls closeConfigurationModal when form is cancelled', function() {
    const component = getShallowComponent(),
        form = component.find(NxForm);

    form.simulate('cancel');
    expect(closeConfigurationModalSpy).toHaveBeenCalled();
  });

  it('calls saveConfiguration when form is submitted', function() {
    const component = getShallowComponent(),
        form = component.find(NxForm);

    const releaseIntegrityToggle = component.find('#auto-unquarantine-toggle');
    releaseIntegrityToggle.simulate('change');
    expect(toggleAutoUnquarantineEnabledSpy).toHaveBeenCalled();

    form.simulate('submit');
    expect(saveConfigurationSpy).toHaveBeenCalled();
  });

  it('calls toggleAutoUnquarantineEnabled when toggle is changed', function() {
    const component = getShallowComponent(),
        releaseIntegrityToggle = component.find('#auto-unquarantine-toggle');

    releaseIntegrityToggle.simulate('change');
    expect(toggleAutoUnquarantineEnabledSpy).toHaveBeenCalled();
  });

  it('renders a submit Mask if saving is in progress', function() {
    const component = getMountedComponent({submitMaskSuccessState: false}),
        submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Saving…');
  });

  it('renders a success mask if saving is successful', function() {
    const component = getMountedComponent({submitMaskSuccessState: true}),
        submitMask = component.find(NxSubmitMask);

    expect(submitMask).toExist();
    expect(submitMask).toHaveText('Success!');
  });

  it('renders an error if something went wrong', function() {
    const component = getMountedComponent({saveConfigurationError: 'err!'}),
        err = component.find(NxLoadError);

    expect(err).toExist();
    expect(err).toHaveProp('error', 'err!');
    expect(err).toHaveProp('titleMessage', 'An error occurred saving data.');
    expect(err).toHaveProp('retryHandler', saveConfigurationSpy);
  });
});
