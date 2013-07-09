/*
 * Copyright (c) 2011-2013 GoPivotal, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.event.registry;


import reactor.event.selector.Selector;
import reactor.support.Supports;

/**
 * A {@code SelectionStrategy} is used to provide custom {@link reactor.event.selector.Selector} matching behaviour.
 *
 * @author Jon Brisbin
 * @author Andy Wilkinson
 * @author Stephane Maldini
 */
public interface SelectionStrategy extends Supports<Object> {

	/**
	 * Indicates whether or not the {@link reactor.event.selector.Selector} matches the {@code key}.
	 *
	 * @param selector The selector to perform the match
	 * @param key The object to match
	 *
	 * @return {@code true} if the Selector matches, otherwise {@code false}.
	 */
	boolean matches(Selector selector, Object key);

}