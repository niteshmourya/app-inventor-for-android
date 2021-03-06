// Copyright 2010 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android.util;

import android.os.Build;

/**
 * Support for discovering which version of the Android SDK is on the phone.
 *
 * For more information about Android API levels see
 * http://developer.android.com/guide/appendix/api-levels.html.
 *
 */
public class SdkLevel {
  public static final int LEVEL_CUPCAKE = 3;           // a.k.a. 1.5
  public static final int LEVEL_DONUT = 4;             // a.k.a. 1.6
  public static final int LEVEL_ECLAIR = 5;            // a.k.a. 2.0
  public static final int LEVEL_ECLAIR_0_1 = 6;        // a.k.a. 2.0.1
  public static final int LEVEL_ECLAIR_MR1 = 7;        // a.k.a. 2.1
  public static final int LEVEL_FROYO = 8;             // a.k.a. 2.2
  public static final int LEVEL_GINGERBREAD = 9;       // a.k.a. 2.3
  public static final int LEVEL_GINGERBREAD_MR1 = 10;  // a.k.a. 2.3.3
  public static final int LEVEL_HONEYCOMB = 11;        // a.k.a. 3.0

  private SdkLevel() {
  }

  /**
   * Returns the API level of the SDK on the phone
   */
  public static int getLevel() {
    // Determine the SDK version in a way that is compatible with API level 3.
    return Integer.parseInt(Build.VERSION.SDK);
  }
}
