/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxButton, NxFontAwesomeIcon, NxTextInput, NxTooltip } from '@sonatype/react-shared-components';
import { faCopy } from '@fortawesome/free-solid-svg-icons/faCopy';

import * as enzymeUtils from '../../../../enzymeUtils';
import UserTokenDisplay from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/UserTokenDisplay';

describe('UserTokenDisplay', function () {
  let minimalProps, getShallowComponent, getMountedComponent;

  beforeEach(function () {
    minimalProps = {
      userToken: {
        userCode: 'userCodeFromBackend',
        passCode: 'passCodeFromBackend',
      },
    };

    getShallowComponent = enzymeUtils.getShallowComponent(UserTokenDisplay, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(UserTokenDisplay, minimalProps);
  });

  it('renders a form with 2 inputs for the user token information', function () {
    const component = getShallowComponent(),
      inputs = component.find(NxTextInput);
    expect(component).toMatchSelector('form');
    expect(inputs.length).toBe(2);
  });

  it('prevents default submit of the form', function () {
    const preventDefaultSpy = jasmine.createSpy('preventDefault');
    const form = getShallowComponent();

    form.simulate('submit', { preventDefault: preventDefaultSpy });
    expect(preventDefaultSpy).toHaveBeenCalled();
  });

  it('renders an NxInput with label User Code with the value of userCode from the userToken prop', function () {
    const component = getMountedComponent(),
      userCodeInputContainer = component.find('.nx-form-group').at(0);

    const inputTitle = userCodeInputContainer.find('span');
    expect(inputTitle).toHaveText('User Code');

    const userCodeInput = userCodeInputContainer.find(NxTextInput);
    expect(userCodeInput).toHaveProp('value', 'userCodeFromBackend');
    expect(userCodeInput).toHaveProp('isPristine', true);
  });

  describe('Copy User Code to clipboard button', function () {
    it('renders a button to copy the userCode input content into the clipboard', function () {
      const component = getMountedComponent(),
        userCodeInputCopyButtonContainer = component.find('.nx-btn-bar').at(0);

      const buttonTooltip = userCodeInputCopyButtonContainer.find(NxTooltip);
      expect(buttonTooltip).toHaveProp('title', 'Copy to clipboard');

      const button = userCodeInputCopyButtonContainer.find(NxButton);
      expect(button).toHaveProp('variant', 'tertiary');
      expect(button).toHaveProp('onClick', jasmine.any(Function));
      const buttonIcon = button.find(NxFontAwesomeIcon);
      expect(buttonIcon).toHaveProp('icon', faCopy);
    });

    it('calls copyToClipboard function on click with the value of the userCode from userToken', function () {
      const copyToClipboardSpy = jasmine.createSpy('copyToClipboard');
      navigator.clipboard.writeText = copyToClipboardSpy;
      const component = getMountedComponent(),
        userCodeInputCopyButtonContainer = component.find('.nx-btn-bar').at(0),
        button = userCodeInputCopyButtonContainer.find(NxButton);

      button.simulate('click');
      expect(copyToClipboardSpy).toHaveBeenCalledWith('userCodeFromBackend');
    });
  });

  it('renders an NxInput with label Passcode with the value of passCode from the userToken prop', function () {
    const component = getMountedComponent(),
      passCodeInputContainer = component.find('.nx-form-group').at(1);

    const inputTitle = passCodeInputContainer.find('span');
    expect(inputTitle).toHaveText('Passcode');

    const passCodeInput = passCodeInputContainer.find(NxTextInput);
    expect(passCodeInput).toHaveProp('value', 'passCodeFromBackend');
    expect(passCodeInput).toHaveProp('isPristine', true);
  });

  describe('Copy Passcode to clipboard button', function () {
    it('renders a button to copy the passCode input content into the clipboard', function () {
      const component = getMountedComponent(),
        passCodeInputCopyButtonContainer = component.find('.nx-btn-bar').at(1);

      const buttonTooltip = passCodeInputCopyButtonContainer.find(NxTooltip);
      expect(buttonTooltip).toHaveProp('title', 'Copy to clipboard');

      const button = passCodeInputCopyButtonContainer.find(NxButton);
      expect(button).toHaveProp('variant', 'tertiary');
      expect(button).toHaveProp('onClick', jasmine.any(Function));
      const buttonIcon = button.find(NxFontAwesomeIcon);
      expect(buttonIcon).toHaveProp('icon', faCopy);
    });

    it('calls copyToClipboard function on click with the value of the passCode from userToken', function () {
      const copyToClipboardSpy = jasmine.createSpy('copyToClipboard');
      navigator.clipboard.writeText = copyToClipboardSpy;
      const component = getMountedComponent(),
        passCodeInputCopyButtonContainer = component.find('.nx-btn-bar').at(1),
        button = passCodeInputCopyButtonContainer.find(NxButton);

      button.simulate('click');
      expect(copyToClipboardSpy).toHaveBeenCalledWith('passCodeFromBackend');
    });
  });
});
