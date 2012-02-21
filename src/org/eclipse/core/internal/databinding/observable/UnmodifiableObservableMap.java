/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - bug 237718
 ******************************************************************************/

package org.eclipse.core.internal.databinding.observable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.map.DecoratingObservableMap;
import org.eclipse.core.databinding.observable.map.IObservableMap;

/**
 * IObservableMap implementation that prevents modification by consumers. Events
 * in the originating wrapped map are propagated and thrown from this instance
 * when appropriate. All mutators throw an UnsupportedOperationException.
 * 
 * @since 1.0
 */
public class UnmodifiableObservableMap<K, V> extends
DecoratingObservableMap<K, V> {
Map<K, V> unmodifiableMap;

	/**
	 * @param decorated
	 */
public UnmodifiableObservableMap(IObservableMap<K, V> decorated) {
		super(decorated, false);
		this.unmodifiableMap = Collections.unmodifiableMap(decorated);
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		getterCalled();
		return unmodifiableMap.entrySet();
	}

	public Set<K> keySet() {
		getterCalled();
		return unmodifiableMap.keySet();
	}

	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}

	public Collection<V> values() {
		getterCalled();
		return unmodifiableMap.values();
	}

	public synchronized void dispose() {
		unmodifiableMap = null;
		super.dispose();
	}
}
