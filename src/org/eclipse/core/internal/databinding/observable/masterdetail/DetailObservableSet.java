/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Matthew Hall - bug 221351, 247875, 246782, 249526, 268022, 251424
 *     Ovidio Mallo - bug 241318
 *******************************************************************************/
package org.eclipse.core.internal.databinding.observable.masterdetail;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.DisposeEvent;
import org.eclipse.core.databinding.observable.IDisposeListener;
import org.eclipse.core.databinding.observable.IObserving;
import org.eclipse.core.databinding.observable.ObservableTracker;
import org.eclipse.core.databinding.observable.masterdetail.IObservableFactory;
import org.eclipse.core.databinding.observable.set.IObservableSet;
import org.eclipse.core.databinding.observable.set.ISetChangeListener;
import org.eclipse.core.databinding.observable.set.ObservableSet;
import org.eclipse.core.databinding.observable.set.SetChangeEvent;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.Assert;

/**
 * @since 3.2
 * 
 */
public class DetailObservableSet<U, V> extends ObservableSet<V> implements
IObserving {

	private boolean updating = false;

	private ISetChangeListener<V> innerChangeListener = new ISetChangeListener<V>() {
		public void handleSetChange(SetChangeEvent<V> event) {
			if (!updating) {
				fireSetChange(event.diff);
			}
		}
	};

	private U currentOuterValue;

	private IObservableSet<V> innerObservableSet;

	private IObservableValue<U> outerObservableValue;

	private IObservableFactory<IObservableSet<V>, ? super U> factory;

	/**
	 * @param factory
	 * @param outerObservableValue
	 * @param detailType
	 */
	public DetailObservableSet(
			IObservableFactory<IObservableSet<V>, ? super U> factory,
			IObservableValue<U> outerObservableValue, Object detailType) {
		super(outerObservableValue.getRealm(), Collections.<V>emptySet(),
				detailType);
		Assert.isTrue(!outerObservableValue.isDisposed(),
				"Master observable is disposed"); //$NON-NLS-1$

		this.factory = factory;
		this.outerObservableValue = outerObservableValue;

		outerObservableValue.addDisposeListener(new IDisposeListener() {
			public void handleDispose(DisposeEvent staleEvent) {
				dispose();
			}
		});

		ObservableTracker.setIgnore(true);
		try {
			updateInnerObservableSet();
		} finally {
			ObservableTracker.setIgnore(false);
		}
		outerObservableValue.addValueChangeListener(outerChangeListener);
	}

	IValueChangeListener<U> outerChangeListener = new IValueChangeListener<U>() {
		public void handleValueChange(ValueChangeEvent<U> event) {
			if (isDisposed())
				return;
			ObservableTracker.setIgnore(true);
			try {
				Set<V> oldSet = new HashSet<V>(wrappedSet);
				updateInnerObservableSet();
				fireSetChange(Diffs.computeSetDiff(oldSet, wrappedSet));
			} finally {
				ObservableTracker.setIgnore(false);
			}
		}
	};

	private void updateInnerObservableSet() {
		currentOuterValue = outerObservableValue.getValue();
		if (innerObservableSet != null) {
			innerObservableSet.removeSetChangeListener(innerChangeListener);
			innerObservableSet.dispose();
		}
		if (currentOuterValue == null) {
			innerObservableSet = null;
			wrappedSet = Collections.emptySet();
		} else {
			ObservableTracker.setIgnore(true);
			try {
				innerObservableSet = factory
						.createObservable(currentOuterValue);
			} finally {
				ObservableTracker.setIgnore(false);
			}
			DetailObservableHelper.warnIfDifferentRealms(getRealm(),
					innerObservableSet.getRealm());
			wrappedSet = innerObservableSet;

			if (elementType != null) {
				Object innerValueType = innerObservableSet.getElementType();

				Assert.isTrue(elementType.equals(innerValueType),
						"Cannot change value type in a nested observable set"); //$NON-NLS-1$
			}

			innerObservableSet.addSetChangeListener(innerChangeListener);
		}
	}

	public boolean add(final V o) {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			return wrappedSet.add(o);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public boolean remove(final Object o) {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			return wrappedSet.remove(o);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public boolean addAll(final Collection<? extends V> c) {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			return wrappedSet.addAll(c);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public boolean removeAll(final Collection<?> c) {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			return wrappedSet.removeAll(c);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public boolean retainAll(final Collection<?> c) {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			return wrappedSet.retainAll(c);
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public void clear() {
		getterCalled();
		ObservableTracker.setIgnore(true);
		try {
			wrappedSet.clear();
		} finally {
			ObservableTracker.setIgnore(false);
		}
	}

	public synchronized void dispose() {
		super.dispose();

		if (outerObservableValue != null) {
			outerObservableValue.removeValueChangeListener(outerChangeListener);
		}
		if (innerObservableSet != null) {
			innerObservableSet.removeSetChangeListener(innerChangeListener);
			innerObservableSet.dispose();
		}
		outerObservableValue = null;
		outerChangeListener = null;
		currentOuterValue = null;
		factory = null;
		innerObservableSet = null;
		innerChangeListener = null;
	}

	public Object getObserved() {
		if (innerObservableSet instanceof IObserving) {
			return ((IObserving) innerObservableSet).getObserved();
		}
		return null;
	}

}
