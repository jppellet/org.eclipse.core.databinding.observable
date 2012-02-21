/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matthew Hall - bug 233306, 226289, 190881
 *******************************************************************************/
package org.eclipse.core.databinding.observable.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.set.WritableSet;
import org.eclipse.core.runtime.Assert;

/**
 * A read-only observable map formed by the composition of two observable maps.
 * If map1 maps keys a:A to values b1:B, and map2 maps keys b2:B to values c:C,
 * the composite map maps keys a:A to values c:C. For example, map1 could map
 * Order objects to their corresponding Customer objects, and map2 could map
 * Customer objects to their "last name" property of type String. The composite
 * map of map1 and map2 would then map Order objects to their customers' last
 * names.
 * 
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @since 1.1
 * 
 */
public class CompositeMap<K, I, V> extends ObservableMap<K, V> {
	// adds that need to go through the second map and thus will be picked up by
	// secondMapListener.
	private Set<I> pendingAdds = new HashSet<I>();

	// Removes that need to go through the second map and thus will be picked up
	// by secondMapListener. Maps from value being removed to key being removed.
	private Map<I, K> pendingRemoves = new HashMap<I, K>();

	// Changes that need to go through the second map and thus will be picked up
	// by secondMapListener. Maps from old value to new value and new value to old
	// value.
	private Map<I, I> pendingChanges = new HashMap<I, I>();

	private IMapChangeListener<K, I> firstMapListener = new IMapChangeListener<K, I>() {

		public void handleMapChange(MapChangeEvent<K, I> event) {
			MapDiff<K, I> diff = event.diff;
			Set<I> rangeSetAdditions = new HashSet<I>();
			Set<I> rangeSetRemovals = new HashSet<I>();
			final Set<K> adds = new HashSet<K>();
			final Set<K> changes = new HashSet<K>();
			final Set<K> removes = new HashSet<K>();
			final Map<K, V> oldValues = new HashMap<K, V>();

			for (Iterator<K> it = diff.getAddedKeys().iterator(); it.hasNext();) {
				K addedKey = it.next();
				I newValue = diff.getNewValue(addedKey);
				if (!rangeSet.contains(newValue)) {
					pendingAdds.add(newValue);
					rangeSetAdditions.add(newValue);
				} else {
					adds.add(addedKey);
					wrappedMap.put(addedKey, secondMap.get(newValue));
				}
			}
			for (Iterator<K> it = diff.getChangedKeys().iterator(); it
					.hasNext();) {
				K changedKey = it.next();
				I oldValue = diff.getOldValue(changedKey);
				I newValue = diff.getNewValue(changedKey);
				boolean removed = firstMap.getKeys(oldValue).isEmpty();
				boolean added = !rangeSet.contains(newValue);
				if (removed) {
					pendingRemoves.put(oldValue, changedKey);
					rangeSetRemovals.add(oldValue);
				}
				if (added) {
					pendingAdds.add(newValue);
					rangeSetAdditions.add(newValue);
				}
				if (added || removed) {
					pendingChanges.put(oldValue, newValue);
					pendingChanges.put(newValue, oldValue);
				} else {
					changes.add(changedKey);
//					oldValues.put(changedKey, oldValue); // jpp: was bug!
					oldValues.put(changedKey, secondMap.get(oldValue));
					wrappedMap.put(changedKey, secondMap.get(newValue));
				}
			}
			for (Iterator<K> it = diff.getRemovedKeys().iterator(); it
					.hasNext();) {
				K removedKey = it.next();
				I oldValue = diff.getOldValue(removedKey);
				if (firstMap.getKeys(oldValue).isEmpty()) {
					pendingRemoves.put(oldValue, removedKey);
					rangeSetRemovals.add(oldValue);
				} else {
					removes.add(removedKey);
					oldValues.put(removedKey, secondMap.get(oldValue));
					wrappedMap.remove(removedKey);
				}
			}

			if (adds.size() > 0 || removes.size() > 0 || changes.size() > 0) {
				fireMapChange(new MapDiff<K, V>() {

					public Set<K> getAddedKeys() {
						return adds;
					}

					public Set<K> getChangedKeys() {
						return changes;
					}

					public V getNewValue(Object key) {
						return wrappedMap.get(key);
					}

					public V getOldValue(Object key) {
						return oldValues.get(key);
					}

					public Set<K> getRemovedKeys() {
						return removes;
					}
				});
			}

			if (rangeSetAdditions.size() > 0 || rangeSetRemovals.size() > 0) {
				rangeSet.addAndRemove(rangeSetAdditions, rangeSetRemovals);
			}
		}
	};

	private IMapChangeListener<I, V> secondMapListener = new IMapChangeListener<I, V>() {

		public void handleMapChange(MapChangeEvent<I, V> event) {
			MapDiff<I, V> diff = event.diff;
			final Set<K> adds = new HashSet<K>();
			final Set<K> changes = new HashSet<K>();
			final Set<K> removes = new HashSet<K>();
			final Map<K, V> oldValues = new HashMap<K, V>();
			final Map<K, V> newValues = new HashMap<K, V>();
			Set<I> addedKeys = new HashSet<I>(diff.getAddedKeys());
			Set<I> removedKeys = new HashSet<I>(diff.getRemovedKeys());

			for (Iterator<I> it = addedKeys.iterator(); it.hasNext();) {
				I addedKey = it.next();
				Set<K> elements = firstMap.getKeys(addedKey);
				V newValue = diff.getNewValue(addedKey);
				if (pendingChanges.containsKey(addedKey)) {
					I oldKey = pendingChanges.remove(addedKey);
					V oldValue;
					if (removedKeys.remove(oldKey)) {
						oldValue = diff.getOldValue(oldKey);
					} else {
						oldValue = secondMap.get(oldKey);
					}
					pendingChanges.remove(oldKey);
					pendingAdds.remove(addedKey);
					pendingRemoves.remove(oldKey);
					for (Iterator<K> it2 = elements.iterator(); it2.hasNext();) {
						K element = it2.next();
						changes.add(element);
						oldValues.put(element, oldValue);
						newValues.put(element, newValue);
						wrappedMap.put(element, newValue);
					}
				} else if (pendingAdds.remove(addedKey)) {
					for (Iterator<K> it2 = elements.iterator(); it2.hasNext();) {
						K element = it2.next();
						adds.add(element);
						newValues.put(element, newValue);
						wrappedMap.put(element, newValue);
					}
				} else {
					Assert.isTrue(false, "unexpected case"); //$NON-NLS-1$
				}
			}
			for (Iterator<I> it = diff.getChangedKeys().iterator(); it
					.hasNext();) {
				I changedKey = it.next();
				Set<K> elements = firstMap.getKeys(changedKey);
				for (Iterator<K> it2 = elements.iterator(); it2.hasNext();) {
					K element = it2.next();
					changes.add(element);
					oldValues.put(element, diff.getOldValue(changedKey));
					V newValue = diff.getNewValue(changedKey);
					newValues.put(element, newValue);
					wrappedMap.put(element, newValue);
				}
			}
			for (Iterator<I> it = removedKeys.iterator(); it.hasNext();) {
				I removedKey = it.next();
				K element = pendingRemoves.remove(removedKey);
				if (element != null) {
					if (pendingChanges.containsKey(removedKey)) {
						Object newKey = pendingChanges.remove(removedKey);
						pendingChanges.remove(newKey);
						pendingAdds.remove(newKey);
						pendingRemoves.remove(removedKey);
						changes.add(element);
						oldValues.put(element, diff.getOldValue(removedKey));
						V newValue = secondMap.get(newKey);
						newValues.put(element, newValue);
						wrappedMap.put(element, newValue);
					} else {
						removes.add(element);
						V oldValue = diff.getOldValue(removedKey);
						oldValues.put(element, oldValue);
						wrappedMap.remove(element);
					}
				} else {
					Assert.isTrue(false, "unexpected case"); //$NON-NLS-1$
				}
			}

			if (adds.size() > 0 || removes.size() > 0 || changes.size() > 0) {
				fireMapChange(new MapDiff<K, V>() {

					public Set<K> getAddedKeys() {
						return adds;
					}

					public Set<K> getChangedKeys() {
						return changes;
					}

					public V getNewValue(Object key) {
						return newValues.get(key);
					}

					public V getOldValue(Object key) {
						return oldValues.get(key);
					}

					public Set<K> getRemovedKeys() {
						return removes;
					}
				});
			}
		}
	};

	private BidiObservableMap<K, I> firstMap;
	private IObservableMap<I, V> secondMap;

	private static class WritableSetPlus<E> extends WritableSet<E> {
		void addAndRemove(Set<E> additions, Set<E> removals) {
			wrappedSet.removeAll(removals);
			wrappedSet.addAll(additions);
			fireSetChange(Diffs.createSetDiff(additions, removals));
		}
	}

	private WritableSetPlus<I> rangeSet = new WritableSetPlus<I>();

	/**
	 * Creates a new composite map. Because the key set of the second map is
	 * determined by the value set of the given observable map
	 * <code>firstMap</code>, it cannot be passed in as an argument. Instead,
	 * the second map will be created by calling
	 * <code>secondMapFactory.createObservable(valueSet())</code>.
	 * 
	 * @param firstMap
	 *            the first map
	 * @param secondMapFactory
	 *            a factory that creates the second map when given an observable
	 *            set representing the value set of <code>firstMap</code>.
	 */
	public CompositeMap(
			IObservableMap<K, I> firstMap,
			IObservableFactory<? extends IObservableMap<I, V>, ? super WritableSet<I>> secondMapFactory) {
		super(firstMap.getRealm(), new HashMap<K, V>());
		this.firstMap = new BidiObservableMap<K, I>(firstMap);
		this.firstMap.addMapChangeListener(firstMapListener);
		rangeSet.addAll(this.firstMap.values());
		this.secondMap = secondMapFactory
				.createObservable(rangeSet);
		secondMap.addMapChangeListener(secondMapListener);
		for (Iterator<Map.Entry<K, I>> it = this.firstMap.entrySet().iterator(); it
				.hasNext();) {
			Map.Entry<K, I> entry = it.next();
			wrappedMap.put(entry.getKey(), secondMap.get(entry.getValue()));
		}
	}

	/**
	 * @since 1.2
	 */
	public Object getKeyType() {
		return firstMap.getKeyType();
	}

	/**
	 * @since 1.2
	 */
	public Object getValueType() {
		return secondMap.getValueType();
	}

	public synchronized void dispose() {
		super.dispose();
		if (firstMap != null) {
			firstMap.removeMapChangeListener(firstMapListener);
			firstMap = null;
		}
		if (secondMap != null) {
			secondMap.dispose();
			secondMap = null;
		}
	}

}
