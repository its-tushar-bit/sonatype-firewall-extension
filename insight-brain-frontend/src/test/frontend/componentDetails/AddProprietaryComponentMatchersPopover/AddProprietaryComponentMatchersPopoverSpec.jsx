/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxAlert,
  NxCheckbox,
  NxStatefulForm,
  NxInfoAlert,
  NxSubmitMask,
  NxTextInput,
  NxTextLink,
} from '@sonatype/react-shared-components';
import AddProprietaryComponentMatchersPopover from 'MainRoot/componentDetails/AddProprietaryComponentMatchersPopover/AddProprietaryComponentMatchersPopover';
import * as enzymeUtils from 'TestRoot/enzymeUtils';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('AddProprietaryComponentMatchersPopover', () => {
  let minimalProps,
    getShallowComponent,
    getMountedComponent,
    onCloseSpy,
    addProprietaryMatchersSpy,
    mountedComponent,
    resetSubmitErrorSpy,
    setComponentMatchersDataSpy,
    hrefSpy;

  beforeEach(() => {
    hrefSpy = jasmine.createSpy('href').and.returnValue('http://some-href');
    onCloseSpy = jasmine.createSpy('onClose');
    addProprietaryMatchersSpy = jasmine.createSpy('addProprietaryMatchers');
    resetSubmitErrorSpy = jasmine.createSpy('resetSubmitError');
    setComponentMatchersDataSpy = jasmine.createSpy('setComponentMatchersData');

    spyOn(routerContext, 'useRouterState').and.returnValue({
      href: hrefSpy,
    });

    minimalProps = {
      onClose: onCloseSpy,
      showPopover: true,
      pathnames: ['pathname 1', 'pathname 2'],
      appInfo: {
        applicationName: 'app name',
        applicationId: 'appId',
      },
      addProprietaryMatchers: addProprietaryMatchersSpy,
      submitError: null,
      submitMaskState: null,
      resetSubmitError: resetSubmitErrorSpy,
      data: { paths: ['pathname 1', 'pathname 2'], regex: '' },
      setComponentMatchersData: setComponentMatchersDataSpy,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AddProprietaryComponentMatchersPopover, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AddProprietaryComponentMatchersPopover, minimalProps);
  });

  afterEach(() => {
    mountedComponent?.unmount();
  });

  it('renders the link to config', () => {
    const popover = getShallowComponent().find('#component-details-add-proprietary-component-matchers-popover'),
      alert = popover.find(NxInfoAlert),
      link = alert.find(NxTextLink);

    expect(link).toHaveProp('newTab');
    expect(link).toHaveProp('href', 'http://some-href');
    expect(hrefSpy).toHaveBeenCalledWith('management.edit.application.proprietary-config-policy', {
      applicationPublicId: 'appId',
    });
  });

  it('renders the matchers list', () => {
    const component = getShallowComponent(),
      popover = component.find('#component-details-add-proprietary-component-matchers-popover');
    let matchers = component.find(NxCheckbox);
    expect(popover).toExist();
    expect(matchers.length).toBe(2);
    expect(matchers.at(0)).toHaveText('pathname 1');
    expect(matchers.at(1)).toHaveText('pathname 2');
    expect(matchers.at(0)).toHaveProp('isChecked', true);
    expect(matchers.at(1)).toHaveProp('isChecked', true);
  });

  it('makes sure that the checkboxes change state', () => {
    const component = getShallowComponent({ data: { paths: ['pathname 2'], regex: '' } });
    let matchers = component.find(NxCheckbox);
    expect(matchers.length).toBe(2);

    expect(matchers.at(0)).toHaveProp('isChecked', false);
    expect(matchers.at(1)).toHaveProp('isChecked', true);
    matchers.at(0).invoke('onChange')();
    matchers = component.find(NxCheckbox);
    expect(setComponentMatchersDataSpy).toHaveBeenCalledTimes(1);
  });

  it('does not render a popover when showPopover is false', () => {
    mountedComponent = getMountedComponent({ showPopover: false });
    const popover = mountedComponent.find('#component-details-add-proprietary-component-matchers-popover');
    expect(setComponentMatchersDataSpy).not.toHaveBeenCalled();
    expect(resetSubmitErrorSpy).not.toHaveBeenCalled();
    expect(popover).not.toExist();
  });

  it('renders a popover when showPopover is true with the right information and triggers the useEffect hook', () => {
    mountedComponent = getMountedComponent();
    const popover = mountedComponent.find('#component-details-add-proprietary-component-matchers-popover'),
      alert = popover.find(NxInfoAlert);
    expect(popover).toExist();
    expect(setComponentMatchersDataSpy).toHaveBeenCalledTimes(1);
    expect(resetSubmitErrorSpy).toHaveBeenCalledTimes(1);
    expect(alert).toHaveText(
      'The following matchers will be added to the app name Configuration (duplicates will be ignored). The new matchers will be in effect for the next application analysis.'
    );
  });

  it('makes sure that the regex updates state', () => {
    mountedComponent = getMountedComponent();
    let regexInput = mountedComponent.find(NxTextInput);
    regexInput.at(0).invoke('onChange')('some input');
    expect(setComponentMatchersDataSpy).toHaveBeenCalledTimes(2);
    expect(setComponentMatchersDataSpy).toHaveBeenCalledWith({
      paths: ['pathname 1', 'pathname 2'],
      regex: 'some input',
    });
  });

  it('makes sure that the form has showValidationErrors class when there are none matchers selected and no regex', () => {
    mountedComponent = getMountedComponent();
    const showValidationErrorsClass = 'nx-form--show-validation-errors';

    let formEl = mountedComponent.find(NxStatefulForm);
    expect(formEl.getDOMNode().classList.contains(showValidationErrorsClass)).toBe(false);

    mountedComponent.unmount();
    mountedComponent = getMountedComponent({ data: { paths: [], regex: 'some regex' } });

    formEl = mountedComponent.find(NxStatefulForm);
    expect(formEl.getDOMNode().classList.contains(showValidationErrorsClass)).toBe(false);

    mountedComponent.unmount();
    mountedComponent = getMountedComponent({ data: { paths: [], regex: '' } });

    formEl = mountedComponent.find(NxStatefulForm);
    formEl.simulate('submit');

    mountedComponent.update();
    expect(formEl.getDOMNode().classList.contains(showValidationErrorsClass)).toBe(true);
  });

  it('sends the right information on submit', () => {
    mountedComponent = getMountedComponent({ data: { paths: ['pathname 2'], regex: 'some regex' } });
    const regexInput = mountedComponent.find(NxTextInput);

    expect(regexInput).toHaveProp('value', 'some regex');
    const form = mountedComponent.find(NxStatefulForm);
    form.invoke('onSubmit')();
    mountedComponent.update();
    expect(addProprietaryMatchersSpy).toHaveBeenCalledTimes(1);
    expect(addProprietaryMatchersSpy).toHaveBeenCalledWith({
      paths: ['pathname 2'],
      regex: 'some regex',
    });
  });

  it('clears the data onclose', () => {
    mountedComponent = getMountedComponent();
    const closeBtn = mountedComponent.find('.nx-form__cancel-btn').at(0);
    expect(closeBtn).toHaveText('Cancel');
    closeBtn.invoke('onClick')();
    expect(onCloseSpy).toHaveBeenCalledTimes(1);
    expect(resetSubmitErrorSpy).toHaveBeenCalledTimes(1);
  });

  it('shows an alert on error ', () => {
    mountedComponent = getMountedComponent({ submitError: 'some crazy error' });
    const alert = mountedComponent.find(NxAlert).at(1);
    expect(alert).toHaveText('An error occurred saving data. some crazy errorRetry');
  });

  it('shows the submitting modal ', () => {
    mountedComponent = getMountedComponent({ submitMaskState: false });
    const modal = mountedComponent.find(NxSubmitMask);
    expect(modal).toHaveText('Submitting…');
  });

  it('shows the success modal ', () => {
    mountedComponent = getMountedComponent({ submitMaskState: true });
    const modal = mountedComponent.find(NxSubmitMask);
    expect(modal).toHaveText('Success!');
  });
});
