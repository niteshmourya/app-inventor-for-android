// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.devtools.simple.runtime.components.android.util;

import com.google.devtools.simple.runtime.components.android.Form;
import com.google.devtools.simple.runtime.components.android.collect.Lists;

import android.os.Handler;
import android.util.Log;
import android.widget.Toast;
import kawa.standard.Scheme;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;


/**
 * Support that runs on the phone for starting REPLs associated
 * with a form.
 * The initial handler for the menu is set up by OnCreateOptionsMenu
 * in Form.java.
 *
 */

// this is the controller that runs on the phone,  The other end of the Repl
// controller, that runs on the blocks client, is implemented in DeviceReplCommController.

public class ReplCommController {

  private static final String LOG_TAG = "REPL Controller";

  // port for communicating with codeblocks for the repl
  // this should agree with the value used in yacodeblocks.PhoneCommManager
  // TODO(user): consider whether we'd want to let this port
  // change dynamically.
  public static final int BLOCKS_EDITOR_PORT = 9997;

  private REPLServerController blocksEditorReplController;

  private Form form;
  private final Handler handler;

  // temporary security fix: only let the server start once
  private boolean everStarted = false;

  public ReplCommController(Form form) {

    this.form = form;

    // handler for posting alerts in the UI thread
    handler = new Handler();

    blocksEditorReplController = new REPLServerController(BLOCKS_EDITOR_PORT);
  }

  /**
   * Don't release the port. We expect to be the only ReplCommController
   * running on the phone at any time.
   */
  public void stopListening(boolean showAlert) {
    // nothing to do
  }

  /**
   * Destroy the repl server, releasing the port.
   */
  public void destroy() {
    blocksEditorReplController.StopServer();
  }

  /**
   * Start the phone's server listening to App Inventor.
   *
   * @param showAlert boolean that determines whether or not to show
   * an alert on the phone screen
   */
  public void startListening(boolean showAlert) {
    // if we've ever started the server, we're not willing to restart it
    // this overrides the next test. I'm leaving it here
    // in case we get rid of this start once policy
    if (everStarted) {
      return;
    }
    // if the server is already listening, don't restart
    if (blocksEditorReplController.ServerRunning()) {
      return;
    }
    blocksEditorReplController.StartServer();
    everStarted = true;
    if (showAlert) {
      ShowAlert("Listening to App Inventor. Click \"Restart app on device\" in the Blocks Editor "
          + " if you don't eventually see your components.");
    }
  }

  private void ShowAlert(final String notice) {
    // In REPL, it is currently ok to show notices that are in English.
    // However, in a component, it is not ok to do so. Therefore, this method should never be used
    // to show messages from a component such as GameClient or Voting.
    handler.post(new Runnable() {
      public void run() {
        Toast.makeText(form, notice, Toast.LENGTH_LONG).show();
      }
    });
  }

  /**
   * Opens a socket and starts a Kawa telnet REPL listening on the
   * given port.
   * Note: For the Blocks Editor server, the
   * other end of this connection, which runs on the CodeBlocks client
   * has I/O management to make the communication from the Blocks Editor more
   * that just a raw telnet. This is implemented in
   * yacodeblocks/DeviceReplCommcontrollers.
   */
  private class REPLServerController {

    private final Object lock = new Object();  // protects socket, openClientSockets
    // the socket and port for this REPL
    private ServerSocket socket;
    private int port;
    // The current thread running the server, if any
    private Thread serverThread;
    private List<Socket> openClientSockets;

    public REPLServerController (int port) {
      this.port = port;
      socket = null;
      serverThread = null;
      openClientSockets = Lists.newArrayList();
    }

    public void StartServer(){
      // closing the socket here should be redundant, but it might
      // have been left open due to some error
      closeSockets();
      serverThread = createServerThread();
      if (serverThread != null) {
        serverThread.start();
      }
    }

    public void StopServer() {
      Log.d(LOG_TAG, "Stopping server on port " + port);
      serverThread = null;
      closeSockets();
    }

    public boolean ServerRunning() {
      return serverThread != null && serverThread.isAlive();
    }

    private Thread createServerThread() {
      return new Thread(new Runnable() {
        public void run() {
          ServerSocket mySocket = null;
          Thread myThread = null;
          try {
            // TODO(user): I'm not 100% sure that we need to call setReuseAddress
            // but it doesn't seem to hurt.
            mySocket = new ServerSocket();
            mySocket.setReuseAddress(true);
            mySocket.bind(new InetSocketAddress((InetAddress) null, port));
            synchronized(lock) {
              socket = mySocket;
            }
            Log.d(LOG_TAG, "Starting a REPL Server thread on port " + port);
            myThread = Thread.currentThread();
            // TODO(user): this "if" used to be a "while". Changing it
            // to avoid opening more than one client socket.
            if (serverThread == myThread) {
              gnu.expr.ModuleExp.mustNeverCompile();
              final Socket clientSocket = mySocket.accept();
              synchronized(lock) {
                openClientSockets.add(clientSocket);
              }
              Thread telnetThread = TelnetRepl.serve(Scheme.getInstance("scheme"),
                  clientSocket);
              telnetThread.join();
            }
          } catch (InterruptedException e) {
            // TODO(user): what to do here? for now, nothing
          } catch (IOException e) {
            synchronized(lock) {
              if (socket != null && socket == mySocket) {
                // we didn't get here by having our socket closed by StopServer()
                // or another call to StartServer()
                Log.d(LOG_TAG, "IOException with server socket on port " + port +
                    ", closing sockets");
                Log.d(LOG_TAG, Log.getStackTraceString(e));
                closeSockets();
                if (serverThread == myThread) {
                  serverThread = null;
                }
              }
            }
          } finally {
            // TODO(user): see comments in Form about how we exit. May
            // need to fix it up here too.
            form.finish();
            System.exit(0);
          }
        }
      });
    }

    // Note: call this without lock held.
    private void closeSockets() {
      synchronized(lock) {
        if (socket != null) {
          Log.d(LOG_TAG, "Trying to close server sockets for port " + port);
          try {
            socket.close();
          } catch (IOException e) {
            Log.d(LOG_TAG, "IOException closing server socket on port " + port);
            Log.d(LOG_TAG, Log.getStackTraceString(e));
          } finally {
            socket = null;
            for (Socket openClientSocket : openClientSockets) {
              try {
                openClientSocket.close();
              } catch (IOException e) {
                Log.d(LOG_TAG, "IOException closing client socket on port " + port);
                Log.d(LOG_TAG, Log.getStackTraceString(e));
              } finally {
                openClientSockets = Lists.newArrayList();
              }
            }
          }
        }
      }
    }

  }
}
