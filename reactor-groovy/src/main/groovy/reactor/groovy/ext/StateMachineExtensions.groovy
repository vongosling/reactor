package reactor.groovy.ext

import reactor.fsm.StateMachine
import reactor.groovy.support.ClosureFunction

/**
 * @author Jon Brisbin
 */
class StateMachineExtensions {

  static StateMachine.Spec when(StateMachine.Spec self, String state, Closure<String> fn) {
    return self.when(state, new ClosureFunction<String, String>(fn))
  }

}
