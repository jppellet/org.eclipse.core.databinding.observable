/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.core.databinding.observable.map;

import org.eclipse.core.databinding.observable.IObservablesListener;
import org.eclipse.core.databinding.observable.ObservableEvent;

/**
 * Map change event describing an incremental change of an
 * {@link IObservableMap} object.
 * 
 * @since 1.0
 * 
 */
public class MapChangeEvent<K, V> extends ObservableEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8092347212410548463L;
	static final Object TYPE = new Object();

	/**
	 * Description of the change to the source observable map. Listeners must
	 * not change this field.
	 */
	public MapDiff<K, V> diff;

	/**
	 * Creates a new map change event
	 * 
	 * @param source
	 *            the source observable map
	 * @param diff
	 *            the map change
	 */
	public MapChangeEvent(IObservableMap<K, V> source, MapDiff<K, V> diff) {
		super(source);
		this.diff = diff;
	}

	/**
	 * Returns the observable map from which this event originated.
	 * 
	 * @return the observable map from which this event originated
	 */
	@SuppressWarnings("unchecked")
	// always safe, same object as in constructor
	public IObservableMap<K, V> getObservableMap() {
		return (IObservableMap<K, V>) getSource();
	}

	@SuppressWarnings("unchecked")
	// TODO check safety
	protected void dispatch(IObservablesListener listener) {
		((IMapChangeListener<K, V>) listener).handleMapChange(this);
	}

	protected Object getListenerType() {
		return TYPE;
	}

}
