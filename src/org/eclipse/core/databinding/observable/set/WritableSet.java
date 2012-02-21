/*******************************************************************************
 * Copyright (c) 2006-2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Brad Reynolds - bug 147515
 *     Matthew Hall - bug 221351
 *******************************************************************************/

package org.eclipse.core.databinding.observable.set;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.Realm;

/**
 * Mutable (writable) implementation of {@link IObservableSet}.
 * 
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @since 1.0
 */
public class WritableSet<E> extends ObservableSet<E> {

	/**
	 * Constructs a new empty instance in the default realm with a
	 * <code>null</code> element type.
	 * 
	 */
	public WritableSet() {
		this(Realm.getDefault());
	}

	/**
	 * Constructs a new instance in the default realm containing the
	 * elements of the given collection. Changes to the given collection after
	 * calling this method do not affect the contents of the created WritableSet.
	 * 
	 * @param c
	 * @param elementType
	 *            can be <code>null</code>
	 */
	public WritableSet(Collection<? extends E> c, Object elementType) {
		this(Realm.getDefault(), new HashSet<E>(c), elementType);
	}

	/**
	 * Constructs a new empty instance in the given realm and a
	 * <code>null</code> element type.
	 * 
	 * @param realm
	 */
	public WritableSet(Realm realm) {
		this(realm, new HashSet<E>(), null);
	}

	/**
	 * Constructs a new instance in the default realm with the given element
	 * type, containing the elements of the given collection. Changes to the
	 * given collection after calling this method do not affect the contents of
	 * the created WritableSet.
	 * 
	 * @param realm
	 * @param c
	 * @param elementType
	 *            can be <code>null</code>
	 */
	public WritableSet(Realm realm, Collection<? extends E> c,
			Object elementType) {
		super(realm, new HashSet<E>(c), elementType);
		this.elementType = elementType;
	}

	public boolean add(E o) {
		getterCalled();
		boolean added = wrappedSet.add(o);
		if (added) {
			fireSetChange(Diffs.createSetDiff(Collections.singleton(o), Collections.<E> emptySet()));
		}
		return added;
	}

	public boolean addAll(Collection<? extends E> c) {
		getterCalled();
		Set<E> additions = new HashSet<E>();
		Iterator<? extends E> it = c.iterator();
		while (it.hasNext()) {
			E element = it.next();
			if (wrappedSet.add(element)) {
				additions.add(element);
			}
		}
		if (additions.size() > 0) {
			fireSetChange(Diffs.createSetDiff(additions, Collections.<E> emptySet()));
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		getterCalled();
		boolean removed = wrappedSet.remove(o);
		if (removed) {
			fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(),
					Collections.singleton((E) o)));
		}
		return removed;
	}

	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection<?> c) {
		getterCalled();
		Set<E> removes = new HashSet<E>();
		Iterator<?> it = c.iterator();
		while (it.hasNext()) {
			Object element = it.next();
			if (wrappedSet.remove(element)) {
				removes.add((E) element);
			}
		}
		if (removes.size() > 0) {
			fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(), removes));
			return true;
		}
		return false;
	}

	public boolean retainAll(Collection<?> c) {
		getterCalled();
		Set<E> removes = new HashSet<E>();
		Iterator<E> it = wrappedSet.iterator();
		while (it.hasNext()) {
			E element = it.next();
			if (!c.contains(element)) {
				it.remove();
				removes.add(element);
			}
		}
		if (removes.size() > 0) {
			fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(),
					removes));
			return true;
		}
		return false;
	}

	public void clear() {
		getterCalled();
		Set<E> removes = new HashSet<E>(wrappedSet);
		wrappedSet.clear();
		fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(), removes));
	}

	/**
	 * @param elementType
	 *            can be <code>null</code>
	 * @return new instance with the default realm
	 */
	public static <E> WritableSet<E> withElementType(Object elementType) {
		return new WritableSet<E>(Realm.getDefault(), new HashSet<E>(),
				elementType);
	}
}
