export default function routeStateUtilService($state) {
  return {
    /**
     * Determines whether the specified router state requires the user to be authenticated.  If no parameter is passed,
     * the current state is checked
     */
    stateRequiresAuthentication(state = $state.current) {
      const { data } = state,
          authenticationRequired = data ? data.authenticationRequired !== false : true;

      return authenticationRequired;
    }
  };
}

routeStateUtilService.$inject = ['$state'];
