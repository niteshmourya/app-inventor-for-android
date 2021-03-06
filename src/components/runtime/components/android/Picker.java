// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android;

import com.google.devtools.simple.runtime.annotations.SimpleEvent;
import com.google.devtools.simple.runtime.annotations.SimpleFunction;
import com.google.devtools.simple.runtime.annotations.SimpleObject;
import com.google.devtools.simple.runtime.events.EventDispatcher;

import android.content.Intent;

/**
 * Abstract superclass for all of the "Picker" components.
 *
 */
@SimpleObject
public abstract class Picker extends ButtonBase implements ActivityResultListener {
  protected final ComponentContainer container;

  /* Used to identify the call to startActivityForResult. Will be passed back into the
  resultReturned() callback method. */
  protected int requestCode;

  public Picker(ComponentContainer container) {
    super(container);
    this.container = container;
  }

  /**
   *  Provides the Intent used to launch the picker activity.
   */
  protected abstract Intent getIntent();

  @Override
  public void click() {
    BeforePicking();
    if (requestCode == 0) { // only need to register once
      requestCode = container.$form().registerForActivityResult(this);
    }
    container.$context().startActivityForResult(getIntent(), requestCode);
  }

  // Functions

  /**
   * Opens the picker, as though the user clicked on it.
   */
  @SimpleFunction
  public void Open() {
    click();
  }

  // Events

  /**
   * Simple event to raise when the component is clicked but before the
   * picker activity is started.
   */
  @SimpleEvent
  public void BeforePicking() {
    EventDispatcher.dispatchEvent(this, "BeforePicking");
  }

  /**
   * Simple event to be raised after the picker activity returns its
   * result and the properties have been filled in.
   */
  @SimpleEvent
  public void AfterPicking() {
    EventDispatcher.dispatchEvent(this, "AfterPicking");
  }
}
