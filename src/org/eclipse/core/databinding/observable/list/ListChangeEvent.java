/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.core.databinding.observable.list;

import org.eclipse.core.databinding.observable.IObservablesListener;
import org.eclipse.core.databinding.observable.ObservableEvent;

/**
 * List change event describing an incremental change of an
 * {@link IObservableList} object.
 * 
 * @since 1.0
 */
public class ListChangeEvent<E> extends ObservableEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9154315534258776672L;

	static final Object TYPE = new Object();

	/**
	 * Description of the change to the source observable list. Listeners must
	 * not change this field.
	 */
	public ListDiff<E> diff;

	/**
	 * Creates a new list change event.
	 * 
	 * @param source
	 *            the source observable list
	 * @param diff
	 *            the list change
	 */
	public ListChangeEvent(IObservableList<E> source, ListDiff<E> diff) {
		super(source);
		this.diff = diff;
	}

	/**
	 * Returns the observable list from which this event originated.
	 * 
	 * @return the observable list from which this event originated
	 */
	@SuppressWarnings("unchecked")
	// safe, same object as in typed constructor
	public IObservableList<E> getObservableList() {
		return (IObservableList<E>) getSource();
	}

	@SuppressWarnings("unchecked")
	// TODO check safety
	protected void dispatch(IObservablesListener listener) {
		((IListChangeListener<E>) listener).handleListChange(this);
	}

	protected Object getListenerType() {
		return TYPE;
	}

}
