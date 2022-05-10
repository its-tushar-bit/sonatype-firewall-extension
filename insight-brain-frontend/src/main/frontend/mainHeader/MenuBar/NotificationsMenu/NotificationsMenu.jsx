/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { faBell, faExclamationCircle } from '@fortawesome/pro-solid-svg-icons';

import { timeAgo } from '../../../utilAngular/CommonServices';
import { MenuButton, MenuTitle } from '../MenuButton/MenuButton';
import { NotificationDetails } from './NotificationDetails';
import { NxFontAwesomeIcon } from '@sonatype/react-shared-components';

const NotificationsMenu = (props) => {
  const { notificationsToDisplay, loading, error, loadNotifications, setNotificationViewed } = props;
  const [selectedNotification, setSelectedNotification] = useState(null);
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

  const handleMenuChange = (isOpen) => !isOpen && setSelectedNotification(null);

  const notificationMappingToComponent = (notification) => {
    const { age, qualifier } = timeAgo(notification.dateCreated);
    return (
      <div
        key={notification.id + notification.summaryText}
        className={classnames(['iq-dropdown-menu__link--main-header', 'iq-notification'], {
          viewed: notification.viewed,
          selected: selectedNotification && selectedNotification.id === notification.id,
        })}
        onClick={() => handleViewNotificationDetails(notification)}
      >
        <span className="iq-notification__age">{age}</span>
        <span className="iq-notification__age-qualifier">{qualifier}</span>
        <span className="iq-notification__text">{notification.summaryText}</span>
      </div>
    );
  };

  return (
    <div className="iq-notifications-menu-button">
      <MenuButton icon={faBell} iconLabel="Notifications" onChange={handleMenuChange} closeOnClick={false}>
        <MenuTitle>Notifications</MenuTitle>

        {loading && <div className="alert alert-info">Loading notification content from server...</div>}

        {error && <div className="alert alert-danger">{error}</div>}

        {!loading && !error && (
          <div className="iq-scrollable">
            {notificationsToDisplay && notificationsToDisplay.map(notificationMappingToComponent)}
            {selectedNotification && <NotificationDetails notification={selectedNotification} />}
          </div>
        )}
      </MenuButton>

      {error && <NxFontAwesomeIcon className="iq-notif-error" icon={faExclamationCircle} />}
      {!error && unreadNotificationCount > 0 && <div className="iq-unread-dot"></div>}
    </div>
  );
};

NotificationsMenu.propTypes = {
  notificationsToDisplay: PropTypes.array,
  selectedNotification: PropTypes.any,
  loading: PropTypes.bool,
  error: PropTypes.string,
  loadNotifications: PropTypes.func,
  setNotificationViewed: PropTypes.func,
};

export default NotificationsMenu;
