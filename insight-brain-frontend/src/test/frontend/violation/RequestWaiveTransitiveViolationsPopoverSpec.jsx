/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import * as enzymeUtils from '../enzymeUtils';
import RequestWaiveTransitiveViolationsPopover from '../../../main/frontend/violation/RequestWaiveTransitiveViolationsPopover';
import { getWaiveTransitiveViolationsUrl } from '../../../main/frontend/util/CLMLocation';
import TransitiveViolationsSummary from '../../../main/frontend/violation/TransitiveViolationsSummary';
import { IqPopoverHeader } from '../../../main/frontend/react/IqPopover';

describe('RequestWaiveTransitiveViolationsPopover', function () {
  let minimalProps, spyToggleRequestWaiveTransitiveViolations, getShallowComponent;

  beforeEach(function () {
    spyToggleRequestWaiveTransitiveViolations = jasmine.createSpy('spyToggleRequestWaiveTransitiveViolations');
    minimalProps = {
      scanId: 'someScanId',
      hash: 'someHash',
      availableScopes: {
        data: [{ publicId: 'appPublicId' }],
      },
      componentTransitivePolicyViolations: {
        threatCounts: {
          critical: 5,
          severe: 4,
          moderate: 3,
          low: 2,
          none: 1,
        },
        threatCountsTotal: 15,
        componentCount: 1,
      },
      toggleRequestWaiveTransitiveViolations: spyToggleRequestWaiveTransitiveViolations,
    };
    getShallowComponent = enzymeUtils.getShallowComponent(RequestWaiveTransitiveViolationsPopover, minimalProps);
  });

  it('calls toggleRequestWaiveTransitiveViolations when the toggle is clicked', function () {
    const wrapper = getShallowComponent();
    const header = wrapper.find(IqPopoverHeader);
    const toggle = header.dive().find('#request-waive-transitive-violations-popover-toggle');
    toggle.simulate('click');
    expect(spyToggleRequestWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('calls toggleRequestWaiveTransitiveViolations when the popover is closed', function () {
    const wrapper = getShallowComponent();
    const toggle = wrapper.find('#request-waive-transitive-violations-popover');
    toggle.simulate('close');
    expect(spyToggleRequestWaiveTransitiveViolations).toHaveBeenCalled();
  });

  it('creates a transitive violations summary with the correct props', function () {
    const wrapper = getShallowComponent();
    const transitiveViolationsSummary = wrapper.find(TransitiveViolationsSummary);
    expect(transitiveViolationsSummary).toHaveProp(
      'threatCounts',
      minimalProps.componentTransitivePolicyViolations.threatCounts
    );
    expect(transitiveViolationsSummary).toHaveProp(
      'threatCountsTotal',
      minimalProps.componentTransitivePolicyViolations.threatCountsTotal
    );
    expect(transitiveViolationsSummary).toHaveProp(
      'componentCount',
      minimalProps.componentTransitivePolicyViolations.componentCount
    );
  });

  it('shows the application public id', function () {
    const wrapper = getShallowComponent();
    const applicationPublicIdContainer = wrapper.find('#request-waive-transitive-violations-application-public-id');
    expect(applicationPublicIdContainer).toHaveProp('content', 'appPublicId');
  });

  it('shows the report id', function () {
    const wrapper = getShallowComponent();
    const reportIdContainer = wrapper.find('#request-waive-transitive-violations-report-id');
    expect(reportIdContainer).toHaveProp('content', 'someScanId');
  });

  it('shows the component hash', function () {
    const wrapper = getShallowComponent();
    const hashContainer = wrapper.find('#request-waive-transitive-violations-component-hash');
    expect(hashContainer).toHaveProp('content', 'someHash');
  });

  it('shows the curl example', function () {
    const wrapper = getShallowComponent();
    const curlExampleContainer = wrapper.find('#request-waive-transitive-violations-curl-example');
    expect(curlExampleContainer).toHaveProp(
      'content',
      'curl -u admin:admin123 -X POST ' + getWaiveTransitiveViolationsUrl('appPublicId', 'someScanId', 'someHash')
    );
  });
});
