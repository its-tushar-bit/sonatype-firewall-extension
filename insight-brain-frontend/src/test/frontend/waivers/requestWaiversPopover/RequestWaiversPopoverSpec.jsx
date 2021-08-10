/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { NxInfoAlert, NxCodeSnippet, NxFontAwesomeIcon, NxButton, NxTextLink } from '@sonatype/react-shared-components';
import { faArrowToRight } from '@fortawesome/pro-solid-svg-icons';

import * as enzymeUtils from '../../enzymeUtils';
import requestWaiversPopover from '../../../../main/frontend/waivers/requestWaiversPopover/RequestWaiversPopover';
import IqPopover from '../../../../main/frontend/react/IqPopover/IqPopover';

describe('requestWaiversPopover', function () {
  let minimalProps, getShallowComponent, violationDetailsMock;

  beforeEach(function () {
    violationDetailsMock = {
      constraintViolations: [
        {
          constraintId: 'id',
          constraintName: 'name',
          reasons: [
            {
              reason: 'reason',
              reference: {
                value: 'vulnerabilityId',
              },
            },
          ],
        },
      ],
      filename: 'componentName',
      policyViolationId: 'id',
      policyName: 'name',
    };

    minimalProps = {
      violationDetails: null,
      isShown: true,
      onClose: () => {},
    };

    getShallowComponent = enzymeUtils.getShallowComponent(requestWaiversPopover, minimalProps);
  });

  it('renders a component', () => {
    expect(getShallowComponent()).toExist();
  });

  it('renders an IqPopover', () => {
    const el = getShallowComponent();
    const popover = el.find(IqPopover);
    expect(popover).toExist();
    expect(popover).toHaveProp('size', 'automatic');
  });

  it('renders an IqPopover header with close button', () => {
    const el = getShallowComponent();
    const popover = el.find(IqPopover);
    const button = popover.find(NxButton);
    const icon = popover.find(NxFontAwesomeIcon);
    const h2 = popover.find('.request-waivers-header__title-text');
    expect(button).toExist();
    expect(icon).toHaveProp('icon', faArrowToRight);
    expect(h2).toHaveText('Request Waiver');
  });

  it('renders a NxInfoAlert', () => {
    expect(getShallowComponent().find(NxInfoAlert)).toExist();
  });

  it('renders Policy label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(1);
    expect(policyIdSection).toHaveText('Policy');
  });

  it('renders Constraint Name label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(2);
    expect(policyIdSection).toHaveText('Constraint Name');
  });

  it('renders Conditions label section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find('.nx-read-only__label').at(3);
    expect(policyIdSection).toHaveText('Conditions');
  });

  it('renders a NxCodeSnippet for policy violation id section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find(NxCodeSnippet).at(0);
    expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
  });

  it('renders a NxCodeSnippet for policy violation details page section', () => {
    const el = getShallowComponent();
    const policyIdSection = el.find(NxCodeSnippet).at(1);
    expect(policyIdSection).toHaveProp('label', 'Policy Violation Details Page');
  });

  it('renders a NxCodeSnippet for curl example section', () => {
    const el = getShallowComponent();
    const curlSection = el.find(NxCodeSnippet).at(2);
    expect(curlSection).toHaveProp('label', 'Curl Example');
  });

  describe('when violationDetails is not null', () => {
    let element;
    beforeEach(() => {
      element = getShallowComponent({ violationDetails: violationDetailsMock });
    });

    it('renders Policy data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(1);
      expect(policyIdSection).toHaveText(violationDetailsMock.policyName);
    });

    it('renders Constraint Name data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(2);
      expect(policyIdSection).toHaveText(violationDetailsMock.constraintViolations[0].constraintName);
    });

    it('renders Conditions data section', () => {
      const policyIdSection = element.find('.nx-read-only__data').at(3);
      expect(policyIdSection).toHaveText(violationDetailsMock.constraintViolations[0].reasons[0].reason);
    });

    it('renders a NxCodeSnippet for policy violation id section', () => {
      const policyIdSection = element.find(NxCodeSnippet).at(0);
      expect(policyIdSection).toHaveProp('label', 'Policy Violation ID');
      expect(policyIdSection).toHaveProp('content', violationDetailsMock.policyViolationId);
    });

    it('renders a NxCodeSnippet for policy violation details page section', () => {
      const policyIdSection = element.find(NxCodeSnippet).at(1);
      expect(policyIdSection).toHaveProp('label', 'Policy Violation Details Page');
      expect(policyIdSection).toHaveProp('content', `/assets/#/violation/${violationDetailsMock.policyViolationId}`);
    });

    it('renders a NxCodeSnippet for curl example section', () => {
      const curlSection = element.find(NxCodeSnippet).at(2);
      expect(curlSection).toHaveProp('label', 'Curl Example');
      expect(curlSection).toHaveProp(
        'content',
        'curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" /api/v2/policyWaiver/id/application --data-binary \'waiver comment (optional)\''
      );
    });

    it('renders a NxTextLink for policy violation details page url section', () => {
      const policyPageLinkSection = element.find(NxTextLink).at(1);
      expect(policyPageLinkSection).toHaveProp('href', `/assets/#/violation/${violationDetailsMock.policyViolationId}`);
    });

    it('renders an input for policy violation details page url section', () => {
      const policyPageUrlSection = element.find('.iq-request-waivers-page__link-input').at(0);
      expect(policyPageUrlSection).toHaveProp('value', `/assets/#/violation/${violationDetailsMock.policyViolationId}`);
      expect(policyPageUrlSection).toHaveProp('readOnly', true);
    });
  });
});
