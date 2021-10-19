/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AttributionReportForm from 'MainRoot/legal/application/AttributionReportForm';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';

describe('AttributionReportForm component', function () {
  let getShallowComponent, getMountedComponent, minimalProps, spy$State;

  let spyGetAttributionReportTemplates = jasmine.createSpy('getAttributionReportTemplates');
  let spyApplyAttributionReportTemplateByIndex = jasmine.createSpy('applyAttributionReportTemplateByIndex');

  beforeEach(function () {
    spy$State = jasmine.createSpyObj('$state', ['get', 'href']);
    spy$State.get.and.callFake((stateName) => stateName);
    spy$State.href.and.callFake((stateName, stateParams) => {
      if (stateParams) {
        return `${stateName}-${JSON.stringify(stateParams)}`;
      }
      return stateName;
    });

    minimalProps = {
      applicationPublicId: 'legal-detection-service',
      stageTypeId: 'release',
      $state: spy$State,
      attributionReportTemplates: {
        results: [
          {
            id: '8025b1a97727492db7636cd40084611c',
            templateName: 'Template 1',
            documentTitle: 'Report Title 1',
            header: 'Header 1',
            footer: 'Footer 1',
            includeTableOfContents: true,
            includeAppendix: true,
            includeStandardLicenseTexts: true,
            lastUpdatedAt: 1631210819988,
          },
          {
            id: 'a66ff304268447ccb558ddcc4d6e62f8',
            templateName: 'Template 2',
            documentTitle: 'Report Title 2',
            header: 'Header 2',
            footer: 'Footer 2',
            includeTableOfContents: false,
            includeAppendix: true,
            includeStandardLicenseTexts: true,
            lastUpdatedAt: 1631210809228,
          },
        ],
        error: null,
        loading: false,
        selectedTemplateIndex: 0,
        submitMaskState: null,
      },
      attributionReports: { selectedTemplateIndex: -1 },
      getAttributionReportTemplates: spyGetAttributionReportTemplates,
      applyAttributionReportTemplateByIndex: spyApplyAttributionReportTemplateByIndex,
    };

    getShallowComponent = enzymeUtils.getShallowComponent(AttributionReportForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AttributionReportForm, minimalProps);
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
      'legal.applicationDetails-{"applicationPublicId":"appId","stageTypeId":"stage"}'
    );
  });

  it('renders report title input with a default text when no template is selected', function () {
    const wrapper = getMountedComponent();
    expect(
      wrapper.find('input[name="title"]').prop('value') === 'Attribution Report for ' + minimalProps.applicationPublicId
    ).toBe(true);
  });

  it('renders all checkboxes checked by default', function () {
    const wrapper = getMountedComponent();
    expect(wrapper.find('.tm-checked input[type="checkbox"]').length).toBe(3);
  });

  it('renders form with action param url to attribution report accordingly applcationPublicId and stageTypeId props`', function () {
    const wrapper = getMountedComponent();
    const reportUrl = `/api/v2/licenseLegalMetadata/application/${minimalProps.applicationPublicId}/stage/${minimalProps.stageTypeId}/report`;
    expect(wrapper.find(`form[action="${reportUrl}"]`)).toBeTruthy();
  });

  it('renders a dropdown with template lists and calls getAttributionReportTemplates action to retrive them', function () {
    const wrapper = getMountedComponent();
    const dropDown = wrapper.find('.nx-dropdown__toggle-label');
    expect(dropDown).toBeTruthy();
    dropDown.simulate('click');
    expect(wrapper.find('.nx-dropdown-button').length).toBe(2);
    expect(spyGetAttributionReportTemplates).toHaveBeenCalled();
  });

  it('renders a disabled dropdown on empty templates list', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReportTemplates: {
        results: [],
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportForm, customMinimalProps)();
    expect(wrapper.find('.nx-dropdown__toggle.disabled')).toExist();
  });

  it('renders a form with a template list and then the applied template is changed and calls applyAttributionReportTemplateByIndex action', function () {
    const wrapper = getMountedComponent();
    wrapper.find('.nx-dropdown__toggle-label').simulate('click');
    wrapper.update();
    wrapper.find('.nx-dropdown-button').first().simulate('click');
    expect(spyApplyAttributionReportTemplateByIndex).toHaveBeenCalledWith(0);
  });

  it('renders the form with selected template values', function () {
    const customMinimalProps = {
      ...minimalProps,
      attributionReports: {
        selectedTemplateIndex: 0,
      },
    };
    const wrapper = enzymeUtils.getMountedComponent(AttributionReportForm, customMinimalProps)();
    const dropDown = wrapper.find('.nx-dropdown__toggle-label');
    expect(dropDown).toBeTruthy();
    const selectedAttributionReportTemplate =
      minimalProps.attributionReportTemplates.results[customMinimalProps.attributionReports.selectedTemplateIndex];
    expect(wrapper.find('input[name="title"]').prop('value') === selectedAttributionReportTemplate.documentTitle).toBe(
      true
    );
    expect(wrapper.find('input[name="header"]').prop('value') === selectedAttributionReportTemplate.header).toBe(true);
    expect(wrapper.find('input[name="footer"]').prop('value') === selectedAttributionReportTemplate.footer).toBe(true);
    expect(
      wrapper.find('input[name="includeToc"]').prop('value') ===
        selectedAttributionReportTemplate.includeTableOfContents.toString()
    ).toBe(true);
    expect(
      wrapper.find('input[name="includeStandardLicenseTexts"]').prop('value') ===
        selectedAttributionReportTemplate.includeStandardLicenseTexts.toString()
    ).toBe(true);
    expect(
      wrapper.find('input[name="includeAppendix"]').prop('value') ===
        selectedAttributionReportTemplate.includeAppendix.toString()
    ).toBe(true);
  });
});
