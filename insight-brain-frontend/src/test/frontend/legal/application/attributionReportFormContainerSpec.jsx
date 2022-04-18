/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { shallow } from 'enzyme';
import React from 'react';
import configureStore from 'redux-mock-store';

describe('AttributionReportFormContainerSpec', function () {
  let store,
    state,
    vdom,
    AttributionReportFormContainer,
    stateGoMock,
    getAttributionReportTemplatesMock,
    applyAttributionReportTemplateByIndexMock,
    setDirtyFlagToAttributionReportMock,
    spy$State;

  beforeEach(function () {
    spy$State = jasmine.createSpyObj('$state', ['get', 'href']);
    getAttributionReportTemplatesMock = jasmine
      .createSpy('getAttributionReportTemplates')
      .and.returnValue({ type: 'FOO' });
    applyAttributionReportTemplateByIndexMock = jasmine
      .createSpy('applyAttributionReportTemplateByIndex')
      .and.returnValue({ type: 'FOO' });
    setDirtyFlagToAttributionReportMock = jasmine
      .createSpy('setDirtyFlagToAttributionReport')
      .and.returnValue({ type: 'FOO' });

    state = {
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
      router: {
        currentParams: {
          applicationPublicId: 'applicationPublicId',
          stageTypeId: 'build',
          $state: spy$State,
        },
        currentState: {
          data: {
            isMultiApp: false,
          },
        },
      },
    };

    stateGoMock = jasmine.createSpy('stateGo').and.returnValue({ type: 'BAR' });

    AttributionReportFormContainer = require('inject-loader!../../../../main/' +
      'frontend/legal/application/AttributionReportFormContainer')({
      './attributionReportsActions': {
        getAttributionReportTemplates: getAttributionReportTemplatesMock,
        applyAttributionReportTemplateByIndex: applyAttributionReportTemplateByIndexMock,
        setDirtyFlagToAttributionReport: setDirtyFlagToAttributionReportMock,
      },
      '../../reduxUiRouter/routerActions': {
        stateGo: stateGoMock,
      },
    }).default;

    store = configureStore()(() => state);
    vdom = <AttributionReportFormContainer store={store} />;
  });

  it('maps the state slice to props', () => {
    let wrapper = shallow(vdom).dive();
    expect(wrapper).toHaveProp('applicationPublicId', 'applicationPublicId');
    expect(wrapper).toHaveProp('isMultiApp', false);
  });
});
