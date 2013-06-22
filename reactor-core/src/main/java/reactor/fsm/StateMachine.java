package reactor.fsm;

import reactor.core.ComponentSpec;
import reactor.core.Reactor;
import reactor.fn.Consumer;
import reactor.fn.Event;
import reactor.fn.Function;
import reactor.util.Assert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import static reactor.fn.Functions.$;

/**
 * A {@code StateMachine} is a {@link Reactor}-backed component that facilitates assigning a fixed set of states and
 * their handlers. A {@code StateMachine} can be transitioned from one state to the next either by calling the {@link
 * #next()} and {@link #previous()} methods.
 * <p/>
 * Each state can have one or more {@link Function Functions} assigned that will be invoked when that state is reached.
 * A {@link Function} receives as its argument, the previous state the machine was in before transitioning. It is
 * expected to return the next state to automatically transition to, or {@code null}, which means simply consume the
 * event and don't transition the state at all.
 *
 * @author Jon Brisbin
 */
public class StateMachine {

	private final ReentrantLock transitionLock = new ReentrantLock();
	private final Reactor      reactor;
	private final String[]     origStates;
	private final int          numStates;
	private final List<String> states;
	private final    Map<String, Event<String>> events       = new HashMap<String, Event<String>>();
	private volatile int                        currentIdx   = -1;
	private volatile String                     currentState = null;

	private StateMachine(Reactor reactor, Map<String, Function<String, String>> fns, String... states) {
		this.reactor = reactor;
		this.origStates = states;
		this.numStates = states.length;
		this.states = Arrays.asList(states);
		for (String state : states) {
			Event<String> ev = Event.wrap(state);
			this.events.put(state, ev);

			if (!fns.containsKey(state)) {
				continue;
			}
			final Function<String, String> fn = fns.get(state);
			reactor.on(
					$(state),
					new Consumer<Event<String>>() {
						@Override
						public void accept(Event<String> ev) {
							String next = fn.apply(ev.getData());
							if (null != next) {
								if ("fsm:next".equals(next)) {
									next();
								} else if ("fsm:prev".equals(next)) {
									previous();
								} else if ("fsm:first".equals(next)) {
									state(first());
								} else if ("fsm:last".equals(next)) {
									state(last());
								} else if ("fsm:current".equals(next)) {
									state(ev.getData());
								} else {
									state(next);
								}
							}
						}
					}
			);
		}
	}

	/**
	 * Return the first state in this chain.
	 *
	 * @return The first state value.
	 */
	public String first() {
		return (numStates > 0 ? origStates[0] : null);
	}

	/**
	 * Return the last state in this chain.
	 *
	 * @return The last state value.
	 */
	public String last() {
		return (numStates > 0 ? origStates[numStates - 1] : null);
	}

	/**
	 * Return the current state value.
	 *
	 * @return The current state value.
	 */
	public String current() {
		transitionLock.lock();
		try {
			return currentState;
		} finally {
			transitionLock.unlock();
		}
	}

	/**
	 * Rewind this {@link StateMachine} to the first state.
	 *
	 * @return The current state, which at the start is {@literal null}.
	 */
	public String rewind() {
		transitionLock.lock();
		try {
			currentIdx = -1;
			currentState = null;
		} finally {
			transitionLock.unlock();
		}
		return null;
	}

	/**
	 * Transition to the next state. States are traversed in the order the were at creation time.
	 *
	 * @return The current state value.
	 */
	public String next() {
		transitionLock.lock();
		try {
			String prevState = currentState;
			int nextIdx = (currentIdx < numStates - 1 ? ++currentIdx : 0);
			moveTo(nextIdx, prevState);
		} finally {
			transitionLock.unlock();
		}
		return currentState;
	}

	/**
	 * Transition to the previous state. States are traversed in the order the were at creation time.
	 *
	 * @return The current state value.
	 */
	public String previous() {
		transitionLock.lock();
		try {
			String prevState = currentState;
			int nextIdx = (currentIdx - 1 >= 0 ? --currentIdx : numStates - 1);
			moveTo(nextIdx, prevState);
		} finally {
			transitionLock.unlock();
		}
		return currentState;
	}

	/**
	 * Update to the given state value.
	 *
	 * @param state The state to transition to.
	 * @return The current state value.
	 */
	public String state(String state) {
		Assert.state(states.contains(state), "State '" + state + "' not one of " + states);
		transitionLock.lock();
		try {
			String prevState = currentState;
			int nextIdx = states.indexOf(state);
			moveTo(nextIdx, prevState);
		} finally {
			transitionLock.unlock();
		}
		return currentState;
	}

	private void moveTo(int idx, String prevState) {
		this.currentState = states.get(idx);
		this.currentIdx = idx;
		notify(prevState);
	}

	private void notify(String prev) {
		reactor.notify(currentState, (null != prev ? events.get(prev) : Event.NULL_EVENT));
	}

	/**
	 * Create a new {@link StateMachine} based on this specification.
	 */
	public static class Spec extends ComponentSpec<Spec, StateMachine> {
		private final Map<String, Function<String, String>> fns = new HashMap<String, Function<String, String>>();
		private String[] states;

		/**
		 * Set the states to use for this machine.
		 *
		 * @param states The states to use.
		 * @return {@literal this}
		 */
		public Spec using(String... states) {
			this.states = states;
			return this;
		}

		/**
		 * Assign a {@link Function} to be invoked when the machine transitions to the given state.
		 *
		 * @param state The state value to assign the function to.
		 * @param fn    The {@link Function} to invoke.
		 * @return {@literal this}
		 */
		public Spec when(String state, Function<String, String> fn) {
			fns.put(state, fn);
			return this;
		}

		@Override
		protected StateMachine configure(final Reactor reactor) {
			return new StateMachine(reactor, fns, states);
		}
	}

}
