/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';
import { faCube } from '@fortawesome/pro-solid-svg-icons';
import { faFile, faNetworkWired, faTerminal } from '@fortawesome/free-solid-svg-icons';
import TransitiveViolationsPageSubtitle from '../../../main/frontend/violation/TransitiveViolationsPageSubtitle';
import { ComponentDetailsReportInfo } from '../../../main/frontend/componentDetails/ComponentDetailsHeader/ComponentDetailsReportInfo';

describe('TransitiveViolationsPageSubtitle', function () {
  let minimalProps, getShallowComponent, getShallowComponentNoProps;

  beforeEach(function () {
    minimalProps = {
      availableScopes: [
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          label: 'Organization',
        },
      ],
      componentName: 'someComponentName',
      stageTypeId: 'someStageTypeId',
    };
    getShallowComponent = enzymeUtils.getShallowComponent(TransitiveViolationsPageSubtitle, minimalProps);
    getShallowComponentNoProps = enzymeUtils.getShallowComponent(ComponentDetailsReportInfo);
  });

  it('does not render if there is no subtitle part to render', () => {
    const component = getShallowComponentNoProps();
    expect(component).toBeEmptyRender();
  });

  it('shows the org, app, component, and stage', function () {
    const wrapper = getShallowComponent({
      availableScopes: [
        {
          id: 'some-application-id',
          name: 'Some Application',
          type: 'application',
        },
        {
          id: 'someOrg',
          name: 'Some Other Organization',
          type: 'organization',
        },
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          type: 'organization',
        },
      ],
    });
    const subtitleParts = wrapper.find('.nx-page-title__description>span');
    expect(subtitleParts.length).toBe(4);
    expect(subtitleParts.at(0).html()).toContain('Some Other Organization');
    expect(subtitleParts.at(0).find(NxFontAwesomeIcon)).toHaveProp('icon', faNetworkWired);
    expect(subtitleParts.at(1).html()).toContain('Some Application');
    expect(subtitleParts.at(1).find(NxFontAwesomeIcon)).toHaveProp('icon', faTerminal);
    expect(subtitleParts.at(2).html()).toContain('someComponentName');
    expect(subtitleParts.at(2).find(NxFontAwesomeIcon)).toHaveProp('icon', faCube);
    expect(subtitleParts.at(3).html()).toContain('Latest SomeStageTypeId Report');
    expect(subtitleParts.at(3).find(NxFontAwesomeIcon)).toHaveProp('icon', faFile);
  });

  it('shows the org, component, and stage', function () {
    const wrapper = getShallowComponent({
      availableScopes: [
        {
          id: 'someOrg',
          name: 'Some Other Organization',
          type: 'organization',
        },
        {
          id: 'ROOT_ORGANIZATION_ID',
          name: 'Root Organization',
          type: 'organization',
        },
      ],
    });
    const subtitleParts = wrapper.find('.nx-page-title__description>span');
    expect(subtitleParts.length).toBe(3);
    expect(subtitleParts.at(0).html()).toContain('Some Other Organization');
    expect(subtitleParts.at(0).find(NxFontAwesomeIcon)).toHaveProp('icon', faNetworkWired);
    expect(subtitleParts.at(1).html()).toContain('someComponentName');
    expect(subtitleParts.at(1).find(NxFontAwesomeIcon)).toHaveProp('icon', faCube);
    expect(subtitleParts.at(2).html()).toContain('Latest SomeStageTypeId Reports');
    expect(subtitleParts.at(2).find(NxFontAwesomeIcon)).toHaveProp('icon', faFile);
  });

  it('shows the root org, component, and stage', function () {
    const wrapper = getShallowComponent();
    const subtitleParts = wrapper.find('.nx-page-title__description>span');
    expect(subtitleParts.length).toBe(3);
    expect(subtitleParts.at(0).html()).toContain('Root Organization');
    expect(subtitleParts.at(0).find(NxFontAwesomeIcon)).toHaveProp('icon', faNetworkWired);
    expect(subtitleParts.at(1).html()).toContain('someComponentName');
    expect(subtitleParts.at(1).find(NxFontAwesomeIcon)).toHaveProp('icon', faCube);
    expect(subtitleParts.at(2).html()).toContain('Latest SomeStageTypeId Reports');
    expect(subtitleParts.at(2).find(NxFontAwesomeIcon)).toHaveProp('icon', faFile);
  });
});
