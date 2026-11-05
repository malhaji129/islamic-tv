package org.local.karbalascreen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

public final class PreviewActivity extends Activity {
    private ScreenView screen;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        screen = new ScreenView(this); setContentView(screen);
    }
    @Override protected void onResume() { super.onResume(); screen.start(); }
    @Override protected void onPause() { screen.stop(); super.onPause(); }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_DPAD_LEFT) { screen.navigate(-1); return true; }
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT) { screen.navigate(1); return true; }
        if (code == KeyEvent.KEYCODE_MENU) { startActivity(new Intent(this, SettingsActivity.class)); return true; }
        return super.onKeyDown(code, event);
    }
}
