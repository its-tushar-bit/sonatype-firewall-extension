/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { faBell, faExclamationCircle } from '@fortawesome/pro-regular-svg-icons';
import { timeAgo } from 'MainRoot/util/CommonServices';
import {
  NxButtonBar,
  NxFontAwesomeIcon,
  NxH4,
  NxModal,
  NxStatefulNavigationDropdown,
  NxFooter,
  NxButton,
  NxH2,
  NxNavigationDropdown,
  NxErrorAlert,
  NxInfoAlert,
} from '@sonatype/react-shared-components';

const NotificationsMenu = (props) => {
  const { notificationsToDisplay, loading, error, loadNotifications, setNotificationViewed } = props;
  const [selectedNotification, setSelectedNotification] = useState(null);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const unreadNotificationCount = notificationsToDisplay
    ? notificationsToDisplay.filter(({ viewed }) => !viewed).length
    : 0;

  useEffect(() => {
    if (!notificationsToDisplay && !error) {
      loadNotifications();
    }
  });

  const handleViewNotificationDetails = (notification) => {
    const isSameNotificationAsSelectedClicked = !!selectedNotification && notification.id === selectedNotification.id;
    setSelectedNotification(isSameNotificationAsSelectedClicked ? null : notification);
    if (!notification.viewed) {
      setNotificationViewed(notification);
    }
  };

  const handleNotifClick = (notification) => {

    if (notification.summaryUrl && notification.summaryUrl.startsWith('#/')) {
      if (!notification.viewed) {
        setNotificationViewed(notification);
      }
      window.location.hash = notification.summaryUrl.substring(1);
    } else {
      handleViewNotificationDetails(notification);
      setDetailModalOpen(true);
    }
  };

  const notificationMappingToComponent = (notification) => {
    const { age, qualifier } = timeAgo(notification.dateCreated);
    return (
      <button
        key={notification.id + notification.summaryText}
        className={classnames(['iq-notification nx-dropdown-button'], {
          viewed: notification.viewed,
        })}
        onClick={() => handleNotifClick(notification)}
      >
        <span className="iq-notification__text">{notification.summaryText}</span>
        <span className="iq-notification__age">
          {age} {qualifier}
        </span>
      </button>
    );
  };

  return (
    <div className="iq-notifications-menu-button">
      <NxStatefulNavigationDropdown icon={faBell} title="Notifications" className="iq-notifications-menu">
        <NxNavigationDropdown.MenuHeader>
          <NxH4>Notifications</NxH4>
        </NxNavigationDropdown.MenuHeader>

        {loading && <NxInfoAlert>Loading notification content from server...</NxInfoAlert>}

        {error && <NxErrorAlert>{error}</NxErrorAlert>}

        <div>
          {!loading && !error && notificationsToDisplay && notificationsToDisplay.map(notificationMappingToComponent)}
        </div>
      </NxStatefulNavigationDropdown>
      {selectedNotification && detailModalOpen && (
        <NotificationModal
          notification={selectedNotification}
          setSelectedNotification={setSelectedNotification}
          setDetailModalOpen={setDetailModalOpen}
        />
      )}
      {error && <NxFontAwesomeIcon className="iq-notif-error" icon={faExclamationCircle} />}
      {!error && unreadNotificationCount > 0 && <div data-testid="iq-unread-notif-dot" className="iq-unread-dot"></div>}
    </div>
  );
};

function NotificationModal({ notification, setSelectedNotification, setDetailModalOpen }) {
  const handleCancel = () => {
    setSelectedNotification(null);
    setDetailModalOpen(false);
  };

  return (
    <>
      <NxModal className="iq-notification-detail-modal" onCancel={handleCancel}>
        <NxModal.Header>
          <NxH2 className="iq-notification-detail-modal-header">{notification.summaryText}</NxH2>
        </NxModal.Header>
        <NxModal.Content className="iq-notification-detail-modal-content">
          <div dangerouslySetInnerHTML={{ __html: notification.detailHtml }}></div>
        </NxModal.Content>
        <NxFooter>
          <NxButtonBar>
            <NxButton onClick={() => setSelectedNotification(null)}>Close</NxButton>
          </NxButtonBar>
        </NxFooter>
      </NxModal>
    </>
  );
}

NotificationsMenu.propTypes = {
  notificationsToDisplay: PropTypes.array,
  selectedNotification: PropTypes.any,
  loading: PropTypes.bool,
  error: PropTypes.string,
  loadNotifications: PropTypes.func,
  setNotificationViewed: PropTypes.func,
};

NotificationModal.propTypes = {
  notification: PropTypes.shape({
    id: PropTypes.string,
    summaryText: PropTypes.string,
    detailHtml: PropTypes.string,
    dateCreated: PropTypes.number,
    viewed: PropTypes.bool,
  }),
  setSelectedNotification: PropTypes.func,
  setDetailModalOpen: PropTypes.func,
};

export default NotificationsMenu;
