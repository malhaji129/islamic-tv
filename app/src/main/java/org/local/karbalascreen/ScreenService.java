package org.local.karbalascreen;

import android.service.dreams.DreamService;
import android.view.KeyEvent;

public final class ScreenService extends DreamService {
    private ScreenView screen;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow(); setInteractive(false); setFullscreen(true); setScreenBright(false);
        screen = new ScreenView(this); setContentView(screen);
    }
    @Override public void onDreamingStarted() { super.onDreamingStarted(); if (screen != null) screen.start(); }
    @Override public void onDreamingStopped() { if (screen != null) screen.stop(); super.onDreamingStopped(); }
    @Override public void onDetachedFromWindow() { if (screen != null) screen.stop(); super.onDetachedFromWindow(); }
    @Override public boolean dispatchKeyEvent(KeyEvent event) { finish(); return true; }
}
