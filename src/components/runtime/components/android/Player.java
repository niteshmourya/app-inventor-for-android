// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android;

import com.google.devtools.simple.common.ComponentCategory;
import com.google.devtools.simple.common.PropertyCategory;
import com.google.devtools.simple.common.YaVersion;
import com.google.devtools.simple.runtime.annotations.DesignerComponent;
import com.google.devtools.simple.runtime.annotations.DesignerProperty;
import com.google.devtools.simple.runtime.annotations.SimpleEvent;
import com.google.devtools.simple.runtime.annotations.SimpleFunction;
import com.google.devtools.simple.runtime.annotations.SimpleObject;
import com.google.devtools.simple.runtime.annotations.SimpleProperty;
import com.google.devtools.simple.runtime.annotations.UsesPermissions;
import com.google.devtools.simple.runtime.components.Component;
import com.google.devtools.simple.runtime.components.android.util.MediaUtil;
import com.google.devtools.simple.runtime.components.util.ErrorMessages;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;

import java.io.IOException;

// TODO: This implementation does nothing about releasing the Media
// Player resources when the application stops.  This needs to be handled
// at the application level, not just at the component level.
// We do release a previously used MediaPlayer before creating a new one.
//
// TODO: This implementation fails when there are multiple media
// players in an application.  This appears to be a bug in the
// Android SDK, or possibly in ODE, but we need to investigate more
// fully.
//
// TODO: Do more extensive testing of how state is handled here to see
// if the state restrictions are adequate given the API, and prove that
// there can't be deadlock or starvation.
// TODO: Remove writes of state debugging info to the log after we're
// sure things are working solidly.
/**
 * Multimedia component that plays audio or video and optionally
 * vibrates.  It is built on top of {@link android.media.MediaPlayer}.
 *
 */
@DesignerComponent(version = YaVersion.PLAYER_COMPONENT_VERSION,
    description = "<p>Multimedia component that plays audio or video and " +
    "controls phone vibration.  The name of a multimedia field is " +
    "specified in the <code>Source</code> property, which can be set in " +
    "the Designer or in the Blocks Editor.  The length of time for a " +
    "vibration is specified in the Blocks Editor in milliseconds " +
    "(thousandths of a second).</p>" +
    "<p>For legal sound and video formats, see " +
    "<a href=\"http://developer.android.com/guide/appendix/media-formats.html\"" +
    " target=\"_blank\">Android Supported Media Formats</a>.</p>" +
    "<p>If you will only be playing sound files and vibrating, not using " +
    "video, this component is best for long sound files, such as songs, " +
    "while the <code>Sound</code> component is more efficient for short " +
    "files, such as sound effects.</p>",
    category = ComponentCategory.MEDIA,
    nonVisible = true,
    iconName = "images/player.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.VIBRATE, android.permission.INTERNET")
public final class Player extends AndroidNonvisibleComponent implements Component, Deleteable {

  private MediaPlayer mp;
  private final Vibrator vibe;

  private int playerState;
  private String sourcePath;

  /*
   * playerState encodes a simplified version of the full MediaPlayer state space, that should be
   * adequate, given this API:
   * 0: player initial state
   * 1: player prepared but not started
   * 2: player started or paused
   * The allowable transitions are:
   * Start: must be called in state 1 or 2, results in state 2
   * Pause: must be called in state 2, results in state 2
   * Stop: must be called in state 1 or 2, results in state 1
   * We can simplify this to remove state 0 and use a simple boolean after we're
   * more confident that there are no start-up problems.
   */

  /**
   * Creates a new Player component.
   *
   * @param container
   */
  public Player(ComponentContainer container) {
    super(container.$form());
    sourcePath = "";
    vibe = (Vibrator) form.getSystemService(Context.VIBRATOR_SERVICE);
  }

  /**
   * Returns the path to the audio or video source
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR)
  public String Source() {
    return sourcePath;
  }

  /**
   * Sets the audio or video source.
   *
   * <p/>See {@link MediaUtil#determineMediaSource} for information about what
   * a path can be.
   *
   * @param path  the path to the audio or video source
   */
  @DesignerProperty(editorType = DesignerProperty.PROPERTY_TYPE_ASSET,
      defaultValue = "")
  @SimpleProperty
  public void Source(String path) {
    sourcePath = (path == null) ? "" : path;

    // Clear the previous MediaPlayer.
    if (playerState == 1 || playerState == 2) {
      mp.stop();
    }
    playerState = 0;
    if (mp != null) {
      mp.release();
      mp = null;
    }

    if (sourcePath.length() > 0) {
      Log.i("Player", "Source path is " + sourcePath);
      mp = new MediaPlayer();

      try {
        MediaUtil.loadMediaPlayer(mp, form, sourcePath);

      } catch (IOException e) {
        mp.release();
        mp = null;
        form.dispatchErrorOccurredEvent(this, "Source",
            ErrorMessages.ERROR_UNABLE_TO_LOAD_MEDIA, sourcePath);
        return;
      }

      mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
      Log.i("Player", "Successfully loaded source path " + sourcePath);

      // The Simple API is set up so that the user never has
      // to call prepare.
      prepare();
      // Player should now be in state 1. (If prepare failed, we are in state 0.)
    }
  }

  /**
   * Plays the media.  If it was previously paused, the playing is resumed.
   */
  @SimpleFunction
  public void Start() {
    Log.i("Player", "Calling Start -- State=" + playerState);
    if (playerState == 1 || playerState == 2) {
      mp.start();
      playerState = 2;
      // Player should now be in state 2
    }
  }

  /**
   * Suspends playing the media if it is playing.
   */
  @SimpleFunction
  public void Pause() {
    Log.i("Player", "Calling Pause -- State=" + playerState);
    if (playerState == 2) {
      mp.pause();
      playerState = 2;
      // Player should now be in state 2.
    }
  }

  /**
   * Stops playing the media
   */
  @SimpleFunction
  public void Stop() {
    Log.i("Player", "Calling Stop -- State=" + playerState);
    if (playerState == 1 || playerState == 2) {
      mp.stop();
      prepare();
      // Player should now be in state 1. (If prepare failed, we are in state 0.)
    }
  }

  //  TODO: Reconsider whether vibrate should be here or in a separate component.
  /**
   * Vibrates for specified number of milliseconds.
   */
  @SimpleFunction
  public void Vibrate(long milliseconds) {
    vibe.vibrate(milliseconds);
  }

  @SimpleEvent(description = "The PlayerError event is no longer used. " +
      "Please use the Screen.ErrorOccurred event instead.",
      userVisible = false)
  public void PlayerError(String message) {
  }

  private void prepare() {
    // This should be called only after mp.stop() or directly after
    // initialization
    try {
      mp.prepare();
      playerState = 1;
      Log.i("Player", "Successfully prepared");

    } catch (IOException ioe) {
      mp.release();
      mp = null;
      playerState = 0;
      form.dispatchErrorOccurredEvent(this, "Source",
          ErrorMessages.ERROR_UNABLE_TO_PREPARE_MEDIA, sourcePath);
    }
  }

  // Deleteable implementation

  @Override
  public void onDelete() {
    mp.stop();
    mp.release();
    vibe.cancel();
  }
}
