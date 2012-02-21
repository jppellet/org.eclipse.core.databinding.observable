/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 237718)
 *     Matthew Hall - but 246626, 226289
 ******************************************************************************/

package org.eclipse.core.databinding.observable.map;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.DecoratingObservable;

/**
 * An observable map which decorates another observable map.
 * 
 * @since 1.2
 */
public class DecoratingObservableMap<K, V> extends DecoratingObservable
implements IObservableMap<K, V> {
	private IObservableMap<K, V> decorated;

	private IMapChangeListener<K, V> mapChangeListener;

	/**
	 * Constructs a DecoratingObservableMap which decorates the given
	 * observable.
	 * 
	 * @param decorated
	 *            the observable map being decorated
	 * @param disposeDecoratedOnDispose
	 */
	public DecoratingObservableMap(IObservableMap<K, V> decorated,
			boolean disposeDecoratedOnDispose) {
		super(decorated, disposeDecoratedOnDispose);
		this.decorated = decorated;
	}

	public synchronized void addMapChangeListener(
			IMapChangeListener<? super K, ? super V> listener) {
		addListener(MapChangeEvent.TYPE, listener);
	}

	public synchronized void removeMapChangeListener(
			IMapChangeListener<? super K, ? super V> listener) {
		removeListener(MapChangeEvent.TYPE, listener);
	}

	public Object getKeyType() {
		return decorated.getKeyType();
	}

	public Object getValueType() {
		return decorated.getValueType();
	}

	protected void fireMapChange(MapDiff<K, V> diff) {
		// fire general change event first
		super.fireChange();
		fireEvent(new MapChangeEvent<K, V>(this, diff));
	}

	protected void fireChange() {
		throw new RuntimeException(
				"fireChange should not be called, use fireListChange() instead"); //$NON-NLS-1$
	}

	protected void firstListenerAdded() {
		if (mapChangeListener == null) {
			mapChangeListener = new IMapChangeListener<K, V>() {
				public void handleMapChange(MapChangeEvent<K, V> event) {
					DecoratingObservableMap.this.handleMapChange(event);
				}
			};
		}
		decorated.addMapChangeListener(mapChangeListener);
		super.firstListenerAdded();
	}

	protected void lastListenerRemoved() {
		super.lastListenerRemoved();
		if (mapChangeListener != null) {
			decorated.removeMapChangeListener(mapChangeListener);
			mapChangeListener = null;
		}
	}

	/**
	 * Called whenever a MapChangeEvent is received from the decorated
	 * observable. By default, this method fires the map change event again,
	 * with the decorating observable as the event source. Subclasses may
	 * override to provide different behavior.
	 * 
	 * @param event
	 *            the change event received from the decorated observable
	 */
	protected void handleMapChange(final MapChangeEvent<K, V> event) {
		fireMapChange(event.diff);
	}

	public void clear() {
		checkRealm();
		decorated.clear();
	}

	public boolean containsKey(Object key) {
		getterCalled();
		return decorated.containsKey(key);
	}

	public boolean containsValue(Object value) {
		getterCalled();
		return decorated.containsValue(value);
	}

	private class BackedCollection<E> implements Collection<E> {
		private Collection<E> collection;

		BackedCollection(Collection<E> set) {
			this.collection = set;
		}

		public boolean add(E o) {
			throw new UnsupportedOperationException();
		}

		public boolean addAll(Collection<? extends E> arg0) {
			throw new UnsupportedOperationException();
		}

		public void clear() {
			checkRealm();
			collection.clear();
		}

		public boolean contains(Object o) {
			getterCalled();
			return collection.contains(o);
		}

		public boolean containsAll(Collection<?> c) {
			getterCalled();
			return collection.containsAll(c);
		}

		public boolean isEmpty() {
			getterCalled();
			return collection.isEmpty();
		}

		public Iterator<E> iterator() {
			final Iterator<E> iterator = collection.iterator();
			return new Iterator<E>() {
				public boolean hasNext() {
					getterCalled();
					return iterator.hasNext();
				}

				public E next() {
					getterCalled();
					return iterator.next();
				}

				public void remove() {
					checkRealm();
					iterator.remove();
				}
			};
		}

		public boolean remove(Object o) {
			getterCalled();
			return collection.remove(o);
		}

		public boolean removeAll(Collection<?> c) {
			getterCalled();
			return collection.removeAll(c);
		}

		public boolean retainAll(Collection<?> c) {
			getterCalled();
			return collection.retainAll(c);
		}

		public int size() {
			getterCalled();
			return collection.size();
		}

		public Object[] toArray() {
			getterCalled();
			return collection.toArray();
		}

		public <T> T[] toArray(T[] array) {
			getterCalled();
			return collection.toArray(array);
		}

		public boolean equals(Object obj) {
			getterCalled();
			return collection.equals(obj);
		}

		public int hashCode() {
			getterCalled();
			return collection.hashCode();
		}

		public String toString() {
			getterCalled();
			return collection.toString();
		}
	}

	private class BackedSet<E> extends BackedCollection<E> implements Set<E> {
		BackedSet(Set<E> set) {
			super(set);
		}
	}

	Set<Map.Entry<K, V>> entrySet = null;

	public Set<Map.Entry<K, V>> entrySet() {
		getterCalled();
		if (entrySet == null) {
			entrySet = new BackedSet<Map.Entry<K, V>>(decorated.entrySet());
		}
		return entrySet;
	}

	public V get(Object key) {
		getterCalled();
		return decorated.get(key);
	}

	public boolean isEmpty() {
		getterCalled();
		return decorated.isEmpty();
	}

	Set<K> keySet = null;

	public Set<K> keySet() {
		getterCalled();
		if (keySet == null) {
			keySet = new BackedSet<K>(decorated.keySet());
		}
		return keySet;
	}

	public V put(K key, V value) {
		checkRealm();
		return decorated.put(key, value);
	}

	public void putAll(Map<? extends K, ? extends V> m) {
		checkRealm();
		decorated.putAll(m);
	}

	public V remove(Object key) {
		checkRealm();
		return decorated.remove(key);
	}

	public int size() {
		getterCalled();
		return decorated.size();
	}

	Collection<V> values;

	public Collection<V> values() {
		getterCalled();
		if (values == null) {
			values = new BackedCollection<V>(decorated.values());
		}
		return values;
	}

	public boolean equals(Object obj) {
		getterCalled();
		if (this == obj) {
			return true;
		}
		return decorated.equals(obj);
	}

	public int hashCode() {
		getterCalled();
		return decorated.hashCode();
	}

	public String toString() {
		getterCalled();
		return decorated.toString();
	}

	public synchronized void dispose() {
		if (decorated != null && mapChangeListener != null) {
			decorated.removeMapChangeListener(mapChangeListener);
		}
		decorated = null;
		mapChangeListener = null;
		super.dispose();
	}
}
