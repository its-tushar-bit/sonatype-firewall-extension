/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import { waiverMatcherStrategy, displayWaiverScope } from 'MainRoot/util/waiverUtils';
import SidebarNavWaiversList from 'MainRoot/sidebarNav/SidebarNavWaiversList';

import 'TestRoot/SpecUtil';

describe('SidebarNavWaiversList', function () {
  let minimalProps, onClickSpy, renderComponent;

  beforeEach(function () {
    onClickSpy = jest.fn().mockName('onClick');

    minimalProps = {
      currentWaiverId: '35513cecc0214e0cb0207238dc1fba6e',
      onClick: onClickSpy,
      scrollToSelection: false,
      waivers: [
        {
          id: '35513cecc0214e0cb0207238dc1fba6e',
          threatLevel: 7,
          policyId: '67a74447c2bf4c53b8e26f93b16ad4ee',
          policyName: 'Component-Similar',
          ownerId: 'ROOT_ORGANIZATION_ID',
          ownerName: 'Root Organization',
          ownerType: 'root_organization',
          associatedPackageUrl: 'a/package/url',
          scope: 'Root Organization',
          isAutoWaiver: false,
          forContainerImage: false,
          componentMatchStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
          displayName: {
            parts: [
              {
                field: 'Group',
                value: 'test-group',
              },
              {
                value: ':',
              },
              {
                field: 'Artifact',
                value: 'test-artifact',
              },
              {
                value: ':',
              },
              {
                field: 'Version',
                value: '1.2.3',
              },
            ],
          },
        },
        {
          id: 'bbb045cb733d4868bd6d30e4384e19f4',
          threatLevel: 9,
          policyId: '358f08a34c7b47739f6962b35b84fbea',
          policyName: 'Security-High',
          ownerId: '79e2b6864a4d4f5fbce461cf930c3f2c',
          ownerName: 'unprotected zip big java app',
          ownerType: 'application',
          scope: 'Application - unprotected zip big java app',
          isAutoWaiver: false,
          forContainerImage: false,
          associatedPackageUrl: 'a/package/url',
          componentMatchStrategy: waiverMatcherStrategy.ALL_VERSIONS,
          displayName: {
            parts: [
              {
                field: 'Group',
                value: 'test-group',
              },
              {
                value: ':',
              },
              {
                field: 'Artifact',
                value: 'test-artifact',
              },
              {
                value: ':',
              },
              {
                field: 'Version',
                value: '1.2.3',
              },
            ],
          },
        },
        {
          id: '97b6e453199f452482693e44d3390eab',
          threatLevel: 7,
          policyId: null,
          policyName: null,
          ownerId: 'f1c6c5defdfe47ea8b2e3b15d87d1842',
          ownerName: 'Auto Waiver App',
          ownerType: 'application',
          componentMatchStrategy: 'DEFAULT',
          hash: null,
          constraintFacts: null,
          comment: null,
          creatorId: null,
          creatorName: null,
          componentIdentifier: null,
          componentUpgradeAvailable: false,
          isAutoWaiver: true,
          displayName: null,
          forContainerImage: false,
          scope: 'Application - Auto Waiver App',
        },
        {
          id: 'b1b59985e22a4dd095e6812732af6407',
          threatLevel: 9,
          policyId: '7e2d159c02734df6be7529ff5b88f67f',
          policyName: 'docker-policy-sonatype-container',
          ownerId: '0caa731cc7b149e7bc24fe9602e3a7dd',
          ownerName: 'localhost_8070-docker-proxy-library-alpine-3.614',
          ownerType: 'application',
          componentMatchStrategy: 'ALL_COMPONENTS',
          hash: null,
          constraintFacts: null,
          comment: null,
          creatorId: null,
          creatorName: null,
          componentIdentifier: null,
          componentUpgradeAvailable: null,
          isAutoWaiver: false,
          isExpireWhenRemediationAvailable: false,
          policyWaiverReason: null,
          forContainerImage: true,
          displayName: null,
          scope: 'Application - localhost_8070-docker-proxy-library-alpine-3.614',
        },
      ],
    };

    renderComponent = (props = {}) => render(<SidebarNavWaiversList {...minimalProps} {...props} />);
  });

  function validateNavListItems(ulChild, waiver, expectedThreatClass) {
    const {
      policyName,
      threatLevel,
      ownerName,
      ownerType,
      ownerId,
      id,
      componentMatchStrategy,
      isAutoWaiver,
      forContainerImage,
    } = waiver;
    const isStandaloneFirewall = false;

    fireEvent.click(ulChild);
    expect(onClickSpy).toHaveBeenCalledWith(
      ownerId,
      ownerType,
      id,
      isAutoWaiver ? 'autoWaiver' : 'waiver',
      isStandaloneFirewall
    );

    expect(ulChild).toHaveClass('nx-list__item');
    expect(ulChild.children.length).toEqual(3);

    const threatIndicator = ulChild.children[0];
    expect(threatIndicator).toHaveClass(expectedThreatClass);

    const policyNameElement = ulChild.children[1];
    expect(policyNameElement.textContent).toEqual(
      isAutoWaiver
        ? `≤ ${threatLevel}Auto`
        : forContainerImage
        ? `${threatLevel} ${ownerName}`
        : `${threatLevel} ${policyName}`
    );

    if (!isAutoWaiver) {
      const subTextItems = ulChild.children[2];
      const componentDisplay = forContainerImage ? subTextItems : subTextItems.children[0];
      const componentNameText = forContainerImage ? componentDisplay : componentDisplay.children[0];

      switch (componentMatchStrategy) {
        case waiverMatcherStrategy.EXACT_COMPONENT:
          expect(componentNameText.textContent).toEqual('test-group:test-artifact:1.2.3');
          break;
        case waiverMatcherStrategy.ALL_VERSIONS:
          expect(componentNameText.textContent).toEqual('test-group:test-artifact (all versions)');
          break;
        case waiverMatcherStrategy.ALL_COMPONENTS:
          expect(componentNameText.textContent).toEqual('All components');
          break;
      }
    }

    if (!forContainerImage) {
      const subTextItems = ulChild.children[2];
      const fullOrgName = isAutoWaiver ? subTextItems.children[0] : subTextItems.children[1];
      expect(fullOrgName.textContent).toEqual(
        displayWaiverScope({ scopeOwnerType: ownerType, scopeOwnerName: ownerName })
      );
    }
  }

  it('properly renders a list of waivers', function () {
    const { waivers } = minimalProps;
    renderComponent();

    const wrappingList = screen.getByRole('list');
    expect(wrappingList.children.length).toEqual(4);

    validateNavListItems(wrappingList.children[0], waivers[0], 'nx-threat-indicator--severe');
    validateNavListItems(wrappingList.children[1], waivers[1], 'nx-threat-indicator--critical');
    validateNavListItems(wrappingList.children[2], waivers[2], 'nx-threat-indicator--severe');
    validateNavListItems(wrappingList.children[3], waivers[3], 'nx-threat-indicator--critical');

    expect(screen.getByText('test-group:test-artifact (all versions)')).toBeVisible();
    expect(screen.getByText('test-group:test-artifact:1.2.3')).toBeVisible();
  });

  it('adds the selected CSS class only to a waiver id that matches the passed-in prop', function () {
    renderComponent();

    const wrappingList = screen.getByRole('list');
    expect(wrappingList.children[0]).toHaveClass('selected');
    expect(wrappingList.children[1]).not.toHaveClass('selected');
  });

  describe('scrollBehavior', function () {
    beforeEach(function () {
      renderComponent = (props = {}) =>
        render(
          <div data-testid="container">
            <SidebarNavWaiversList {...minimalProps} {...props} />
          </div>
        );
    });

    function container() {
      return within(screen.getByTestId('container'));
    }

    it('scrolls to selection if `scrollToSelection` is true', function (done) {
      const { waivers } = minimalProps;

      jest.spyOn(window, 'setTimeout').mockImplementation((...params) => {
        const [callback, time, flag] = params;
        // use the flag to differentiate all the calls to `setTimeout`
        if (flag === 'sidebar-nav') {
          expect(typeof callback).toEqual('function');
          expect(time).toEqual(200);
          const selectedItem = container().getByTestId('selected');
          expect(selectedItem).not.toBeNull();
          selectedItem.scrollIntoView = jest.fn();
          const scrollSpy = selectedItem.scrollIntoView;
          callback();
          expect(scrollSpy).toHaveBeenCalled();
          done();
        }
      });

      const props = {
        ...minimalProps,
        waivers,
        scrollToSelection: true,
      };
      /**
       * have to mount the component to a container
       * so that it still exists by the time the timeout mock executes
       */
      renderComponent(props);
    });

    it('does not executes timeout if `scrollToSelection` is false', function () {
      const { waivers } = minimalProps;

      // setTimeout will be called for NxThreatIndicator's NxTooltip in updateBatcher.ts
      // grab all flags and check that the setTimeout invoked in the useEffect isn't called.
      const flags = [];
      jest.spyOn(window, 'setTimeout').mockImplementation((...params) => {
        const [, , flag] = params;
        flags.push(flag);
      });

      const props = {
        waivers,
        scrollToSelection: false,
      };
      renderComponent(props);

      expect(flags).not.toContain('sidebar-nav');
    });
  });
});
