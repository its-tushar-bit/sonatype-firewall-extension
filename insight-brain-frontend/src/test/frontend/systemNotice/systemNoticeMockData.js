export default {
  getSystemNotice: function(message, enabled) {
    return {
      'message': message,
      'enabled': enabled
    };
  }
};
