/*******************************************************************************
 * Copyright (c) 2008, 2009 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 215531)
 *     Matthew Hall - bug 230267
 *         (through ObservableViewerElementSet.java)
 *     Matthew Hall - bug 262269
 ******************************************************************************/

package org.eclipse.core.internal.databinding.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.databinding.observable.set.AbstractObservableSet;
import org.eclipse.core.databinding.observable.set.IObservableSet;

/**
 * An {@link IObservableSet} of elements where elements are added, removed and
 * compared by identity. Elements of the set are compared using <code>==</code>
 * instead of {@link #equals(Object)}.
 * <p>
 * This class is <i>not</i> a strict implementation the {@link IObservableSet}
 * interface. It intentionally violates the {@link Set} contract, which requires
 * the use of {@link #equals(Object)} when comparing elements.
 * 
 * @since 1.2
 */
public class IdentityObservableSet<E> extends AbstractObservableSet<E> {
	private Set<E> wrappedSet;
	private Object elementType;

	/**
	 * Constructs an IdentityObservableSet on the given {@link Realm}.
	 * 
	 * @param realm
	 *            the realm of the constructed set.
	 * @param elementType
	 *            the element type of the constructed set.
	 */
	public IdentityObservableSet(Realm realm, Object elementType) {
		super(realm);

		this.wrappedSet = new IdentitySet<E>();
		this.elementType = elementType;
	}

	protected Set<E> getWrappedSet() {
		return wrappedSet;
	}

	public Object getElementType() {
		return elementType;
	}

	public Iterator<E> iterator() {
		getterCalled();
		final Iterator<E> wrappedIterator = wrappedSet.iterator();
		return new Iterator<E>() {
			E last;

			public boolean hasNext() {
				getterCalled();
				return wrappedIterator.hasNext();
			}

			public E next() {
				getterCalled();
				return last = wrappedIterator.next();
			}

			public void remove() {
				getterCalled();
				wrappedIterator.remove();
				fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(),
						Collections.singleton(last)));
			}
		};
	}

	public boolean add(E o) {
		getterCalled();
		boolean changed = wrappedSet.add(o);
		if (changed)
			fireSetChange(Diffs.createSetDiff(Collections.singleton(o),
					Collections.<E> emptySet()));
		return changed;
	}

	public boolean addAll(Collection<? extends E> c) {
		getterCalled();
		Set<E> additions = new IdentitySet<E>();
		for (Iterator<? extends E> iterator = c.iterator(); iterator.hasNext();) {
			E element = iterator.next();
			if (wrappedSet.add(element))
				additions.add(element);
		}
		boolean changed = !additions.isEmpty();
		if (changed)
			fireSetChange(Diffs.createSetDiff(additions,
					Collections.<E> emptySet()));
		return changed;
	}
	
	@SuppressWarnings("unchecked")
	public boolean remove(Object o) {
		getterCalled();
		boolean changed = wrappedSet.remove(o);
		if (changed)
			fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(),
					Collections.singleton((E) o)));
		return changed;
	}

	@SuppressWarnings("unchecked")
	public boolean removeAll(Collection<?> c) {
		getterCalled();
		Set<E> removals = new IdentitySet<E>();
		for (Iterator<?> iterator = c.iterator(); iterator.hasNext();) {
			Object element = iterator.next();
			if (wrappedSet.remove(element))
				removals.add((E) element);
		}
		boolean changed = !removals.isEmpty();
		if (changed)
			fireSetChange(Diffs.createSetDiff(Collections.EMPTY_SET, removals));
		return changed;
	}

	@SuppressWarnings("unchecked")
	public boolean retainAll(Collection<?> c) {
		getterCalled();
		Set<E> removals = new IdentitySet<E>();
		Object[] toRetain = c.toArray();
		outer: for (Iterator<?> iterator = wrappedSet.iterator(); iterator
				.hasNext();) {
			Object element = iterator.next();
			// Cannot rely on c.contains(element) because we must compare
			// elements using IElementComparer.
			for (int i = 0; i < toRetain.length; i++) {
				if (element == toRetain[i])
					continue outer;
			}
			iterator.remove();
			removals.add((E) element);
		}
		boolean changed = !removals.isEmpty();
		if (changed)
			fireSetChange(Diffs.createSetDiff(Collections.EMPTY_SET, removals));
		return changed;
	}

	public void clear() {
		getterCalled();
		if (!wrappedSet.isEmpty()) {
			Set<E> removals = wrappedSet;
			wrappedSet = new IdentitySet<E>();
			fireSetChange(Diffs.createSetDiff(Collections.<E> emptySet(),
					removals));
		}
	}
}