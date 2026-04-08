package org.areslib.sim;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashSet;
import java.util.Set;

public class DesktopKeyboardListener implements KeyListener {

  private final Set<Integer> pressedKeys = new HashSet<>();

  public boolean isKeyDown(int keyCode) {
    synchronized (pressedKeys) {
      return pressedKeys.contains(keyCode);
    }
  }

  public boolean isKeyDown(char keyChar) {
    int keyCode = KeyEvent.getExtendedKeyCodeForChar(keyChar);
    return isKeyDown(keyCode);
  }

  @Override
  public void keyTyped(KeyEvent e) {
    // Not used
  }

  @Override
  public void keyPressed(KeyEvent e) {
    synchronized (pressedKeys) {
      pressedKeys.add(e.getKeyCode());
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    synchronized (pressedKeys) {
      pressedKeys.remove(e.getKeyCode());
    }
  }
}
