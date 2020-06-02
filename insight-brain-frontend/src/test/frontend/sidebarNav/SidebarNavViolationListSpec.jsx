/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { mount } from 'enzyme';
import * as enzymeUtils from '../enzymeUtils';

describe('SidebarNavViolationList', function() {
  let minimalProps,
      onClickSpy,
      SidebarNavViolationList,
      getShallowComponent;

  beforeEach(function() {
    SidebarNavViolationList =
        require('inject-loader!../../../main/frontend/sidebarNav/SidebarNavViolationList')().default;

    onClickSpy = jasmine.createSpy('onClick');

    minimalProps = {
      currentViolationId: 'aaa',
      onClick: onClickSpy,
      scrollToSelection: false,
      violations: [{
        policyViolationId: 'aaa',
        threatLevel: 1,
        policyName: 'fooName'
      }]
    };

    getShallowComponent = enzymeUtils.getShallowComponent(SidebarNavViolationList, minimalProps);
  });

  function validateNavListItems(ulChild, violation) {
    ulChild.prop('onClick')();
    expect(onClickSpy).toHaveBeenCalledWith(violation.policyViolationId);

    let listItem = ulChild.find('li');
    expect(listItem).toMatchSelector('.nx-list__item');
    expect(listItem.children().length).toEqual(3);

    let threatBar = listItem.childAt(0);
    expect(threatBar).toMatchSelector('NxThreatBar');
    expect(threatBar.prop('policyThreatLevel')).toEqual(violation.threatLevel);

    let threatLevelSpan = listItem.childAt(1);
    expect(threatLevelSpan).toMatchSelector('span.iq-threat-number');
    expect(threatLevelSpan).toMatchSelector('span.iq-threat-number--sidebar-nav');
    expect(threatLevelSpan.text()).toEqual(violation.threatLevel.toString());

    let policyNameSpan = listItem.childAt(2);
    expect(policyNameSpan).toMatchSelector('span');
    expect(policyNameSpan.text()).toEqual(violation.policyName);
  }

  it('properly renders a list of violations', function() {
    let violations = [{
      policyViolationId: 'aaa',
      threatLevel: 1,
      policyName: 'fooName'
    }, {
      policyViolationId: 'bbb',
      threatLevel: 2,
      policyName: 'barName'
    }];
    let wrappingList = getShallowComponent({ violations }).find('ul');
    expect(wrappingList.children().length).toEqual(2);

    validateNavListItems(wrappingList.childAt(0), violations[0]);
    validateNavListItems(wrappingList.childAt(1), violations[1]);
  });

  it('adds the selected CSS class only to a policy violation id that matches the passed-in prop', function() {
    let violations = [{
      policyViolationId: 'aaa',
      threatLevel: 1,
      policyName: 'fooName'
    }, {
      policyViolationId: 'bbb',
      threatLevel: 2,
      policyName: 'barName'
    }];
    let wrappingList = getShallowComponent({ violations }).find('ul');
    expect(wrappingList.childAt(0)).toHaveClassName('selected');
    expect(wrappingList.childAt(1)).not.toHaveClassName('selected');
  });

  describe('scrollBehavior', function() {
    let container;

    beforeEach(function() {
      container = document.createElement('div');
      document.body.appendChild(container);
    });

    afterEach(function() {
      if (container) {
        document.body.removeChild(container);
        container = null;
      }
    });

    it('scrolls to selection if `scrollToSelection` is true', function(done) {
      const violations = [{
        policyViolationId: 'aaa',
        threatLevel: 1,
        policyName: 'fooName'
      }, {
        policyViolationId: 'bbb',
        threatLevel: 2,
        policyName: 'barName'
      }];

      spyOn(window, 'setTimeout').and.callFake((...params) => {
        const [callback, time, flag] = params;
        // use the flag to differentiate all the calls to `setTimeout`
        if (flag === 'sidebar-nav') {
          expect(typeof callback).toEqual('function');
          expect(time).toEqual(220);
          const selectedItem = container.querySelector('.nx-list__item.selected');
          expect(selectedItem).not.toBeNull();
          const scrollSpy = spyOn(selectedItem, 'scrollIntoView');
          callback();
          expect(scrollSpy).toHaveBeenCalled();
          done();
        }
      });

      const props = {
        ...minimalProps,
        violations,
        scrollToSelection: true
      };
      /**
       * have to mount the component to a container
       * so that it still exists by the time the timeout mock executes
       */
      mount(<SidebarNavViolationList {...props} />, { attachTo: container });
    });

    it('does not executes timeout if `scrollToSelection` is false', function() {
      const violations = [{
        policyViolationId: 'aaa',
        threatLevel: 1,
        policyName: 'fooName'
      }, {
        policyViolationId: 'bbb',
        threatLevel: 2,
        policyName: 'barName'
      }];

      spyOn(window, 'setTimeout').and.callThrough();

      const props = {
        ...minimalProps,
        violations,
        scrollToSelection: false
      };
      mount(<SidebarNavViolationList {...props} />, { attachTo: container });
      expect(window.setTimeout).not.toHaveBeenCalled();
    });
  });
});
