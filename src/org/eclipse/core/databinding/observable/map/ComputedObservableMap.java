/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matthew Hall - bugs 241585, 247394, 226289, 194734, 190881, 266754,
 *                    268688
 *     Ovidio Mallo - bug 303847
 *******************************************************************************/

package org.eclipse.core.databinding.observable.map;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.StaleEvent;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.internal.databinding.identity.IdentitySet;

/**
 * Maps objects to one of their attributes. Tracks changes to the underlying
 * observable set of objects (keys), as well as changes to attribute values.
 */
public abstract class ComputedObservableMap<K, V> extends
AbstractObservableMap<K, V> {

private IObservableSet<K> keySet;

private Set<K> knownKeys;

	private Object valueType;

	private ISetChangeListener<K> setChangeListener = new ISetChangeListener<K>() {
		public void handleSetChange(SetChangeEvent<K> event) {
			Set<K> addedKeys = new HashSet<K>(event.diff.getAdditions());
			Set<K> removedKeys = new HashSet<K>(event.diff.getRemovals());
			Map<K, V> oldValues = new HashMap<K, V>();
			Map<K, V> newValues = new HashMap<K, V>();
			for (Iterator<K> it = removedKeys.iterator(); it.hasNext();) {
				K removedKey = it.next();
				V oldValue = null;
				if (removedKey != null) {
					oldValue = doGet(removedKey);
					unhookListener(removedKey);
					knownKeys.remove(removedKey);
				}
				oldValues.put(removedKey, oldValue);
			}
			for (Iterator<K> it = addedKeys.iterator(); it.hasNext();) {
				K addedKey = it.next();
				V newValue = null;
				if (addedKey != null) {
					newValue = doGet(addedKey);
					hookListener(addedKey);
					knownKeys.add(addedKey);
				}
				newValues.put(addedKey, newValue);
			}
			fireMapChange(Diffs.createMapDiff(addedKeys, removedKeys,
					Collections.<K>emptySet(), oldValues, newValues));
		}
	};

	private IStaleListener staleListener = new IStaleListener() {
		public void handleStale(StaleEvent staleEvent) {
			fireStale();
		}
	};

	private Set<Map.Entry<K, V>> entrySet = new EntrySet();

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		public Iterator<Map.Entry<K, V>> iterator() {
			final Iterator<K> keyIterator = keySet.iterator();
			return new Iterator<Map.Entry<K, V>>() {

				public boolean hasNext() {
					return keyIterator.hasNext();
				}

				public Map.Entry<K, V> next() {
					final K key = keyIterator.next();
					return new Map.Entry<K, V>() {

						public K getKey() {
							getterCalled();
							return key;
						}

						public V getValue() {
							return get(getKey());
						}

						public V setValue(V value) {
							return put(getKey(), value);
						}
					};
				}

				public void remove() {
					keyIterator.remove();
				}
			};
		}

		public int size() {
			return keySet.size();
		}

	}

	/**
	 * @param keySet
	 */
	public ComputedObservableMap(IObservableSet<K> keySet) {
		this(keySet, null);
	}

	/**
	 * @param keySet
	 * @param valueType
	 * @since 1.2
	 */
	public ComputedObservableMap(IObservableSet<K> keySet, Object valueType) {
		super(keySet.getRealm());
		this.keySet = keySet;
		this.valueType = valueType;

		keySet.addDisposeListener(new IDisposeListener() {
			public void handleDispose(DisposeEvent staleEvent) {
				ComputedObservableMap.this.dispose();
			}
		});
	}

	/**
	 * @deprecated Subclasses are no longer required to call this method.
	 */
	protected void init() {
	}

	protected void firstListenerAdded() {
		getRealm().exec(new Runnable() {
			public void run() {
				hookListeners();
			}
		});
	}

	protected void lastListenerRemoved() {
		unhookListeners();
	}

	private void hookListeners() {
		if (keySet != null) {
			knownKeys = new IdentitySet<K>();
			keySet.addSetChangeListener(setChangeListener);
			keySet.addStaleListener(staleListener);
			for (Iterator<K> it = this.keySet.iterator(); it.hasNext();) {
				K key = it.next();
				hookListener(key);
				knownKeys.add(key);
			}
		}
	}

	@SuppressWarnings("unchecked")
	// safe
	private void unhookListeners() {
		if (keySet != null) {
			keySet.removeSetChangeListener(setChangeListener);
			keySet.removeStaleListener(staleListener);
		}
		if (knownKeys != null) {
			Object[] keys = knownKeys.toArray();
			for (int i = 0; i < keys.length; i++) {
				unhookListener((K) keys[i]);
			}
			knownKeys.clear();
			knownKeys = null;
		}
	}

	protected final void fireSingleChange(K key, V oldValue, V newValue) {
		fireMapChange(Diffs.createMapDiffSingleChange(key, oldValue, newValue));
	}

	/**
	 * @since 1.2
	 */
	public Object getKeyType() {
		return keySet.getElementType();
	}

	/**
	 * @since 1.2
	 */
	public Object getValueType() {
		return valueType;
	}

	/**
	 * @since 1.3
	 */
	public V remove(Object key) {
		checkRealm();

		V oldValue = get(key);
		keySet().remove(key);

		return oldValue;
	}

	/**
	 * @since 1.3
	 */
	public boolean containsKey(Object key) {
		getterCalled();
		return keySet().contains(key);
	}

	public Set<Map.Entry<K, V>> entrySet() {
		return entrySet;
	}

	public Set<K> keySet() {
		return keySet;
	}

	final public V get(Object key) {
		getterCalled();
		if (!keySet.contains(key))
			return null;
		@SuppressWarnings("unchecked")
		K castKey = (K) key; // safe, because contained by set
		return doGet(castKey);
	}

	private void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	final public V put(K key, V value) {
		checkRealm();
		if (!keySet.contains(key))
			return null;
		return doPut(key, value);
	}

	/**
	 * @param removedKey
	 */
	protected abstract void unhookListener(K removedKey);

	/**
	 * @param addedKey
	 */
	protected abstract void hookListener(K addedKey);

	/**
	 * @param key
	 * @return the value for the given key
	 */
	protected abstract V doGet(K key);

	/**
	 * @param key
	 * @param value
	 * @return the old value for the given key
	 */
	protected abstract V doPut(K key, V value);

	public boolean isStale() {
		return super.isStale() || keySet.isStale();
	}

	public synchronized void dispose() {
		unhookListeners();
		entrySet = null;
		keySet = null;
		setChangeListener = null;
		super.dispose();
	}
}
