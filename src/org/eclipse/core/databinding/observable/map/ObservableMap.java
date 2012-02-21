/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brad Reynolds - bug 164653
 *     Matthew Hall - bugs 226289, 274450
 *******************************************************************************/

package org.eclipse.core.databinding.observable.map;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.AbstractObservable;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.Realm;

/**
 * 
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @since 1.0
 */
public class ObservableMap<K, V> extends AbstractObservable implements
IObservableMap<K, V> {

	protected Map<K, V> wrappedMap;

	private boolean stale = false;

	/**
	 * @param wrappedMap
	 */
	public ObservableMap(Map<K, V> wrappedMap) {
		this(Realm.getDefault(), wrappedMap);
	}

	/**
	 * @param realm
	 * @param wrappedMap
	 */
	public ObservableMap(Realm realm, Map<K, V> wrappedMap) {
		super(realm);
		this.wrappedMap = wrappedMap;
	}

	public synchronized void addMapChangeListener(
			IMapChangeListener<? super K, ? super V> listener) {
		addListener(MapChangeEvent.TYPE, listener);
	}

	public synchronized void removeMapChangeListener(
			IMapChangeListener<? super K, ? super V> listener) {
		removeListener(MapChangeEvent.TYPE, listener);
	}

	/**
	 * @since 1.2
	 */
	public Object getKeyType() {
		return null;
	}

	/**
	 * @since 1.2
	 */
	public Object getValueType() {
		return null;
	}

	protected void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	protected void fireMapChange(MapDiff<K, V> diff) {
		checkRealm();

		// fire general change event first
		super.fireChange();

		fireEvent(new MapChangeEvent<K, V>(this, diff));
	}

	public boolean containsKey(Object key) {
		getterCalled();
		return wrappedMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		getterCalled();
		return wrappedMap.containsValue(value);
	}

	public Set<Map.Entry<K, V>> entrySet() {
		getterCalled();
		return wrappedMap.entrySet();
	}

	public V get(Object key) {
		getterCalled();
		return wrappedMap.get(key);
	}

	public boolean isEmpty() {
		getterCalled();
		return wrappedMap.isEmpty();
	}

	public Set<K> keySet() {
		getterCalled();
		return wrappedMap.keySet();
	}

	public int size() {
		getterCalled();
		return wrappedMap.size();
	}

	public Collection<V> values() {
		getterCalled();
		return wrappedMap.values();
	}

	/**
	 * Returns the stale state. Must be invoked from the current realm.
	 * 
	 * @return stale state
	 */
	public boolean isStale() {
		checkRealm();
		return stale;
	}

	/**
	 * Sets the stale state. Must be invoked from the current realm.
	 * 
	 * @param stale
	 *            The stale state to set. This will fire a stale event if the
	 *            given boolean is true and this observable set was not already
	 *            stale.
	 */
	public void setStale(boolean stale) {
		checkRealm();
		boolean wasStale = this.stale;
		this.stale = stale;
		if (!wasStale && stale) {
			fireStale();
		}
	}

	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public void putAll(Map<? extends K, ? extends V> arg0) {
		throw new UnsupportedOperationException();
	}

	public boolean equals(Object o) {
		getterCalled();
		return o == this || wrappedMap.equals(o);
	}

	public int hashCode() {
		getterCalled();
		return wrappedMap.hashCode();
	}

	public synchronized void dispose() {
		super.dispose();
	}
}
