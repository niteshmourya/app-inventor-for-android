// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android;

import com.google.devtools.simple.common.ComponentCategory;
import com.google.devtools.simple.common.PropertyCategory;
import com.google.devtools.simple.common.YaVersion;
import com.google.devtools.simple.runtime.annotations.DesignerComponent;
import com.google.devtools.simple.runtime.annotations.DesignerProperty;
import com.google.devtools.simple.runtime.annotations.SimpleObject;
import com.google.devtools.simple.runtime.annotations.SimpleProperty;
import com.google.devtools.simple.runtime.components.Component;
import com.google.devtools.simple.runtime.components.android.util.PaintUtil;

import android.graphics.Canvas;
import android.graphics.Paint;

/**
 * Simple ball, based on Sprite implementation.
 *
 */
@DesignerComponent(version = YaVersion.BALL_COMPONENT_VERSION,
    description = "<p>A round 'sprite' that can be placed on a " +
    "<code>Canvas</code>, where it can react to touches and drags, " +
    "interact with other sprites (<code>ImageSprite</code>s and other " +
    "<code>Ball</code>s) and the edge of the Canvas, and move according " +
    "to its property values.</p>" +
    "<p>For example, to have a <code>Ball</code> move 4 pixels toward the " +
    "top of a <code>Canvas</code> every 500 milliseconds (half second), " +
    "you would set the <code>Speed</code> property to 4 [pixels], the " +
    "<code>Interval</code> property to 500 [milliseconds], the " +
    "<code>Heading</code> property to 90 [degrees], and the " +
    "<code>Enabled</code> property to <code>True</code>.  These and its " +
    "other properties can be changed at any time.</p>" +
    "<p>The difference between a Ball and an <code>ImageSprite</code> is " +
    "that the latter can get its appearance from an image file, while a " +
    "Ball's appearance can only be changed by varying its " +
    "<code>PaintColor</code> and <code>Radius</code> properties.</p>",
    category = ComponentCategory.ANIMATION)
@SimpleObject
public final class Ball extends Sprite {
  private int radius;
  private int paintColor;
  private Paint paint;
  static final int DEFAULT_RADIUS = 5;

  public Ball(ComponentContainer container) {
    super(container);
    paint = new Paint();

    // Set default properties.
    PaintColor(Component.COLOR_BLACK);
    Radius(DEFAULT_RADIUS);
  }

  // Implement or override methods

  @Override
  protected void onDraw(Canvas canvas) {
    if (visible) {
      canvas.drawCircle((float) xLeft + radius, (float) yTop + radius, radius, paint);
    }
  }

  // The following four methods are required by abstract superclass
  // VisibleComponent.  Because we don't want to expose them to the Simple
  // programmer, we omit the SimpleProperty and DesignerProperty pragmas.
  @Override
  public int Height() {
    return 2 * radius;
  }

  @Override
  public void Height(int height) {
    // ignored
  }

  @Override
  public int Width() {
    return 2 * radius;
  }

  @Override
  public void Width(int width) {
    // ignored
  }

  @Override
  public boolean containsPoint(double qx, double qy) {
    double xCenter = xLeft + radius;
    double yCenter = yTop + radius;
    return ((qx - xCenter) * (qx - xCenter) + (qy - yCenter) * (qy - yCenter))
        <= radius * radius;
  }


  // Additional properties

  @DesignerProperty(editorType = DesignerProperty.PROPERTY_TYPE_INTEGER,
      defaultValue = "5")
  @SimpleProperty(
      // Kind of both categories: APPEARANCE and BEHAVIOR
      category = PropertyCategory.APPEARANCE)
  public void Radius(int radius) {
    this.radius = radius;
    registerChange();
  }

  @SimpleProperty
  public int Radius() {
    return radius;
  }

  /**
   * PaintColor property getter method.
   *
   * @return  paint RGB color with alpha
   */
  @SimpleProperty(
      category = PropertyCategory.APPEARANCE)
  public int PaintColor() {
    return paintColor;
  }

  /**
   * PaintColor property setter method.
   *
   * @param argb  paint RGB color with alpha
   */
  @DesignerProperty(editorType = DesignerProperty.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_BLACK)
  @SimpleProperty
  public void PaintColor(int argb) {
    paintColor = argb;
    if (argb != Component.COLOR_DEFAULT) {
      PaintUtil.changePaint(paint, argb);
    } else {
      // The default paint color is black.
      PaintUtil.changePaint(paint, Component.COLOR_BLACK);
    }
    registerChange();
  }
}
