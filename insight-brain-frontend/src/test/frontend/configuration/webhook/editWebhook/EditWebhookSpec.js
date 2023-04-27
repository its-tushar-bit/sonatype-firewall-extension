/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxCheckbox,
  NxStatefulForm,
  NxInfoAlert,
  nxTextInputStateHelpers,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../enzymeUtils';
import EditWebhook from 'MainRoot/configuration/webhook/editWebhook/EditWebhook';
import * as routerContext from 'MainRoot/react/RouterStateContext';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

const { initialState, userInput } = nxTextInputStateHelpers;

describe('EditWebhook', () => {
  let getShallow, minProps;

  beforeEach(() => {
    minProps = {
      availableEventTypes: ['Policy Management', 'Application Evaluation', 'Violation Alert'],
      selectedEventTypes: [],
      isAppWebhooksSupported: true,
      isRepoWebhooksSupported: true,
      inputFields: {
        url: initialState(''),
        description: initialState(''),
        secretKey: initialState(''),
      },
      router: {
        currentParams: {},
      },
    };

    getShallow = enzymeUtils.getShallowComponent(EditWebhook, minProps);
  });

  it('renders a MenuBarBackButton with correct stateName prop', function () {
    const component = getShallow();
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('stateName', 'listWebhooks');
  });

  describe('when application webhooks are not supported ', () => {
    let component;

    beforeEach(() => {
      component = getShallow({
        isAppWebhooksSupported: false,
      });
    });

    it('renders info alert in the form', () => {
      const form = component.find(NxStatefulForm);
      expect(form).toExist();
      expect(form.find(NxInfoAlert)).toExist();
    });

    it('disables Application Evaluation checkbox', () => {
      const appEvaluationCheckbox = component.find(NxCheckbox).at(1);
      expect(appEvaluationCheckbox).toHaveProp('children', 'Application Evaluation');
      expect(appEvaluationCheckbox).toHaveProp('disabled', true);
    });
  });

  describe('when application webhooks are supported ', () => {
    let component;

    beforeEach(() => {
      component = getShallow({
        isAppWebhooksSupported: true,
      });
    });
    it('does not render info alert in the form', () => {
      const form = component.find(NxStatefulForm);
      expect(form).toExist();
      expect(form.find(NxInfoAlert)).not.toExist();
    });

    it('does not disable Application Evaluation checkbox', () => {
      const appEvaluationCheckbox = component.find(NxCheckbox).at(1);
      expect(appEvaluationCheckbox).toHaveProp('children', 'Application Evaluation');
      expect(appEvaluationCheckbox).toHaveProp('disabled', false);
    });
  });

  describe('event types checkboxes', () => {
    it('renders checkbox with proper label and checkboxIds for each event type', () => {
      const component = getShallow();

      const checkboxes = component.find(NxCheckbox);
      expect(checkboxes.length).toBe(3);

      expect(checkboxes.at(0)).toHaveProp('children', 'Policy Management');
      expect(checkboxes.at(0)).toHaveProp('checkboxId', 'Policy-Management');

      expect(checkboxes.at(1)).toHaveProp('children', 'Application Evaluation');
      expect(checkboxes.at(1)).toHaveProp('checkboxId', 'Application-Evaluation');
      expect(checkboxes.at(1)).toHaveProp('disabled', false);

      expect(checkboxes.at(2)).toHaveProp('children', 'Violation Alert');
      expect(checkboxes.at(2)).toHaveProp('checkboxId', 'Violation-Alert');
    });

    it('renders selected checkboxes for selectedEventTypes', () => {
      const component = getShallow({
        selectedEventTypes: ['Policy Management', 'Violation Alert'],
      });

      const checkboxes = component.find(NxCheckbox);
      expect(checkboxes.at(0)).toHaveProp('isChecked', true);
      expect(checkboxes.at(1)).toHaveProp('isChecked', false);
      expect(checkboxes.at(2)).toHaveProp('isChecked', true);
    });

    it('calls toggleEventType action on change with proper event type', () => {
      const toggleEventTypeSpy = jasmine.createSpy('toggleEventType');
      const component = getShallow({
        toggleEventType: toggleEventTypeSpy,
      });

      const checkboxes = component.find(NxCheckbox);
      checkboxes.at(0).simulate('change');
      expect(toggleEventTypeSpy).toHaveBeenCalledWith('Policy Management');
      checkboxes.at(1).simulate('change');
      expect(toggleEventTypeSpy).toHaveBeenCalledWith('Application Evaluation');
      checkboxes.at(2).simulate('change');
      expect(toggleEventTypeSpy).toHaveBeenCalledWith('Violation Alert');
      expect(toggleEventTypeSpy.calls.count()).toBe(3);
    });
  });

  describe('on load', () => {
    let component, getMounted, loadWebhookPageSpy;

    beforeEach(() => {
      spyOn(routerContext, 'useRouterState').and.returnValue({
        get: jasmine.createSpy('get').and.returnValue({ data: { title: 'foo' } }),
        href: jasmine.createSpy('href'),
      });

      loadWebhookPageSpy = jasmine.createSpy('loadWebhookPage');
      getMounted = enzymeUtils.getMountedComponent(EditWebhook, {
        ...minProps,
        loadWebhookPage: loadWebhookPageSpy,
      });
    });

    afterEach(() => {
      component.unmount();
    });

    it('calls loadWebhookPage action with no arguments if creating new webhook', () => {
      component = getMounted();
      expect(loadWebhookPageSpy).toHaveBeenCalled();
    });

    it('calls loadWebhookPage action with webhookId if editing webhook', () => {
      component = getMounted({
        router: {
          currentParams: {
            webhookId: '404',
          },
        },
      });
      expect(loadWebhookPageSpy).toHaveBeenCalledWith('404');
    });
  });

  describe('url text input onChange handler', () => {
    it('calls setUrl action', () => {
      const setUrlSpy = jasmine.createSpy('setUrl');
      const component = getShallow({
        setUrl: setUrlSpy,
      });
      const urlTextInput = component.find('#editor-webhook-url');
      urlTextInput.simulate('change', 'test webhook url');
      expect(setUrlSpy).toHaveBeenCalledWith('test webhook url');
    });
  });

  describe('description text input onChange handler', () => {
    it('calls setDescription action', () => {
      const setDescriptionSpy = jasmine.createSpy('setDescription');
      const component = getShallow({
        setDescription: setDescriptionSpy,
      });
      const descriptionTextInput = component.find('#editor-webhook-description');
      descriptionTextInput.simulate('change', 'test webhook description');
      expect(setDescriptionSpy).toHaveBeenCalledWith('test webhook description');
    });
  });

  describe('secretKey text input onChange handler', () => {
    it('calls setSecretKey action', () => {
      const setSecretKeySpy = jasmine.createSpy('setSecretKey');
      const component = getShallow({
        setSecretKey: setSecretKeySpy,
      });
      const descriptionTextInput = component.find('#editor-webhook-secret-key');
      descriptionTextInput.simulate('change', 'test secret key');
      expect(setSecretKeySpy).toHaveBeenCalledWith('test secret key');
    });
  });

  describe('NxStatefulForm', () => {
    it('is rendered with validationErrors if webhook url is empty', () => {
      const component = getShallow();
      expect(component.find(NxStatefulForm)).toHaveProp('validationErrors', 'Webhook URL is a required field');
    });

    it('is rendered with validationErrors if webhook url is invalid', () => {
      const component = getShallow({
        inputFields: {
          url: userInput(() => 'webhook is invalid', 'foo'),
          description: initialState(''),
          secretKey: initialState(''),
        },
      });
      expect(component.find(NxStatefulForm)).toHaveProp('validationErrors', 'webhook is invalid');
    });

    it('is rendered with validationErrors if in edit mode and no changes applied', () => {
      const component = getShallow({
        router: {
          currentParams: { webhookId: '200' },
        },
      });
      const form = component.find(NxStatefulForm);

      expect(form).toHaveProp('validationErrors', 'There are no changes to update.');
    });

    describe('onCancel', () => {
      it('navigates to webhook list page', () => {
        const stateGoSpy = jasmine.createSpy('stateGo');
        const component = getShallow({
          stateGo: stateGoSpy,
        });
        const form = component.find(NxStatefulForm);
        form.simulate('cancel');
        expect(stateGoSpy).toHaveBeenCalledWith('listWebhooks');
      });
    });

    describe('delete button', () => {
      it('is rendered when editing a webhook', () => {
        const component = getShallow({
          router: {
            currentParams: {
              webhookId: '404',
            },
          },
        });
        expect(component.find('#delete-webhook-button')).toExist();
      });
      it('is not rendered when creating a new webhook', () => {
        const component = getShallow();
        expect(component.find('#delete-webhook')).not.toExist();
      });
      it('shows delete modal when clicked', () => {
        const component = getShallow({
          router: {
            currentParams: {
              webhookId: '404',
            },
          },
        });
        const deleteButton = component.find('#delete-webhook-button');
        deleteButton.simulate('click');
        expect(component.find('#delete-modal')).toExist();
      });
    });

    describe('delete modal', () => {
      let modal, urlValue, webhookId, deleteWebhook;
      beforeEach(() => {
        urlValue = 'http://test';
        webhookId = '404';
        deleteWebhook = jasmine.createSpy('deleteWebhook');
        const component = getShallow({
          inputFields: {
            url: initialState(urlValue),
            description: initialState('test'),
            secretKey: initialState('test'),
          },
          router: {
            currentParams: {
              webhookId,
            },
          },
          deleteWebhook,
        });
        const deleteButton = component.find('#delete-webhook-button');
        deleteButton.simulate('click');
        modal = component.find('#delete-modal');
      });
      it('renders delete alert message', () => {
        const alert = modal.find(NxWarningAlert);
        expect(alert).toExist();
        expect(alert).toHaveText(
          `You are about to permanently remove webhook for ${urlValue}. This action cannot be undone.`
        );
      });
      it('calls deleteWebhook when submitted', () => {
        const form = modal.find(NxStatefulForm);
        form.simulate('submit');
        expect(deleteWebhook).toHaveBeenCalledWith(webhookId);
      });
    });
  });
});
