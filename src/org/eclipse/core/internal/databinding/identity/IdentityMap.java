/*******************************************************************************
 * Copyright (c) 2008, 2010 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 215531)
 *     Matthew Hall - bug 228125
 *         (through ViewerElementMap.java)
 *     Matthew Hall - bugs 262269, 303847
 ******************************************************************************/

package org.eclipse.core.internal.databinding.identity;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.databinding.observable.Util;
import org.eclipse.core.runtime.Assert;

/**
 * A {@link Map} whose keys are added, removed and compared by identity. The
 * keys in the map are compared using <code>==</code> instead of
 * {@link #equals(Object)}.
 * <p>
 * This class is <i>not</i> a strict implementation the {@link Map} interface.
 * It intentionally violates the {@link Map} contract, which requires the use of
 * {@link #equals(Object)} when comparing keys.
 * 
 * @since 1.2
 */
public class IdentityMap<K, V> implements Map<K, V> {
	private Map<IdentityWrapper<K>, V> wrappedMap;

	/**
	 * Constructs an IdentityMap.
	 */
	public IdentityMap() {
		this.wrappedMap = new HashMap<IdentityWrapper<K>, V>();
	}

	/**
	 * Constructs an IdentityMap containing all the entries in the specified
	 * map.
	 * 
	 * @param map
	 *            the map whose entries are to be added to this map.
	 */
	public IdentityMap(Map<K, V> map) {
		this();
		Assert.isNotNull(map);
		putAll(map);
	}

	public void clear() {
		wrappedMap.clear();
	}

	public boolean containsKey(Object key) {
		return wrappedMap.containsKey(IdentityWrapper.wrap(key));
	}

	public boolean containsValue(Object value) {
		return wrappedMap.containsValue(value);
	}

	public Set<Map.Entry<K, V>> entrySet() {
		final Set<Map.Entry<IdentityWrapper<K>, V>> wrappedEntrySet = wrappedMap
				.entrySet();
		return new Set<Map.Entry<K, V>>() {
			public boolean add(Map.Entry<K, V> o) {
				throw new UnsupportedOperationException();
			}

			public boolean addAll(Collection<? extends Map.Entry<K, V>> c) {
				throw new UnsupportedOperationException();
			}

			public void clear() {
				wrappedEntrySet.clear();
			}

			public boolean contains(Object o) {
				for (Iterator<Map.Entry<K, V>> iterator = iterator(); iterator
						.hasNext();)
					if (iterator.next().equals(o))
						return true;
				return false;
			}

			public boolean containsAll(Collection<?> c) {
				for (Iterator<?> iterator = c.iterator(); iterator.hasNext();)
					if (!contains(iterator.next()))
						return false;
				return true;
			}

			public boolean isEmpty() {
				return wrappedEntrySet.isEmpty();
			}

			public Iterator<Map.Entry<K, V>> iterator() {
				final Iterator<Map.Entry<IdentityWrapper<K>, V>> wrappedIterator = wrappedEntrySet
						.iterator();
				return new Iterator<Map.Entry<K, V>>() {
					public boolean hasNext() {
						return wrappedIterator.hasNext();
					}

					public Map.Entry<K, V> next() {
						final Map.Entry<IdentityWrapper<K>, V> wrappedEntry = wrappedIterator
								.next();
						return new Map.Entry<K, V>() {
							public K getKey() {
								return wrappedEntry.getKey()
										.unwrap();
							}

							public V getValue() {
								return wrappedEntry.getValue();
							}

							public V setValue(V value) {
								return wrappedEntry.setValue(value);
							}

							public boolean equals(Object obj) {
								if (obj == this)
									return true;
								if (obj == null || !(obj instanceof Map.Entry))
									return false;
								Map.Entry<?, ?> that = (Map.Entry<?, ?>) obj;
								return this.getKey() == that.getKey()
										&& Util.equals(this.getValue(), that
												.getValue());
							}

							public int hashCode() {
								return wrappedEntry.hashCode();
							}
						};
					}

					public void remove() {
						wrappedIterator.remove();
					}
				};
			}

			public boolean remove(Object o) {
				@SuppressWarnings("unchecked")
				final Map.Entry<K, V> unwrappedEntry = (Map.Entry<K, V>) o;
				final IdentityWrapper<K> wrappedKey = IdentityWrapper
						.wrap(unwrappedEntry.getKey());
				Map.Entry<IdentityWrapper<K>, V> wrappedEntry = new Map.Entry<IdentityWrapper<K>, V>() {
					public IdentityWrapper<K> getKey() {
						return wrappedKey;
					}

					public V getValue() {
						return unwrappedEntry.getValue();
					}

					public V setValue(V value) {
						throw new UnsupportedOperationException();
					}

					public boolean equals(Object obj) {
						if (obj == this)
							return true;
						if (obj == null || !(obj instanceof Map.Entry))
							return false;
						Map.Entry<?, ?> that = (Map.Entry<?, ?>) obj;
						return Util.equals(wrappedKey, that.getKey())
								&& Util
										.equals(this.getValue(), that
												.getValue());
					}

					public int hashCode() {
						return wrappedKey.hashCode()
								^ (getValue() == null ? 0 : getValue()
										.hashCode());
					}
				};
				return wrappedEntrySet.remove(wrappedEntry);
			}

			public boolean removeAll(Collection<?> c) {
				boolean changed = false;
				for (Iterator<?> iterator = c.iterator(); iterator.hasNext();)
					changed |= remove(iterator.next());
				return changed;
			}

			public boolean retainAll(Collection<?> c) {
				boolean changed = false;
				Object[] toRetain = c.toArray();
				outer: for (Iterator<?> iterator = iterator(); iterator
						.hasNext();) {
					Object entry = iterator.next();
					for (int i = 0; i < toRetain.length; i++)
						if (entry.equals(toRetain[i]))
							continue outer;
					iterator.remove();
					changed = true;
				}
				return changed;
			}

			public int size() {
				return wrappedEntrySet.size();
			}

			public Object[] toArray() {
				return toArray(new Object[size()]);
			}

			@SuppressWarnings("unchecked")
			public <T> T[] toArray(T[] a) {
				int size = size();
				if (a.length < size) {
					a = (T[]) Array.newInstance(
							a.getClass().getComponentType(), size);
				}
				int i = 0;
				for (Iterator<Map.Entry<K, V>> iterator = iterator(); iterator
						.hasNext();) {
					a[i] = (T) iterator.next();
					i++;
				}
				return a;
			}

			public boolean equals(Object obj) {
				if (obj == this)
					return true;
				if (obj == null || !(obj instanceof Set))
					return false;
				Set<?> that = (Set<?>) obj;
				return this.size() == that.size() && containsAll(that);
			}

			public int hashCode() {
				return wrappedEntrySet.hashCode();
			}
		};
	}

	public V get(Object key) {
		return wrappedMap.get(IdentityWrapper.wrap(key));
	}

	public boolean isEmpty() {
		return wrappedMap.isEmpty();
	}

	public Set<K> keySet() {
		final Set<IdentityWrapper<K>> wrappedKeySet = wrappedMap.keySet();
		return new Set<K>() {
			public boolean add(Object o) {
				throw new UnsupportedOperationException();
			}

			public boolean addAll(Collection<? extends K> c) {
				throw new UnsupportedOperationException();
			}

			public void clear() {
				wrappedKeySet.clear();
			}

			public boolean contains(Object o) {
				return wrappedKeySet.contains(IdentityWrapper.wrap(o));
			}

			public boolean containsAll(Collection<?> c) {
				for (Iterator<?> iterator = c.iterator(); iterator.hasNext();)
					if (!wrappedKeySet.contains(IdentityWrapper.wrap(iterator
							.next())))
						return false;
				return true;
			}

			public boolean isEmpty() {
				return wrappedKeySet.isEmpty();
			}

			public Iterator<K> iterator() {
				final Iterator<IdentityWrapper<K>> wrappedIterator = wrappedKeySet
						.iterator();
				return new Iterator<K>() {
					public boolean hasNext() {
						return wrappedIterator.hasNext();
					}

					public K next() {
						return wrappedIterator.next()
								.unwrap();
					}

					public void remove() {
						wrappedIterator.remove();
					}
				};
			}

			public boolean remove(Object o) {
				return wrappedKeySet.remove(IdentityWrapper.wrap(o));
			}

			public boolean removeAll(Collection<?> c) {
				boolean changed = false;
				for (Iterator<?> iterator = c.iterator(); iterator.hasNext();)
					changed |= wrappedKeySet.remove(IdentityWrapper
							.wrap(iterator.next()));
				return changed;
			}

			public boolean retainAll(Collection<?> c) {
				boolean changed = false;
				Object[] toRetain = c.toArray();
				outer: for (Iterator<?> iterator = iterator(); iterator
						.hasNext();) {
					Object element = iterator.next();
					for (int i = 0; i < toRetain.length; i++)
						if (element == toRetain[i])
							continue outer;
					// element not contained in collection, remove.
					remove(element);
					changed = true;
				}
				return changed;
			}

			public int size() {
				return wrappedKeySet.size();
			}

			public Object[] toArray() {
				return toArray(new Object[wrappedKeySet.size()]);
			}

			@SuppressWarnings("unchecked")
			public <T> T[] toArray(T[] a) {
				int size = wrappedKeySet.size();
				IdentityWrapper<T>[] wrappedArray = wrappedKeySet
						.toArray(IdentityWrapper.<T> newArray(size));
				T[] result = a;
				if (a.length < size) {
					result = (T[]) Array.newInstance(a.getClass()
							.getComponentType(), size);
				}
				for (int i = 0; i < size; i++)
					result[i] = wrappedArray[i].unwrap();
				return result;
			}

			public boolean equals(Object obj) {
				if (obj == this)
					return true;
				if (obj == null || !(obj instanceof Set))
					return false;
				Set<?> that = (Set<?>) obj;
				return this.size() == that.size() && containsAll(that);
			}

			public int hashCode() {
				return wrappedKeySet.hashCode();
			}
		};
	}

	public V put(K key, V value) {
		return wrappedMap.put(IdentityWrapper.wrap(key), value);
	}

	public void putAll(Map<? extends K, ? extends V> other) {
		for (Map.Entry<? extends K, ? extends V> entry : other.entrySet()) {
			wrappedMap.put(IdentityWrapper.<K>wrap(entry.getKey()), entry
					.getValue());
		}
	}

	public V remove(Object key) {
		return wrappedMap.remove(IdentityWrapper.wrap(key));
	}

	public int size() {
		return wrappedMap.size();
	}

	public Collection<V> values() {
		return wrappedMap.values();
	}

	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj == null || !(obj instanceof Map))
			return false;
		Map<?, ?> that = (Map<?, ?>) obj;
		return this.entrySet().equals(that.entrySet());
	}

	public int hashCode() {
		return wrappedMap.hashCode();
	}
}
