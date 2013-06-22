package reactor.fsm

import reactor.fn.Function
import spock.lang.Specification

/**
 * @author Jon Brisbin
 */
class StateMachineSpec extends Specification {

  def "StateMachines provide a fixed set of events"() {
    given:
      "a StateMachine with states"
      def expectedStates = ["pre", "first", "second", "third", "post"]
      def states = []
      def fsm = new StateMachine.Spec().
          sync().
          using(expectedStates as String[]).
          when("pre", {
            states << "pre"
            null
          } as Function<String, String>).
          when("first", {
            states << "first"
            null
          } as Function<String, String>).
          when("second", {
            states << "second"
            null
          } as Function<String, String>).
          when("third", {
            states << "third"
            null
          } as Function<String, String>).
          when("post", {
            states << "post"
            null
          } as Function<String, String>).
          get()
    when:
      "the states are transitioned through"
      expectedStates.each {
        fsm.state(it)
      }

    then:
      "each state handler was invoked"
      states == expectedStates
  }

  def "StateMachines recognize special state strings"() {
    given:
      "a StateMachine with four states"
      def states = []
      def fsm = new StateMachine.Spec().
          using('one', 'two', 'three', 'four').
          when('one', { s ->
            states << 'one'
            'two' == s ? 'three' : 'fsm:next'
          } as Function<String, String>).
          when('two', { s ->
            states << 'two'
            'fsm:first'
          } as Function<String, String>).
          when('three', { s ->
            states << 'three'
            'four' == s ? null : 'fsm:next'
          } as Function<String, String>).
          when('four', { s ->
            states << 'four'
            'fsm:prev'
          } as Function<String, String>).
          get()

    when:
      "states are transitioned"
      fsm.next()

    then:
      "all states were transitioned through"
      fsm.current() == 'three'
      states == ['one', 'two', 'one', 'three', 'four', 'three']
  }

  def "StateMachines automatically transition through all states"() {
    given:
      "a StateMachine with three states"
      def count = 0
      def fsm = new StateMachine.Spec().
          using('one', 'two', 'three').
          when('one', { s ->
            count++
            'two'
          } as Function<String, String>).
          when('two', { s ->
            count++
            'three'
          } as Function<String, String>).
          when('three', { s ->
            count++
            null
          } as Function<String, String>).
          get()

    when:
      "states are transitioned"
      fsm.next()

    then:
      "all states were transitioned through"
      fsm.current() == 'three'
      count == 3
  }

}
