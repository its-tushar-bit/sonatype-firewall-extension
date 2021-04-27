/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { connect } from 'react-redux';

import { loadNotifications, setNotificationViewed } from './notificationsActions';
import NotificationsMenu from './NotificationsMenu';

function mapStateToProps({ notifications }) {
  const { loading, error, notificationsToDisplay } = notifications;

  return {
    notificationsToDisplay,
    loading,
    error,
  };
}

const mapDispatchToProps = {
  loadNotifications,
  setNotificationViewed,
};

const NotificationsMenuContainer = connect(mapStateToProps, mapDispatchToProps)(NotificationsMenu);
export default NotificationsMenuContainer;
