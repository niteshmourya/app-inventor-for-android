// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android;

import com.google.devtools.simple.common.ComponentCategory;
import com.google.devtools.simple.common.PropertyCategory;
import com.google.devtools.simple.common.YaVersion;
import com.google.devtools.simple.runtime.annotations.DesignerComponent;
import com.google.devtools.simple.runtime.annotations.DesignerProperty;
import com.google.devtools.simple.runtime.annotations.SimpleObject;
import com.google.devtools.simple.runtime.annotations.SimpleProperty;
import com.google.devtools.simple.runtime.components.util.YailList;
import com.google.devtools.simple.runtime.errors.YailRuntimeError;

import android.app.Activity;
import android.content.Intent;

/**
 * A button allowing a user to select one among a list of text strings.
 *
 */
@DesignerComponent(version = YaVersion.LISTPICKER_COMPONENT_VERSION,
    category = ComponentCategory.BASIC,
    description = "<p>A button that, when clicked on, displays a list of " +
    "texts for the user to choose among. The texts can be specified through " +
    "the Designer or Blocks Editor by setting the " +
    "<code>ElementsFromString</code> property to their string-separated " +
    "concatenation (for example, <em>choice 1, choice 2, choice 3</em>) or " +
    "by setting the <code>Elements</code> property to a List in the Blocks " +
    "editor.</p>" +
    "<p>Other properties affect the appearance of the button " +
    "(<code>TextAlignment</code>, <code>BackgroundColor</code>, etc.) and " +
    "whether it can be clicked on (<code>Enabled</code>).</p>")
@SimpleObject
public class ListPicker extends Picker implements ActivityResultListener, Deleteable {

  private static final String LIST_ACTIVITY_CLASS = ListPickerActivity.class.getName();
  static final String LIST_ACTIVITY_ARG_NAME = LIST_ACTIVITY_CLASS + ".list";
  static final String LIST_ACTIVITY_RESULT_NAME = LIST_ACTIVITY_CLASS + ".selection";
  static final String LIST_ACTIVITY_RESULT_INDEX = LIST_ACTIVITY_CLASS + ".index";

  private YailList items;
  private String selection;
  private int selectionIndex;

  /**
   * Create a new ListPicker component.
   *
   * @param container the parent container.
   */
  public ListPicker(ComponentContainer container) {
    super(container);
    items = new YailList();
    selection = "";
    selectionIndex = 0;
  }

  /**
   * Selection property getter method.
   */
  @SimpleProperty(
      description = "<p>The selected item.  When directly changed by the " +
      "programmer, the SelectionIndex property is also changed to the first " +
      "item in the ListPicker with the given value.  If the value does not " +
      "appear, SelectionIndex will be set to 0.</p>",
      category = PropertyCategory.BEHAVIOR)
  public String Selection() {
    return selection;
  }

  /**
   * Selection property setter method.
   */
  @DesignerProperty(editorType = DesignerProperty.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty
  public void Selection(String value) {
    selection = value;
    // Now, we need to change SelectionIndex to correspond to Selection.
    // If multiple Selections have the same SelectionIndex, use the first.
    // If none do, arbitrarily set the SelectionIndex to its default value
    // of 0.
    for (int i = 0; i < items.size(); i++) {
      // The comparison is case-sensitive to be consistent with yail-equal?.
      if (items.getString(i).equals(value)) {
        selectionIndex = i + 1;
        return;
      }
    }
    selectionIndex = 0;
  }

  /**
   * Selection index property getter method.
   */
  @SimpleProperty(
      description = "<p>The index of the currently selected item, starting at " +
      "1.  If no item is selected, the value will be 0.  If an attempt is " +
      "made to set this to a number less than 1 or greater than the number " +
      "of items in the ListPicker, SelectionIndex will be set to 0, and " +
      "Selection will be set to the empty text.</p>",
      category = PropertyCategory.BEHAVIOR)
  public int SelectionIndex() {
    return selectionIndex;
  }

  /**
   * Selection index property setter method.
   */
  // Not a designer property, since this could lead to unpredictable
  // results if Selection is set to an incompatible value.
  @SimpleProperty
  public void SelectionIndex(int index) {
    if (index <= 0 || index > items.size()) {
      selectionIndex = 0;
      selection = "";
    } else {
      selectionIndex = index;
      // YailLists are 0-based, but we want to be 1-based.
      selection = items.getString(selectionIndex - 1);
    }
  }

  /**
   * Elements property getter method
   *
   * @return a YailList representing the list of strings to be picked from
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public YailList Elements() {
    return items;
  }

  /**
   * Elements property setter method
   * @param itemList - a YailList containing the strings to be added to the
   *                   ListPicker
   */
  // TODO(user): we need a designer property for lists
  @SimpleProperty
  public void Elements(YailList itemList) {
    Object[] objects = itemList.toStringArray();
    for (int i = 0; i < objects.length; i++) {
      if (!(objects[i] instanceof String)) {
        throw new YailRuntimeError("Items passed to ListPicker must be Strings", "Error");
      }
    }
    items = itemList;
  }

  /**
   * ElementsFromString property setter method
   *
   * @param itemstring - a string containing a comma-separated list of the
   *                     strings to be picked from
   */
  @DesignerProperty(editorType = DesignerProperty.PROPERTY_TYPE_STRING,
                    defaultValue = "")
  // TODO(user): it might be nice to have a list editorType where the developer
  // could directly enter a list of strings (e.g. one per row) and we could
  // avoid the comma-separated business.
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public void ElementsFromString(String itemstring) {
    if (itemstring.length() == 0) {
      items = new YailList();
    } else {
      items = YailList.makeList((Object[]) itemstring.split(" *, *"));
    }
  }

  @Override
  public Intent getIntent() {
    Intent intent = new Intent();
    intent.setClassName(container.$context(), LIST_ACTIVITY_CLASS);
    intent.putExtra(LIST_ACTIVITY_ARG_NAME, items.toStringArray());
    return intent;
  }

  /**
   * Callback method to get the result returned by the list picker activity
   *
   * @param requestCode a code identifying the request.
   * @param resultCode a code specifying success or failure of the activity
   * @param data the returned data, in this case an Intent whose data field
   *        contains the selected item.
   */
  public void resultReturned(int requestCode, int resultCode, Intent data) {
    if (requestCode == this.requestCode && resultCode == Activity.RESULT_OK) {
      if (data.hasExtra(LIST_ACTIVITY_RESULT_NAME)) {
        selection = data.getStringExtra(LIST_ACTIVITY_RESULT_NAME);
      } else {
        selection = "";
      }
      selectionIndex = data.getIntExtra(LIST_ACTIVITY_RESULT_INDEX, 0);
      AfterPicking();
    }
  }

  // Deleteable implementation

  @Override
  public void onDelete() {
    container.$form().unregisterForActivityResult(this);
  }

}
