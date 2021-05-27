/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxCheckbox, NxForm, NxInfoAlert, nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import * as enzymeUtils from '../../../enzymeUtils';
import EditWebhook from '../../../../../main/frontend/configuration/webhook/editWebhook/EditWebhook';
import * as routerContext from '../../../../../main/frontend/react/RouterStateContext';

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
    };

    getShallow = enzymeUtils.getShallowComponent(EditWebhook, minProps);
  });

  describe('when application webhooks are not supported ', () => {
    let component;

    beforeEach(() => {
      component = getShallow({
        isAppWebhooksSupported: false,
      });
    });

    it('renders info alert in the form', () => {
      const form = component.find(NxForm);
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
      const form = component.find(NxForm);
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
    let component, getMounted, loadWebhookDataSpy;

    beforeEach(() => {
      spyOn(routerContext, 'useRouterState').and.returnValue({
        get: jasmine.createSpy('get').and.returnValue({ data: { title: 'foo' } }),
        href: jasmine.createSpy('href'),
      });

      loadWebhookDataSpy = jasmine.createSpy('loadWebhookData');
      getMounted = enzymeUtils.getMountedComponent(EditWebhook, {
        ...minProps,
        loadWebhookData: loadWebhookDataSpy,
      });
    });

    afterEach(() => {
      component.unmount();
    });

    it('calls loadWebhookData action with no arguments if creating new webhook', () => {
      component = getMounted();
      expect(loadWebhookDataSpy).toHaveBeenCalled();
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

  describe('NxForm', () => {
    it('is rendered with validationErrors if webhook url is empty', () => {
      const component = getShallow();
      expect(component.find(NxForm)).toHaveProp('validationErrors', 'Webhook URL is a required field');
    });

    it('is rendered with validationErrors if webhook url is invalid', () => {
      const component = getShallow({
        inputFields: {
          url: userInput(() => 'webhook is invalid', 'foo'),
          description: initialState(''),
          secretKey: initialState(''),
        },
      });
      expect(component.find(NxForm)).toHaveProp('validationErrors', 'webhook is invalid');
    });

    describe('onCancel', () => {
      it('navigates to webhook list page', () => {
        const stateGoSpy = jasmine.createSpy('stateGo');
        const component = getShallow({
          stateGo: stateGoSpy,
        });
        const form = component.find(NxForm);
        form.simulate('cancel');
        expect(stateGoSpy).toHaveBeenCalledWith('webhooks.list');
      });
    });
  });
});
