/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../enzymeUtils';
import AttributionReportForm from 'MainRoot/legal/application/AttributionReportForm';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { NxTextInput } from '@sonatype/react-shared-components';
import { getAttributionReportMultiApplicationUrl } from 'MainRoot/util/CLMLocation';
import * as routerContext from 'MainRoot/react/RouterStateContext';

describe('AttributionReportForm component', function () {
  let getShallowComponent, getMountedComponent, minimalProps, routerContextMock;

  let spyGetAttributionReportTemplates = jasmine.createSpy('getAttributionReportTemplates');
  let spyApplyAttributionReportTemplateByIndex = jasmine.createSpy('applyAttributionReportTemplateByIndex');
  let spySetDirtyFlagToAttributionReport = jasmine.createSpy('setDirtyFlagToAttributionReport');

  beforeEach(function () {
    minimalProps = {
      applicationPublicId: 'legal-detection-service',
      stageTypeId: 'release',
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
            includeSonatypeSpecialLicenses: false,
            includeStandardLicenseTexts: true,
            includeInnerSource: true,
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
            includeSonatypeSpecialLicenses: false,
            includeStandardLicenseTexts: true,
            includeInnerSource: false,
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
      setDirtyFlagToAttributionReport: spySetDirtyFlagToAttributionReport,
    };

    routerContextMock = {
      href: jasmine.createSpy('href').and.callFake((stateName, stateParams) => {
        if (stateParams) {
          return `${stateName}-${JSON.stringify(stateParams)}`;
        }
        return stateName;
      }),
      get: jasmine.createSpy('get').and.returnValue('mockGetValue'),
    };
    spyOn(routerContext, 'useRouterState').and.returnValue(routerContextMock);

    getShallowComponent = enzymeUtils.getShallowComponent(AttributionReportForm, minimalProps);
    getMountedComponent = enzymeUtils.getMountedComponent(AttributionReportForm, minimalProps);
  });

  const testMenuBarBackButton = (props, expectedHref) => {
    const component = getShallowComponent(props);
    const menuBarBackButton = component.find(MenuBarBackButton);
    expect(menuBarBackButton).toExist();
    expect(menuBarBackButton).toHaveProp('href', expectedHref);
  };

  it('renders a MenuBarBackButton with correct href prop', function () {
    const testHrefProp = (props, expectedHref) => {
      const component = getShallowComponent(props);
      const menuBarBackButton = component.find(MenuBarBackButton);
      expect(menuBarBackButton).toExist();
      expect(menuBarBackButton).toHaveProp('href', expectedHref);
    };

    testHrefProp(
      {
        ...minimalProps,
        applicationPublicId: 'appId',
        stageTypeId: 'stage',
      },
      'legal.applicationDetails-{"applicationPublicId":"appId","stageTypeId":"stage"}'
    );

    testHrefProp(
      {
        ...minimalProps,
        applicationPublicId: 'appId',
        stageTypeId: 'stage',
        isSbomManager: true,
      },
      'sbomManager.legal.applicationDetails-{"applicationPublicId":"appId","stageTypeId":"stage"}'
    );
  });

  it('renders correctly form when isMultiApp property is true', function () {
    const component = getShallowComponent({
      ...minimalProps,
      applicationPublicId: 'appId',
      stageTypeId: 'stage',
      isMultiApp: true,
    });
    const manageTemplateButton = component.find('#manage-templates-button');
    expect(manageTemplateButton).toExist();
    manageTemplateButton.simulate('click');
    expect(manageTemplateButton).toHaveProp('href', 'legal.attributionReportTemplateMultiApp');

    const titleInputText = component.find('[name="title"]');
    expect(titleInputText).toExist();
    expect(titleInputText).toHaveProp('value', 'Attribution Report');
    const form = component.find('#attribution-report-settings-form');
    expect(form).toExist();
    expect(form).toHaveProp('action', getAttributionReportMultiApplicationUrl());
  });

  it('renders correct MenuBarBackButton when no applicationPublicId and stageTypeId are specified', function () {
    testMenuBarBackButton(
      {
        ...minimalProps,
        applicationPublicId: undefined,
        stageTypeId: undefined,
      },
      'legal.dashboard'
    );
  });

  it('renders correct MenuBarBackButton when no applicationPublicId and stageTypeId are specified when isSbomManager is true', function () {
    testMenuBarBackButton(
      {
        ...minimalProps,
        applicationPublicId: undefined,
        stageTypeId: undefined,
        isSbomManager: true,
      },
      'sbomManager.legal.dashboard'
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

  it('renders a confirmation modal when user tries to go back with non saved form changes', function () {
    const wrapper = getShallowComponent();
    var reportTitle = wrapper.find(NxTextInput).at(0);
    reportTitle.simulate('change', 'report name');
    expect(spySetDirtyFlagToAttributionReport).toHaveBeenCalledWith(true);
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
    expect(
      wrapper.find('input[name="includeSonatypeSpecialLicenses"]').prop('value') ===
        selectedAttributionReportTemplate.includeSonatypeSpecialLicenses
    ).toBe(true);
    expect(
      wrapper.find('input[name="includeInnerSource"]').prop('value') ===
        selectedAttributionReportTemplate.includeInnerSource
    ).toBe(true);
  });
});
