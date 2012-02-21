/*******************************************************************************
 * Copyright (c) 2010 Ovidio Mallo and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ovidio Mallo - initial API and implementation (bug 305367)
 ******************************************************************************/

package org.eclipse.core.internal.databinding.observable.masterdetail;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObserving;
import org.eclipse.core.databinding.observable.IStaleListener;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.StaleEvent;
import org.eclipse.core.databinding.observable.map.AbstractObservableMap;
import org.eclipse.core.databinding.observable.map.IMapChangeListener;
import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.core.databinding.observable.map.MapChangeEvent;
import org.eclipse.core.databinding.observable.map.MapDiff;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.internal.databinding.identity.IdentityMap;
import org.eclipse.core.internal.databinding.identity.IdentitySet;
import org.eclipse.core.internal.databinding.observable.Util;

/**
 * @since 1.4
 */
public class MapDetailValueObservableMap<K, I, V> extends AbstractObservableMap<K, V>
		implements IObserving {

	private IObservableMap<K, I> masterMap;

	private IObservableFactory<IObservableValue<V>, ? super I> observableValueFactory;

	private Object detailValueType;

	private Set<Map.Entry<K, V>> entrySet;

	private IdentityHashMap<K, IObservableValue<V>> keyDetailMap = new IdentityHashMap<K, IObservableValue<V>>();

	private IdentitySet<IObservableValue<V>> staleDetailObservables = new IdentitySet<IObservableValue<V>>();

	private IMapChangeListener<K, I> masterMapListener = new IMapChangeListener<K, I>() {
		public void handleMapChange(MapChangeEvent<K, I> event) {
			handleMasterMapChange(event.diff);
		}
	};

	private IStaleListener masterStaleListener = new IStaleListener() {
		public void handleStale(StaleEvent staleEvent) {
			fireStale();
		}
	};

	private IStaleListener detailStaleListener = new IStaleListener() {
		@SuppressWarnings("unchecked")
		// safe because this listener is only added to IObservableValue<V>
		public void handleStale(StaleEvent staleEvent) {
			addStaleDetailObservable((IObservableValue<V>) staleEvent
					.getObservable());
		}
	};

	/**
	 * @param masterMap
	 * @param observableValueFactory
	 * @param detailValueType
	 */
	public MapDetailValueObservableMap(IObservableMap<K, I> masterMap,
			IObservableFactory<IObservableValue<V>, ? super I> observableValueFactory, Object detailValueType) {
		super(masterMap.getRealm());
		this.masterMap = masterMap;
		this.observableValueFactory = observableValueFactory;
		this.detailValueType = detailValueType;

		// Add change/stale/dispose listeners on the master map.
		masterMap.addMapChangeListener(masterMapListener);
		masterMap.addStaleListener(masterStaleListener);
		masterMap.addDisposeListener(new IDisposeListener() {
			public void handleDispose(DisposeEvent event) {
				MapDetailValueObservableMap.this.dispose();
			}
		});

		// Initialize the map with the current state of the master map.
		MapDiff<K, I> initMasterDiff = Diffs.computeMapDiff(Collections.<K, I>emptyMap(),
				masterMap);
		handleMasterMapChange(initMasterDiff);
	}

	private void handleMasterMapChange(MapDiff<K, I> diff) {
		// Collect the detail values for the master values in the input diff.
		IdentityMap<K, V> oldValues = new IdentityMap<K, V>();
		IdentityMap<K, V> newValues = new IdentityMap<K, V>();

		// Handle added master values.
		Set<K> addedKeys = diff.getAddedKeys();
		for (Iterator<K> iter = addedKeys.iterator(); iter.hasNext();) {
			K addedKey = iter.next();

			// For added master values, we set up a new detail observable.
			addDetailObservable(addedKey);

			// Get the value of the created detail observable for the new diff.
			IObservableValue<V> detailValue = getDetailObservableValue(addedKey);
			newValues.put(addedKey, detailValue.getValue());
		}

		// Handle removed master values.
		Set<K> removedKeys = diff.getRemovedKeys();
		for (Iterator<K> iter = removedKeys.iterator(); iter.hasNext();) {
			K removedKey = iter.next();

			// First of all, get the current detail value and add it to the set
			// of old values of the new diff.
			IObservableValue<V> detailValue = getDetailObservableValue(removedKey);
			oldValues.put(removedKey, detailValue.getValue());

			// For removed master values, we dispose the detail observable.
			removeDetailObservable(removedKey);
		}

		// Handle changed master values.
		Set<K> changedKeys = diff.getChangedKeys();
		for (Iterator<K> iter = changedKeys.iterator(); iter.hasNext();) {
			K changedKey = iter.next();

			// Get the detail value prior to the change and add it to the set of
			// old values of the new diff.
			IObservableValue<V> oldDetailValue = getDetailObservableValue(changedKey);
			oldValues.put(changedKey, oldDetailValue.getValue());

			// Remove the old detail value for the old master value and add it
			// again for the new master value.
			removeDetailObservable(changedKey);
			addDetailObservable(changedKey);

			// Get the new detail value and add it to the set of new values.
			IObservableValue<V> newDetailValue = getDetailObservableValue(changedKey);
			newValues.put(changedKey, newDetailValue.getValue());
		}

		// The different key sets are the same, only the values change.
		fireMapChange(Diffs.createMapDiff(addedKeys, removedKeys, changedKeys,
				oldValues, newValues));
	}

	private void addDetailObservable(final K addedKey) {
		I masterElement = masterMap.get(addedKey);

		IObservableValue<V> detailValue = keyDetailMap
				.get(addedKey);

		if (detailValue == null) {
			detailValue = createDetailObservable(masterElement);

			keyDetailMap.put(addedKey, detailValue);

			detailValue.addValueChangeListener(new IValueChangeListener<V>() {
				public void handleValueChange(ValueChangeEvent<V> event) {
					if (!event.getObservableValue().isStale()) {
						staleDetailObservables.remove(event.getSource());
					}

					fireMapChange(Diffs.createMapDiffSingleChange(addedKey,
							event.diff.getOldValue(), event.diff.getNewValue()));
				}
			});

			if (detailValue.isStale()) {
				addStaleDetailObservable(detailValue);
			}
		}

		detailValue.addStaleListener(detailStaleListener);
	}

	private IObservableValue<V> createDetailObservable(I masterElement) {
		ObservableTracker.setIgnore(true);
		try {
			return observableValueFactory
					.createObservable(masterElement);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	private void removeDetailObservable(K removedKey) {
		if (isDisposed()) {
			return;
		}

		IObservableValue<V> detailValue = keyDetailMap
				.remove(removedKey);
		staleDetailObservables.remove(detailValue);
		detailValue.dispose();
	}

	private IObservableValue<V> getDetailObservableValue(K masterKey) {
		return keyDetailMap.get(masterKey);
	}

	private void addStaleDetailObservable(IObservableValue<V> detailObservable) {
		boolean wasStale = isStale();
		staleDetailObservables.add(detailObservable);
		if (!wasStale) {
			fireStale();
		}
	}

	public Set<K> keySet() {
		getterCalled();

		return masterMap.keySet();
	}

	public V get(Object key) {
		getterCalled();

		if (!containsKey(key)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		// safe because key is contained
		K castKey = (K) key;
		IObservableValue<V> detailValue = getDetailObservableValue(castKey);
		return detailValue.getValue();
	}

	public V put(K key, V value) {
		if (!containsKey(key)) {
			return null;
		}

		IObservableValue<V> detailValue = getDetailObservableValue(key);
		V oldValue = detailValue.getValue();
		detailValue.setValue(value);
		return oldValue;
	}

	public boolean containsKey(Object key) {
		getterCalled();

		return masterMap.containsKey(key);
	}

	public V remove(Object key) {
		checkRealm();

		if (!containsKey(key)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		// safe because key is contained
		K castKey = (K) key;
		IObservableValue<V> detailValue = getDetailObservableValue(castKey);
		V oldValue = detailValue.getValue();

		masterMap.remove(key);

		return oldValue;
	}

	public int size() {
		getterCalled();

		return masterMap.size();
	}

	public boolean isStale() {
		return super.isStale()
				|| (masterMap != null && masterMap.isStale())
				|| (staleDetailObservables != null && !staleDetailObservables
						.isEmpty());
	}

	public Object getKeyType() {
		return masterMap.getKeyType();
	}

	public Object getValueType() {
		return detailValueType;
	}

	public Object getObserved() {
		return masterMap;
	}

	public synchronized void dispose() {
		if (masterMap != null) {
			masterMap.removeMapChangeListener(masterMapListener);
			masterMap.removeStaleListener(masterStaleListener);
		}

		if (keyDetailMap != null) {
			for (Iterator<IObservableValue<V>> iter = keyDetailMap.values().iterator(); iter
					.hasNext();) {
				IObservableValue<V> detailValue = iter.next();
				detailValue.dispose();
			}
			keyDetailMap.clear();
		}

		masterMap = null;
		observableValueFactory = null;
		detailValueType = null;
		keyDetailMap = null;
		masterStaleListener = null;
		detailStaleListener = null;
		staleDetailObservables = null;

		super.dispose();
	}

	public Set<Map.Entry<K, V>> entrySet() {
		getterCalled();

		if (entrySet == null) {
			entrySet = new EntrySet();
		}
		return entrySet;
	}

	private void getterCalled() {
		ObservableTracker.getterCalled(this);
	}

	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		public Iterator<Map.Entry<K, V>> iterator() {
			final Iterator<K> keyIterator = keySet().iterator();
			return new Iterator<Map.Entry<K, V>>() {

				public boolean hasNext() {
					return keyIterator.hasNext();
				}

				public Map.Entry<K, V> next() {
					K key = keyIterator.next();
					return new MapEntry(key);
				}

				public void remove() {
					keyIterator.remove();
				}
			};
		}

		public int size() {
			return MapDetailValueObservableMap.this.size();
		}
	}

	private final class MapEntry implements Map.Entry<K, V> {

		private final K key;

		private MapEntry(K key) {
			this.key = key;
		}

		public K getKey() {
			MapDetailValueObservableMap.this.getterCalled();
			return key;
		}

		public V getValue() {
			return MapDetailValueObservableMap.this.get(getKey());
		}

		public V setValue(V value) {
			return MapDetailValueObservableMap.this.put(getKey(), value);
		}

		public boolean equals(Object o) {
			MapDetailValueObservableMap.this.getterCalled();
			if (o == this)
				return true;
			if (o == null)
				return false;
			if (!(o instanceof Map.Entry))
				return false;
			Map.Entry<?, ?> that = (Map.Entry<?, ?>) o;
			return Util.equals(this.getKey(), that.getKey())
					&& Util.equals(this.getValue(), that.getValue());
		}

		public int hashCode() {
			MapDetailValueObservableMap.this.getterCalled();
			Object value = getValue();
			return (getKey() == null ? 0 : getKey().hashCode())
					^ (value == null ? 0 : value.hashCode());
		}
	}
}
