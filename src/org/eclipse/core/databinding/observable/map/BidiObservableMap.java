/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *         (through BidirectionalMap.java)
 *     Matthew Hall - bug 233306
 *******************************************************************************/
package org.eclipse.core.databinding.observable.map;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.internal.databinding.observable.Util;

/**
 * An
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @since 1.2
 */
public class BidiObservableMap<K, V> extends DecoratingObservableMap<K, V> {
	/**
	 * Inverse of wrapped map. When multiple keys map to the same value, they
	 * are combined into a Set. This field is null when no listeners are
	 * registered on this observable.
	 */
	private Map<V, Object> valuesToKeys;

	/**
	 * Constructs a BidirectionalMap tracking the given observable map.
	 * 
	 * @param wrappedMap
	 *            the observable map to track
	 */
	public BidiObservableMap(IObservableMap<K, V> wrappedMap) {
		super(wrappedMap, false);
	}

	protected void firstListenerAdded() {
		valuesToKeys = new HashMap<V, Object>();
		for (Iterator<Map.Entry<K, V>> it = entrySet().iterator(); it.hasNext();) {
			Map.Entry<K, V> entry = it.next();
			addMapping(entry.getKey(), entry.getValue());
		}
		super.firstListenerAdded();
	}

	protected void lastListenerRemoved() {
		super.lastListenerRemoved();
		valuesToKeys.clear();
		valuesToKeys = null;
	}

	protected void handleMapChange(MapChangeEvent<K, V> event) {
		MapDiff<K, V> diff = event.diff;
		for (Iterator<K> it = diff.getAddedKeys().iterator(); it.hasNext();) {
			K addedKey = it.next();
			addMapping(addedKey, diff.getNewValue(addedKey));
		}
		for (Iterator<K> it = diff.getChangedKeys().iterator(); it.hasNext();) {
			K changedKey = it.next();
			removeMapping(changedKey, diff.getOldValue(changedKey));
			addMapping(changedKey, diff.getNewValue(changedKey));
		}
		for (Iterator<K> it = diff.getRemovedKeys().iterator(); it.hasNext();) {
			K removedKey = it.next();
			removeMapping(removedKey, diff.getOldValue(removedKey));
		}
		super.handleMapChange(event);
	}

	public boolean containsValue(Object value) {
		getterCalled();
		// Faster lookup
		if (valuesToKeys != null)
			return valuesToKeys.containsKey(value);
		return super.containsValue(value);
	}

	/**
	 * Adds a mapping from value to key in the valuesToKeys map.
	 * 
	 * @param key
	 *            the key being mapped
	 * @param value
	 *            the value being mapped
	 */
	@SuppressWarnings("unchecked")
	private void addMapping(K key, V value) {
		if (!valuesToKeys.containsKey(value)) {
			Object wrappedKey = key;
			if (wrappedKey instanceof Set<?>)
				wrappedKey = new HashSet<Set<?>>(
						Collections.singleton((Set<?>) wrappedKey));
			valuesToKeys.put(value, wrappedKey);
		} else {
			Object elementOrSet = valuesToKeys.get(value);
			Set<Object> set;
			if (elementOrSet instanceof Set<?>) {
				set = (Set<Object>) elementOrSet;
			} else {
				set = new HashSet<Object>(Collections.singleton(elementOrSet));
				valuesToKeys.put(value, set);
			}
			set.add(key);
		}
	}

	/**
	 * Removes a mapping from value to key in the valuesToKeys map.
	 * 
	 * @param key
	 *            the key being unmapped
	 * @param value
	 *            the value being unmapped
	 */
	private void removeMapping(Object key, Object value) {
		if (valuesToKeys.containsKey(value)) {
			Object elementOrSet = valuesToKeys.get(value);
			if (elementOrSet instanceof Set) {
				Set<?> set = (Set<?>) elementOrSet;
				set.remove(key);
				if (set.isEmpty()) {
					valuesToKeys.remove(value);
				}
			} else if (elementOrSet == key
					|| (elementOrSet != null && elementOrSet.equals(key))) {
				valuesToKeys.remove(value);
			}
		}
	}

	/**
	 * Returns the Set of keys that currently map to the given value.
	 * 
	 * @param value
	 *            the value associated with the keys in the returned Set.
	 * @return the Set of keys that map to the given value. If no keys map to
	 *         the given value, an empty set is returned.
	 */
	@SuppressWarnings("unchecked")
	public Set<K> getKeys(V value) {
		// valuesToKeys is null when no listeners are registered
		if (valuesToKeys == null)
			return findKeys(value);

		if (!valuesToKeys.containsKey(value))
			return Collections.emptySet();
		Object elementOrSet = valuesToKeys.get(value);
		if (elementOrSet instanceof Set<?>)
			return Collections.unmodifiableSet((Set<K>) elementOrSet);
		return (Set<K>) Collections.singleton(elementOrSet);
	}

	/**
	 * Iterates the map and returns the set of keys which currently map to the
	 * given value.
	 * 
	 * @param value
	 *            the value to search for
	 * @return the set of keys which currently map to the specified value.
	 */
	private Set<K> findKeys(V value) {
		Set<K> keys = new HashSet<K>();
		for (Iterator<Map.Entry<K, V>> it = entrySet().iterator(); it.hasNext();) {
			Map.Entry<K, V> entry = it.next();
			if (Util.equals(entry.getValue(), value))
				keys.add(entry.getKey());
		}
		return keys;
	}

	public synchronized void dispose() {
		if (valuesToKeys != null) {
			valuesToKeys.clear();
			valuesToKeys = null;
		}
		super.dispose();
	}
}
