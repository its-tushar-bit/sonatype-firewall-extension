describe('brain.client.js', function() {
  describe('getCsrfHeaders', function() {
    it('can read the CSRF cookie', function() {
      expect(Brain.getCsrfHeaders()).toEqual({ 'X-CSRF-TOKEN' : '' });
      document.cookie = 'CLM-CSRF-TOKEN = csrfToken';
      document.cookie = 'CLM-SESSION = sessionId';
      expect(Brain.getCsrfHeaders()).toEqual({ 'X-CSRF-TOKEN' : 'csrfToken' });
    });
  });
});
