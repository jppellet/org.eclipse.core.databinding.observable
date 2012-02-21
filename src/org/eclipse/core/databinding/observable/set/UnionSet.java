/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matthew Hall - bugs 208332, 265727
 *******************************************************************************/

package org.eclipse.core.databinding.observable.set;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.internal.databinding.observable.IStalenessConsumer;
import org.eclipse.core.internal.databinding.observable.StalenessTracker;

/**
 * Represents a set consisting of the union of elements from one or more other
 * sets. This object does not need to be explicitly disposed. If nobody is
 * listening to the UnionSet, the set will remove its listeners.
 * 
 * <p>
 * This class is thread safe. All state accessing methods must be invoked from
 * the {@link Realm#isCurrent() current realm}. Methods for adding and removing
 * listeners may be invoked from any thread.
 * </p>
 * 
 * @since 1.0
 */
public final class UnionSet<E> extends ObservableSet<E> {

	@SuppressWarnings("unchecked")
	public static <E> IObservableSet<E>[] newArray(int size) {
		return new IObservableSet[size];
	}
	
	/**
	 * child sets
	 */
	private IObservableSet<E>[] childSets;

	private boolean stale = false;

	/**
	 * Map of elements onto Integer reference counts. This map is constructed
	 * when the first listener is added to the union set. Null if nobody is
	 * listening to the UnionSet.
	 */
	private HashMap<E, Integer> refCounts = null;

	private StalenessTracker stalenessTracker;

	/**
	 * @param childSets
	 */
	public UnionSet(IObservableSet<E>[] childSets) {
		this(childSets, childSets[0].getElementType());
	}

	/**
	 * @param childSets
	 * @param elementType
	 * @since 1.2
	 */
	public UnionSet(IObservableSet<E>[] childSets, Object elementType) {
		super(childSets[0].getRealm(), null, elementType);
		System.arraycopy(childSets, 0,
				this.childSets =  newArray(childSets.length), 0,
				childSets.length);
		this.stalenessTracker = new StalenessTracker(childSets,
				stalenessConsumer);
	}

	private ISetChangeListener<E> childSetChangeListener = new ISetChangeListener<E>() {
		public void handleSetChange(SetChangeEvent<E> event) {
			processAddsAndRemoves(event.diff.getAdditions(), event.diff
					.getRemovals());
		}
	};

	private IStalenessConsumer stalenessConsumer = new IStalenessConsumer() {
		public void setStale(boolean stale) {
			boolean oldStale = UnionSet.this.stale;
			UnionSet.this.stale = stale;
			if (stale && !oldStale) {
				fireStale();
			}
		}
	};

	public boolean isStale() {
		getterCalled();
		if (refCounts != null) {
			return stale;
		}

		for (int i = 0; i < childSets.length; i++) {
			IObservableSet<E> childSet = childSets[i];

			if (childSet.isStale()) {
				return true;
			}
		}
		return false;
	}

	private void processAddsAndRemoves(Set<E> adds, Set<E> removes) {
		Set<E> addsToFire = new HashSet<E>();
		Set<E> removesToFire = new HashSet<E>();

		for (Iterator<E> iter = adds.iterator(); iter.hasNext();) {
			E added = iter.next();

			Integer refCount = refCounts.get(added);
			if (refCount == null) {
				refCounts.put(added, new Integer(1));
				addsToFire.add(added);
			} else {
				int refs = refCount.intValue();
				refCount = new Integer(refs + 1);
				refCounts.put(added, refCount);
			}
		}

		for (Iterator<E> iter = removes.iterator(); iter.hasNext();) {
			E removed = iter.next();

			Integer refCount = refCounts.get(removed);
			if (refCount != null) {
				int refs = refCount.intValue();
				if (refs <= 1) {
					removesToFire.add(removed);
					refCounts.remove(removed);
				} else {
					refCount = new Integer(refCount.intValue() - 1);
					refCounts.put(removed, refCount);
				}
			}
		}

		// just in case the removes overlapped with the adds
		addsToFire.removeAll(removesToFire);

		if (addsToFire.size() > 0 || removesToFire.size() > 0) {
			fireSetChange(Diffs.createSetDiff(addsToFire, removesToFire));
		}
	}

	protected void firstListenerAdded() {
		super.firstListenerAdded();

		refCounts = new HashMap<E, Integer>();
		for (int i = 0; i < childSets.length; i++) {
			IObservableSet<E> next = childSets[i];
			next.addSetChangeListener(childSetChangeListener);
			incrementRefCounts(next);
		}
		stalenessTracker = new StalenessTracker(childSets, stalenessConsumer);
		setWrappedSet(refCounts.keySet());
	}

	protected void lastListenerRemoved() {
		super.lastListenerRemoved();

		for (int i = 0; i < childSets.length; i++) {
			IObservableSet<E> next = childSets[i];

			next.removeSetChangeListener(childSetChangeListener);
			stalenessTracker.removeObservable(next);
		}
		refCounts = null;
		stalenessTracker = null;
		setWrappedSet(null);
	}

	private ArrayList<E> incrementRefCounts(Collection<E> added) {
		ArrayList<E> adds = new ArrayList<E>();

		for (Iterator<E> iter = added.iterator(); iter.hasNext();) {
			E next = iter.next();

			Integer refCount = refCounts.get(next);
			if (refCount == null) {
				adds.add(next);
				refCount = new Integer(1);
				refCounts.put(next, refCount);
			} else {
				refCount = new Integer(refCount.intValue() + 1);
				refCounts.put(next, refCount);
			}
		}
		return adds;
	}

	protected void getterCalled() {
		super.getterCalled();
		if (refCounts == null) {
			// no listeners, recompute
			setWrappedSet(computeElements());
		}
	}

	private Set<E> computeElements() {
		// If there is no cached value, compute the union from scratch
		if (refCounts == null) {
			Set<E> result = new HashSet<E>();
			for (int i = 0; i < childSets.length; i++) {
				result.addAll(childSets[i]);
			}
			return result;
		}

		// Else there is a cached value. Return it.
		return refCounts.keySet();
	}

}
