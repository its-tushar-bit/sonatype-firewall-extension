/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as enzymeUtils from '../../../enzymeUtils';
import ListWebhooks from '../../../../../main/frontend/configuration/webhook/listWebhooks/ListWebhooks';
import WebhookListItem from '../../../../../main/frontend/configuration/webhook/listWebhooks/WebhookListItem';

describe('ListWebhooks', () => {
  let getShallow, minProps, webhooks;

  beforeEach(() => {
    webhooks = [
      {
        id: '1',
        url: 'http://test1',
        secretKey: '#~FAKE~SECRET~KEY~#',
        description: 'webhook 1',
        eventTypes: [],
      },
      {
        id: '2',
        url: 'http://test2',
        secretKey: '#~FAKE~SECRET~KEY~#',
        description: '',
        eventTypes: [],
      },
      {
        id: '3',
        url: 'http://test3',
        secretKey: '#~FAKE~SECRET~KEY~#',
        description: 'webhook 3',
        eventTypes: [],
      },
    ];

    minProps = {
      isAppWebhooksSupported: true,
      webhooks,
    };

    getShallow = enzymeUtils.getShallowComponent(ListWebhooks, minProps);
  });

  describe('when no webhooks exist', () => {
    it('renders list with the empty list item', () => {
      const listItems = getShallow({ webhooks: [] }).find('.nx-list__item');
      expect(listItems.length).toBe(1);
      expect(listItems).toHaveClassName('nx-list__item--empty');
      expect(listItems.find(WebhookListItem)).not.toExist();
    });
  });

  describe('webhook list', () => {
    it('renders list item for each webhook', () => {
      const listItems = getShallow().find(WebhookListItem);
      expect(listItems.length).toBe(3);
      expect(listItems.find('.nx-list__item--empty')).not.toExist();

      for (let i = 0; i < 3; i++) {
        expect(listItems.at(i)).toHaveProp('webhook', webhooks[i]);
        expect(listItems.at(i)).toHaveProp('isAppWebhooksSupported', true);
      }
    });
  });

  describe('Add Webhook button', () => {
    it('redirects to addWebhook state onClick', () => {
      const stateGoSpy = jasmine.createSpy('stateGo');
      const addWebhookButton = getShallow({ stateGo: stateGoSpy }).find('#create-webhook');
      addWebhookButton.simulate('click');
      expect(stateGoSpy).toHaveBeenCalledWith('addWebhook');
    });
  });

  describe('on load', () => {
    let component, getMounted, loadWebhookListPageSpy;

    beforeEach(() => {
      loadWebhookListPageSpy = jasmine.createSpy('loadWebhookListPage');
      getMounted = enzymeUtils.getMountedComponent(ListWebhooks, {
        ...minProps,
        loadWebhookListPage: loadWebhookListPageSpy,
      });
    });

    afterEach(() => {
      component.unmount();
    });

    it('calls loadWebhookListPage action', () => {
      component = getMounted();
      expect(loadWebhookListPageSpy).toHaveBeenCalled();
    });
  });
});
