// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components;

/**
 * Interface indicating that this object can handle event dispatching.
 *
 */

public interface HandlesEventDispatching {
  public boolean canDispatchEvent(Component component, String eventName);

  public boolean dispatchEvent(Component component, String componentName, String eventName,
      Object[] args);
}
