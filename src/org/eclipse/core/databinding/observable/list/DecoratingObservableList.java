/*******************************************************************************
 * Copyright (c) 2008 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 237718)
 *     Matthew Hall - but 246626
 *******************************************************************************/

package org.eclipse.core.databinding.observable.list;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import org.eclipse.core.databinding.observable.DecoratingObservableCollection;

/**
 * An observable list which decorates another observable list.
 * 
 * @since 1.2
 */
public class DecoratingObservableList<E> extends DecoratingObservableCollection<E> implements IObservableList<E> {

	private IObservableList<E> decorated;

	private IListChangeListener<? super E> listChangeListener;

	/**
	 * Constructs a DecoratingObservableList which decorates the given
	 * observable.
	 * 
	 * @param decorated
	 *            the observable list being decorated
	 * @param disposeDecoratedOnDispose
	 */
	public DecoratingObservableList(IObservableList<E> decorated,
			boolean disposeDecoratedOnDispose) {
		super(decorated, disposeDecoratedOnDispose);
		this.decorated = decorated;
	}

	public synchronized void addListChangeListener(
			IListChangeListener<? super E> listener) {
		addListener(ListChangeEvent.TYPE, listener);
	}

	public synchronized void removeListChangeListener(
			IListChangeListener<? super E> listener) {
		removeListener(ListChangeEvent.TYPE, listener);
	}

	protected void fireListChange(ListDiff<E> diff) {
		// fire general change event first
		super.fireChange();
		fireEvent(new ListChangeEvent<E>(this, diff));
	}

	protected void fireChange() {
		throw new RuntimeException(
				"fireChange should not be called, use fireListChange() instead"); //$NON-NLS-1$
	}

	protected void firstListenerAdded() {
		if (listChangeListener == null) {
			listChangeListener = new IListChangeListener<E>() {
				public void handleListChange(ListChangeEvent<E> event) {
					DecoratingObservableList.this.handleListChange(event);
				}
			};
		}
		decorated.addListChangeListener(listChangeListener);
		super.firstListenerAdded();
	}

	protected void lastListenerRemoved() {
		super.lastListenerRemoved();
		if (listChangeListener != null) {
			decorated.removeListChangeListener(listChangeListener);
			listChangeListener = null;
		}
	}

	/**
	 * Called whenever a ListChangeEvent is received from the decorated
	 * observable. By default, this method fires the list change event again,
	 * with the decorating observable as the event source. Subclasses may
	 * override to provide different behavior.
	 * 
	 * @param event
	 *            the change event received from the decorated observable
	 */
	protected void handleListChange(final ListChangeEvent<E> event) {
		fireListChange(event.diff);
	}

	public void add(int index, E o) {
		checkRealm();
		decorated.add(index, o);
	}

	public boolean addAll(int index, Collection<? extends E> c) {
		checkRealm();
		return decorated.addAll(index, c);
	}

	public E get(int index) {
		getterCalled();
		return decorated.get(index);
	}

	public int indexOf(Object o) {
		getterCalled();
		return decorated.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		getterCalled();
		return decorated.lastIndexOf(o);
	}

	public ListIterator<E> listIterator() {
		return listIterator(0);
	}

	public ListIterator<E> listIterator(int index) {
		getterCalled();
		final ListIterator<E> iterator = decorated.listIterator(index);
		return new ListIterator<E>() {

			public void add(E o) {
				iterator.add(o);
			}

			public boolean hasNext() {
				getterCalled();
				return iterator.hasNext();
			}

			public boolean hasPrevious() {
				getterCalled();
				return iterator.hasPrevious();
			}

			public E next() {
				getterCalled();
				return iterator.next();
			}

			public int nextIndex() {
				getterCalled();
				return iterator.nextIndex();
			}

			public E previous() {
				getterCalled();
				return iterator.previous();
			}

			public int previousIndex() {
				getterCalled();
				return iterator.previousIndex();
			}

			public void remove() {
				checkRealm();
				iterator.remove();
			}

			public void set(E o) {
				checkRealm();
				iterator.set(o);
			}
		};
	}

	public E move(int oldIndex, int newIndex) {
		checkRealm();
		return decorated.move(oldIndex, newIndex);
	}

	public E remove(int index) {
		checkRealm();
		return decorated.remove(index);
	}

	public E set(int index, E element) {
		checkRealm();
		return decorated.set(index, element);
	}

	public List<E> subList(int fromIndex, int toIndex) {
		getterCalled();
		return decorated.subList(fromIndex, toIndex);
	}

	public synchronized void dispose() {
		if (decorated != null && listChangeListener != null) {
			decorated.removeListChangeListener(listChangeListener);
		}
		decorated = null;
		listChangeListener = null;
		super.dispose();
	}
}
