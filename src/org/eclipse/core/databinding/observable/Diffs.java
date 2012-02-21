/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matthew Hall - bug 226216
 *******************************************************************************/

package org.eclipse.core.databinding.observable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.list.ListDiff;
import org.eclipse.core.databinding.observable.list.ListDiffEntry;
import org.eclipse.core.databinding.observable.map.MapDiff;
import org.eclipse.core.databinding.observable.set.SetDiff;
import org.eclipse.core.databinding.observable.value.ValueDiff;
import org.eclipse.core.internal.databinding.observable.Util;

/**
 * @since 1.0
 * 
 */
public class Diffs {

	/**
	 * Returns a {@link ListDiff} describing the change between the specified
	 * old and new list states.
	 * 
	 * @param oldList
	 *            the old list state
	 * @param newList
	 *            the new list state
	 * @return the differences between oldList and newList
	 */
	public static <E> ListDiff<E> computeListDiff(List<E> oldList,
			List<E> newList) {
		List<ListDiffEntry<E>> diffEntries = new ArrayList<ListDiffEntry<E>>();
		createListDiffs(new ArrayList<E>(oldList), newList, diffEntries);
		ListDiff<E> listDiff = createListDiff(ListDiffEntry
				.newArrayFrom(diffEntries));
		return listDiff;
	}

	/**
	 * Returns a lazily computed {@link ListDiff} describing the change between
	 * the specified old and new list states.
	 * 
	 * @param oldList
	 *            the old list state
	 * @param newList
	 *            the new list state
	 * @return a lazily computed {@link ListDiff} describing the change between
	 *         the specified old and new list states.
	 * @since 1.3
	 */
	public static <E> ListDiff<E> computeLazyListDiff(final List<E> oldList,
			final List<E> newList) {
		return new ListDiff<E>() {
			ListDiff<E> lazyDiff;

			public ListDiffEntry<E>[] getDifferences() {
				if (lazyDiff == null) {
					lazyDiff = Diffs.computeListDiff(oldList, newList);
				}
				return lazyDiff.getDifferences();
			}
		};
	}

	/**
	 * adapted from EMF's ListDifferenceAnalyzer
	 */
	private static <E> void createListDiffs(List<E> oldList, List<E> newList,
			List<ListDiffEntry<E>> listDiffs) {
		int index = 0;
		for (Iterator<E> it = newList.iterator(); it.hasNext();) {
			E newValue = it.next();
			if (oldList.size() <= index) {
				// append newValue to newList
				listDiffs.add(createListDiffEntry(index, true, newValue));
			} else {
				boolean done;
				do {
					done = true;
					E oldValue = oldList.get(index);
					if (oldValue == null ? newValue != null : !oldValue
							.equals(newValue)) {
						int oldIndexOfNewValue = listIndexOf(oldList, newValue,
								index);
						if (oldIndexOfNewValue != -1) {
							int newIndexOfOldValue = listIndexOf(newList,
									oldValue, index);
							if (newIndexOfOldValue == -1) {
								// removing oldValue from list[index]
								listDiffs.add(createListDiffEntry(index, false,
										oldValue));
								oldList.remove(index);
								done = false;
							} else if (newIndexOfOldValue > oldIndexOfNewValue) {
								// moving oldValue from list[index] to
								// [newIndexOfOldValue]
								if (oldList.size() <= newIndexOfOldValue) {
									// The element cannot be moved to the
									// correct index
									// now, however later iterations will insert
									// elements
									// in front of it, eventually moving it into
									// the
									// correct spot.
									newIndexOfOldValue = oldList.size() - 1;
								}
								listDiffs.add(createListDiffEntry(index, false,
										oldValue));
								oldList.remove(index);
								listDiffs.add(createListDiffEntry(
										newIndexOfOldValue, true, oldValue));
								oldList.add(newIndexOfOldValue, oldValue);
								done = false;
							} else {
								// move newValue from list[oldIndexOfNewValue]
								// to [index]
								listDiffs.add(createListDiffEntry(
										oldIndexOfNewValue, false, newValue));
								oldList.remove(oldIndexOfNewValue);
								listDiffs.add(createListDiffEntry(index, true,
										newValue));
								oldList.add(index, newValue);
							}
						} else {
							// add newValue at list[index]
							oldList.add(index, newValue);
							listDiffs.add(createListDiffEntry(index, true,
									newValue));
						}
					}
				} while (!done);
			}
			++index;
		}
		for (int i = oldList.size(); i > index;) {
			// remove excess trailing elements not present in newList
			listDiffs.add(createListDiffEntry(--i, false, oldList.get(i)));
		}
	}

	/**
	 * @param list
	 * @param object
	 * @param index
	 * @return the index, or -1 if not found
	 */
	private static <E> int listIndexOf(List<E> list, E object, int index) {
		int size = list.size();
		for (int i = index; i < size; i++) {
			E candidate = list.get(i);
			if (candidate == null ? object == null : candidate.equals(object)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Checks whether the two objects are <code>null</code> -- allowing for
	 * <code>null</code>.
	 * 
	 * @param left
	 *            The left object to compare; may be <code>null</code>.
	 * @param right
	 *            The right object to compare; may be <code>null</code>.
	 * @return <code>true</code> if the two objects are equivalent;
	 *         <code>false</code> otherwise.
	 */
	public static final boolean equals(final Object left, final Object right) {
		return left == null ? right == null : ((right != null) && left
				.equals(right));
	}

	/**
	 * Returns a {@link SetDiff} describing the change between the specified old
	 * and new set states.
	 * 
	 * @param oldSet
	 *            the old set state
	 * @param newSet
	 *            the new set state
	 * @return a {@link SetDiff} describing the change between the specified old
	 *         and new set states.
	 */
	public static <E> SetDiff<E> computeSetDiff(Set<E> oldSet, Set<E> newSet) {
		Set<E> additions = new HashSet<E>(newSet);
		additions.removeAll(oldSet);
		Set<E> removals = new HashSet<E>(oldSet);
		removals.removeAll(newSet);
		return createSetDiff(additions, removals);
	}

	/**
	 * Returns a lazily computed {@link SetDiff} describing the change between
	 * the specified old and new set states.
	 * 
	 * @param oldSet
	 *            the old set state
	 * @param newSet
	 *            the new set state
	 * @return a lazily computed {@link SetDiff} describing the change between
	 *         the specified old and new set states.
	 * @since 1.3
	 */
	public static <E> SetDiff<E> computeLazySetDiff(final Set<E> oldSet,
			final Set<E> newSet) {
		return new SetDiff<E>() {

			private SetDiff<E> lazyDiff;

			private SetDiff<E> getLazyDiff() {
				if (lazyDiff == null) {
					lazyDiff = computeSetDiff(oldSet, newSet);
				}
				return lazyDiff;
			}

			public Set<E> getAdditions() {
				return getLazyDiff().getAdditions();
			}

			public Set<E> getRemovals() {
				return getLazyDiff().getRemovals();
			}

		};
	}

	/**
	 * Returns a {@link MapDiff} describing the change between the specified old
	 * and new map states.
	 * 
	 * @param oldMap
	 *            the old map state
	 * @param newMap
	 *            the new map state
	 * @return a {@link MapDiff} describing the change between the specified old
	 *         and new map states.
	 */
	public static <K, V> MapDiff<K, V> computeMapDiff(Map<K, V> oldMap,
			Map<K, V> newMap) {
		// starts out with all keys from the new map, we will remove keys from
		// the old map as we go
		final Set<K> addedKeys = new HashSet<K>(newMap.keySet());
		final Set<K> removedKeys = new HashSet<K>();
		final Set<K> changedKeys = new HashSet<K>();
		final Map<K, V> oldValues = new HashMap<K, V>();
		final Map<K, V> newValues = new HashMap<K, V>();
		for (Iterator<Map.Entry<K, V>> it = oldMap.entrySet().iterator(); it
				.hasNext();) {
			Map.Entry<K, V> oldEntry = it.next();
			K oldKey = oldEntry.getKey();
			if (addedKeys.remove(oldKey)) {
				// potentially changed key since it is in oldMap and newMap
				V oldValue = oldEntry.getValue();
				V newValue = newMap.get(oldKey);
				if (!Util.equals(oldValue, newValue)) {
					changedKeys.add(oldKey);
					oldValues.put(oldKey, oldValue);
					newValues.put(oldKey, newValue);
				}
			} else {
				removedKeys.add(oldKey);
				oldValues.put(oldKey, oldEntry.getValue());
			}
		}
		for (Iterator<K> it = addedKeys.iterator(); it.hasNext();) {
			K newKey = it.next();
			newValues.put(newKey, newMap.get(newKey));
		}
		return new MapDiff<K, V>() {
			public Set<K> getAddedKeys() {
				return addedKeys;
			}

			public Set<K> getChangedKeys() {
				return changedKeys;
			}

			public Set<K> getRemovedKeys() {
				return removedKeys;
			}

			public V getNewValue(Object key) {
				return newValues.get(key);
			}

			public V getOldValue(Object key) {
				return oldValues.get(key);
			}
		};
	}

	/**
	 * Returns a lazily computed {@link MapDiff} describing the change between
	 * the specified old and new map states.
	 * 
	 * @param oldMap
	 *            the old map state
	 * @param newMap
	 *            the new map state
	 * @return a lazily computed {@link MapDiff} describing the change between
	 *         the specified old and new map states.
	 * @since 1.3
	 */
	public static <K, V> MapDiff<K, V> computeLazyMapDiff(
			final Map<K, V> oldMap, final Map<K, V> newMap) {
		return new MapDiff<K, V>() {

			private MapDiff<K, V> lazyDiff;

			private MapDiff<K, V> getLazyDiff() {
				if (lazyDiff == null) {
					lazyDiff = computeMapDiff(oldMap, newMap);
				}
				return lazyDiff;
			}

			public Set<K> getAddedKeys() {
				return getLazyDiff().getAddedKeys();
			}

			public Set<K> getRemovedKeys() {
				return getLazyDiff().getRemovedKeys();
			}

			public Set<K> getChangedKeys() {
				return getLazyDiff().getChangedKeys();
			}

			public V getOldValue(Object key) {
				return getLazyDiff().getOldValue(key);
			}

			public V getNewValue(Object key) {
				return getLazyDiff().getNewValue(key);
			}

		};
	}

	/**
	 * @param oldValue
	 * @param newValue
	 * @return a value diff
	 */
	public static <T> ValueDiff<T> createValueDiff(final T oldValue,
			final T newValue) {
		return new ValueDiff<T>() {

			public T getOldValue() {
				return oldValue;
			}

			public T getNewValue() {
				return newValue;
			}
		};
	}

	/**
	 * @param additions
	 * @param removals
	 * @return a set diff
	 */
	public static <E> SetDiff<E> createSetDiff(Set<E> additions, Set<E> removals) {
		final Set<E> unmodifiableAdditions = Collections
				.unmodifiableSet(additions);
		final Set<E> unmodifiableRemovals = Collections
				.unmodifiableSet(removals);
		return new SetDiff<E>() {

			public Set<E> getAdditions() {
				return unmodifiableAdditions;
			}

			public Set<E> getRemovals() {
				return unmodifiableRemovals;
			}
		};
	}

//	/**
//	 * @param difference
//	 * @return a list diff with one differing entry
//	 */
//	public static ListDiff createListDiff(ListDiffEntry difference) {
//		return createListDiff(new ListDiffEntry[] { difference });
//	}
//
//	/**
//	 * @param difference1
//	 * @param difference2
//	 * @return a list diff with two differing entries
//	 */
//	public static ListDiff createListDiff(ListDiffEntry difference1,
//			ListDiffEntry difference2) {
//		return createListDiff(new ListDiffEntry[] { difference1, difference2 });
//	}
	
	/**
	 * @param difference
	 * @return a list diff with the given entries
	 */
	public static <E> ListDiff<E> createListDiff(
			final ListDiffEntry<E> difference) {
		final ListDiffEntry<E>[] differences = ListDiffEntry.newArray(1);
		differences[0] = difference;
		return new ListDiff<E>() {
			public ListDiffEntry<E>[] getDifferences() {
				return differences;
			}
		};
	}

	/**
	 * @param difference1
	 * @param difference2
	 * @return a list diff with the given entries
	 */
	public static <E> ListDiff<E> createListDiff(ListDiffEntry<E> difference1,
			ListDiffEntry<E> difference2) {
		final ListDiffEntry<E>[] differences = ListDiffEntry.newArray(2);
		differences[0] = difference1;
		differences[1] = difference2;
		return new ListDiff<E>() {
			public ListDiffEntry<E>[] getDifferences() {
				return differences;
			}
		};
	}


	/**
	 * @param differences
	 * @return a list diff with the given entries
	 */
	public static <E> ListDiff<E> createListDiff(
			final ListDiffEntry<E>[] differences) {
		return new ListDiff<E>() {
			public ListDiffEntry<E>[] getDifferences() {
				return differences;
			}
		};
	}

	/**
	 * @param position
	 * @param isAddition
	 * @param element
	 * @return a list diff entry
	 */
	public static <E> ListDiffEntry<E> createListDiffEntry(final int position,
			final boolean isAddition, final E element) {
		return new ListDiffEntry<E>() {

			public int getPosition() {
				return position;
			}

			public boolean isAddition() {
				return isAddition;
			}

			public E getElement() {
				return element;
			}
		};
	}

	/**
	 * @param addedKey
	 * @param newValue
	 * @return a map diff
	 */
	public static <K, V> MapDiff<K, V> createMapDiffSingleAdd(final K addedKey,
			final V newValue) {
		return new MapDiff<K, V>() {

			public Set<K> getAddedKeys() {
				return Collections.singleton(addedKey);
			}

			public Set<K> getChangedKeys() {
				return Collections.emptySet();
			}

			public V getNewValue(Object key) {
				return newValue;
			}

			public V getOldValue(Object key) {
				return null;
			}

			public Set<K> getRemovedKeys() {
				return Collections.emptySet();
			}
		};
	}

	/**
	 * @param existingKey
	 * @param oldValue
	 * @param newValue
	 * @return a map diff
	 */
	public static <K, V> MapDiff<K, V> createMapDiffSingleChange(
			final K existingKey, final V oldValue, final V newValue) {
		return new MapDiff<K, V>() {

			public Set<K> getAddedKeys() {
				return Collections.emptySet();
			}

			public Set<K> getChangedKeys() {
				return Collections.singleton(existingKey);
			}

			public V getNewValue(Object key) {
				return newValue;
			}

			public V getOldValue(Object key) {
				return oldValue;
			}

			public Set<K> getRemovedKeys() {
				return Collections.emptySet();
			}
		};
	}

	/**
	 * @param removedKey
	 * @param oldValue
	 * @return a map diff
	 */
	public static <K, V> MapDiff<K, V> createMapDiffSingleRemove(
			final K removedKey, final V oldValue) {
		return new MapDiff<K, V>() {

			public Set<K> getAddedKeys() {
				return Collections.emptySet();
			}

			public Set<K> getChangedKeys() {
				return Collections.emptySet();			}

			public V getNewValue(Object key) {
				return null;
			}

			public V getOldValue(Object key) {
				return oldValue;
			}

			public Set<K> getRemovedKeys() {
				return Collections.singleton(removedKey);
			}
		};
	}

	/**
	 * @param copyOfOldMap
	 * @return a map diff
	 */
	public static <K, V> MapDiff<K, V> createMapDiffRemoveAll(
			final Map<K, V> copyOfOldMap) {
		return new MapDiff<K, V>() {

			public Set<K> getAddedKeys() {
				return Collections.emptySet();
			}

			public Set<K> getChangedKeys() {
				return Collections.emptySet();
			}

			public V getNewValue(Object key) {
				return null;
			}

			public V getOldValue(Object key) {
				return copyOfOldMap.get(key);
			}

			public Set<K> getRemovedKeys() {
				return copyOfOldMap.keySet();
			}
		};
	}

	/**
	 * @param addedKeys
	 * @param removedKeys
	 * @param changedKeys
	 * @param oldValues
	 * @param newValues
	 * @return a map diff
	 */
	public static <K, V> MapDiff<K, V> createMapDiff(final Set<K> addedKeys,
			final Set<K> removedKeys, final Set<K> changedKeys,
			final Map<K, V> oldValues, final Map<K, V> newValues) {
		return new MapDiff<K, V>() {

			public Set<K> getAddedKeys() {
				return addedKeys;
			}

			public Set<K> getChangedKeys() {
				return changedKeys;
			}

			public V getNewValue(Object key) {
				return newValues.get(key);
			}

			public V getOldValue(Object key) {
				return oldValues.get(key);
			}

			public Set<K> getRemovedKeys() {
				return removedKeys;
			}
		};
	}
}
