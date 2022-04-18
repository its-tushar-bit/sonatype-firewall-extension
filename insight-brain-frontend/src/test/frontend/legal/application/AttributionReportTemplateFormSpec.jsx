/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AttributionReportTemplateForm from 'MainRoot/legal/application/AttributionReportTemplateForm';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { NxTextInput } from '@sonatype/react-shared-components';

describe('AttributionReportTemplateForm component', function () {
  let getShallowComponent,
    getMountedComponent,
    minimalProps,
    spy$State,
    spyGetAttributionReportTemplates,
    spySetDirtyFlagToAttributionReportTemplate;

  beforeEach(function () {
    spy$State = jasmine.createSpyObj('$state', ['get', 'href']);
    spy$State.get.and.callFake((stateName) => stateName);
    spy$State.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    spyGetAttributionReportTemplates = jasmine.createSpy('getAttributionReportTemplates');
    spySetDirtyFlagToAttributionReportTemplate = jasmine.createSpy('setDirtyFlagToAttributionReportTemplate');

    minimalProps = {
      applicationPublicId: 'legal-detection-service',
      stageTypeId: 'release',
      attributionReportTemplates: {
        results: [
          {
            id: '9373a43a8fe84422a6d8a235512c363f',
            templateName: 'Template 1',
            documentTitle: 'Application Name 1',
            header: 'custom header',
            footer: 'custom footer',
            includeTableOfContents: false,
            includeAppendix: true,
            includeStandardLicenseTexts: true,
            lastUpdatedAt: 1630351357414,
          },
          {
            id: '32d5ed0f663741e68ac7979a889a0324',
            templateName: 'Template 2',
            documentTitle: 'Application Name 2',
            header: 'custom header',
            footer: 'custom footer',
            includeTableOfContents: false,
            includeAppendix: false,
            includeStandardLicenseTexts: false,
            lastUpdatedAt: 1630351372428,
          },
        ],
        error: null,
        loading: false,
        selectedTemplateIndex: 0,
        submitMaskState: null,
      },
      $state: spy$State,
      getAttributionReportTemplates: spyGetAttributionReportTemplates,
      setDirtyFlagToAttributionReportTemplate: spySetDirtyFlagToAttributionReportTemplate,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AttributionReportTemplateForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, minimalProps);
  });

  it('renders a MenuBarBackButton with correct href prop', function () {
    const component = getShallowComponent({
      ...minimalProps,
      applicationPublicId: 'appId',
      stageTypeId: 'stage',
    });
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp(
      'href',
      'legal.attributionReport-{"applicationPublicId":"appId","stageTypeId":"stage"}'
    );
  });

  it('renders correct MenuBarBackButton when no applicationPublicId and stageTypeId are specified', function () {
    const component = getShallowComponent({
      ...minimalProps,
      applicationPublicId: undefined,
      stageTypeId: undefined,
    });
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', 'legal.attributionReportMultiApp');
  });

  it('renders an empty form with default values', function () {
    const customMinimalProps = {
      ...minimalProps,
      applicationPublicId: 'legal-detection-service',
      stageTypeId: 'release',
      attributionReportTemplates: {
        results: [],
        error: null,
        loading: false,
        selectedTemplateIndex: -1,
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    expect(wrapper.find('.nx-h2').text()).toBe('Create Template');
    expect(wrapper.find('input[name="templateName"]').prop('value') === 'New Template').toBe(true);
    expect(wrapper.find('.tm-checked input[type="checkbox"]').length).toBe(3);
  });

  it('renders a list of templates with the first one selected', function () {
    const wrapper = getMountedComponent();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    const listItems = wrapper.find('.nx-list .nx-list__link');
    expect(listItems.length).toBe(2);
    expect(listItems.at(0).text()).toBe('Template 1');
    expect(listItems.at(0).hasClass('selected')).toBe(true);
    expect(listItems.at(1).text()).toBe('Template 2');
    expect(wrapper.find('input[name="templateName"]').prop('value')).toBe('Template 1');
    expect(wrapper.find('input[name="title"]').prop('value')).toBe('Application Name 1');
    expect(wrapper.find('input[name="header"]').prop('value')).toBe('custom header');
    expect(wrapper.find('input[name="footer"]').prop('value')).toBe('custom footer');
    expect(wrapper.find('.tm-checked input[type="checkbox"]').length).toBe(2);
  });

  it('renders a list of templates with the second one selected and the form showing the second template configuration', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        selectedTemplateIndex: 1,
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    const listItems = wrapper.find('.nx-list .nx-list__link');
    expect(listItems.length).toBe(2);
    expect(listItems.at(0).text()).toBe('Template 1');
    expect(listItems.at(1).hasClass('selected')).toBe(true);
    expect(listItems.at(1).text()).toBe('Template 2');
    expect(wrapper.find('input[name="templateName"]').prop('value')).toBe('Template 2');
    expect(wrapper.find('input[name="title"]').prop('value')).toBe('Application Name 2');
    expect(wrapper.find('input[name="header"]').prop('value')).toBe('custom header');
    expect(wrapper.find('input[name="footer"]').prop('value')).toBe('custom footer');
    expect(wrapper.find('.tm-checked input[type="checkbox"]').length).toBe(0);
  });

  it('renders a loading mask', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        loading: true,
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    expect(wrapper.find('.nx-loading-spinner')).toBeDefined();
  });

  it('renders an error message when the server responds with an error on templates load', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        results: [],
        selectedTemplateIndex: -1,
        error: { type: 'loadError', message: 'server error' },
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    const nxAlert = wrapper.find('.nx-alert--load-error .nx-load-error__message');
    expect(nxAlert).toBeDefined();
    expect(nxAlert.text()).toBe('An error occurred loading templates: server error');
  });

  it('renders an error message when the server responds with an error on template save', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        error: { type: 'saveError', message: 'server error' },
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    const nxAlert = wrapper.find('.nx-alert--load-error .nx-load-error__message');
    expect(nxAlert).toBeDefined();
    expect(nxAlert.text()).toBe('An error occurred saving the template: server error');
  });

  it('renders an error message when the server responds with an error on template delete', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        error: { type: 'deleteError', message: 'server error' },
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    const nxAlert = wrapper.find('.nx-alert--load-error .nx-load-error__message');
    expect(nxAlert).toBeDefined();
    expect(nxAlert.text()).toBe('An error occurred deleting the template: server error');
  });

  it('renders a submit mask when the user saves or deletes a template', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        ...minimalProps.attributionReportTemplates,
        submitMaskState: false,
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportTemplateForm, customMinimalProps)();
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
    expect(wrapper.find('.nx-submit-mask__message')).toBeDefined();
  });

  it('renders a confirmation modal when user tries to go back with non saved form changes', function () {
    const wrapper = getShallowComponent();
    var reportTitle = wrapper.find(NxTextInput).at(0);
    reportTitle.simulate('change', 'report name');
    expect(spySetDirtyFlagToAttributionReportTemplate).toHaveBeenCalledWith(true);
  });
});
