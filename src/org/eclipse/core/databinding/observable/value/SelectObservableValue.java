/*******************************************************************************
 * Copyright (c) 2008, 2009 Matthew Hall and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Matthew Hall - initial API and implementation (bug 249992)
 ******************************************************************************/

package org.eclipse.core.databinding.observable.value;

import org.eclipse.core.databinding.observable.Diffs;
import org.eclipse.core.databinding.observable.Realm;
import org.eclipse.core.internal.databinding.observable.Util;

/**
 * An observable value which behaves similarly to the &lt;select&gt; and
 * &lt;option&gt; HTML tags. A SelectObservableValue has a number of options
 * added to it via the {@link #addOption(Object, IObservableValue)} method. The
 * value of the SelectObservableValue is the value of whichever option's
 * observable has a value of Boolean.TRUE, or null if none of the observable's
 * values are Boolean.TRUE.
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * 
 * @since 1.2
 */
public class SelectObservableValue<T> extends AbstractObservableValue<T> {
	private static class Option<T> {

		@SuppressWarnings("unchecked")
		public static <T> Option<T>[] newArray(int size) {
			return new Option[size];
		}

		private final T value;
		private final IObservableValue<Boolean> observable;
		
		public Option(T value, IObservableValue<Boolean> observable) {
			this.value = value;
			this.observable = observable;
		}
	}

	private final Object valueType;

	private Option<T>[] options;
	private int selectionIndex = -1; // n/a while not hasListeners()

	private boolean updating = false;

	private IValueChangeListener<Boolean> listener = new IValueChangeListener<Boolean>() {
		public void handleValueChange(ValueChangeEvent<Boolean> event) {
			if (!updating) {
				IObservableValue<Boolean> observable = event
						.getObservableValue();
				if (Boolean.TRUE.equals(observable.getValue())) {
					notifyIfChanged(indexOfObservable(observable));
				}
			}
		}
	};

	/**
	 * Constructs a SelectObservableValue on the default realm.
	 */
	public SelectObservableValue() {
		this(Realm.getDefault(), null);
	}

	/**
	 * Constructs a SelectObservableValue on the specified realm.
	 * 
	 * @param realm
	 *            the realm
	 */
	public SelectObservableValue(Realm realm) {
		this(realm, null);
	}

	/**
	 * Constructs a SelectObservableValue on the default realm, with the given
	 * value type.
	 * 
	 * @param valueType
	 *            the value type
	 */
	public SelectObservableValue(Object valueType) {
		this(Realm.getDefault(), valueType);
	}

	/**
	 * Constructs a SelectObservableValue on the given realm, with the given
	 * value type.
	 * 
	 * @param realm
	 *            the realm
	 * @param valueType
	 *            the value type
	 */
	public SelectObservableValue(Realm realm, Object valueType) {
		super(realm);
		this.valueType = valueType;
		this.options = Option.newArray(0);
	}

	protected void firstListenerAdded() {
		super.firstListenerAdded();
		selectionIndex = indexOfValue(getLiveValue());
		for (int i = 0; i < options.length; i++) {
			options[i].observable.addValueChangeListener(listener);
		}
	}

	protected void lastListenerRemoved() {
		for (int i = 0; i < options.length; i++) {
			options[i].observable.removeValueChangeListener(listener);
		}
		selectionIndex = -1;
		super.lastListenerRemoved();
	}

	public Object getValueType() {
		return valueType;
	}

	private void notifyIfChanged(int index) {
		if (hasListeners() && selectionIndex != index) {
			T oldValue = valueAtIndex(selectionIndex);
			T newValue = valueAtIndex(index);
			selectionIndex = index;
			fireValueChange(Diffs.createValueDiff(oldValue, newValue));
		}
	}

	/**
	 * Adds an option to this SelectObservableValue. If the observable contains
	 * Boolean.TRUE then the selection changes immediately to the given value.
	 * 
	 * @param value
	 *            The value associated with the provided observable
	 * @param observable
	 *            an observable of value type Boolean.class or Boolean.TYPE
	 */
	public void addOption(T value, IObservableValue<Boolean> observable) {
		checkRealm();

		Option<T> option = new Option<T>(value, observable);
		addOption(option);

		if (hasListeners()) {
			observable.addValueChangeListener(listener);
			if (Boolean.TRUE.equals(observable.getValue())) {
				notifyIfChanged(indexOfObservable(observable));
			}
		}
	}

	private void addOption(Option<T> option) {
		Option<T>[] newOptions = Option.newArray(options.length + 1);
		System.arraycopy(options, 0, newOptions, 0, options.length);
		newOptions[options.length] = option;
		options = newOptions;
	}

	protected T doGetValue() {
		return hasListeners() ? valueAtIndex(selectionIndex) : getLiveValue();
	}

	private T getLiveValue() {
		for (int i = 0; i < options.length; i++) {
			if (Boolean.TRUE.equals(options[i].observable.getValue()))
				return options[i].value;
		}
		return null;
	}

	protected void doSetValue(T value) {
		int index = indexOfValue(value);

		try {
			updating = true;
			for (int i = 0; i < options.length; i++) {
				options[i].observable.setValue(i == index ? Boolean.TRUE
						: Boolean.FALSE);
			}
		} finally {
			updating = false;
		}

		notifyIfChanged(index);
	}

	private T valueAtIndex(int index) {
		if (index == -1)
			return null;
		return options[index].value;
	}

	private int indexOfValue(Object value) {
		for (int i = 0; i < options.length; i++)
			if (Util.equals(options[i].value, value))
				return i;
		return -1;
	}

	private int indexOfObservable(IObservableValue<Boolean> observable) {
		for (int i = 0; i < options.length; i++)
			if (options[i].observable == observable)
				return i;
		return -1;
	}
}
