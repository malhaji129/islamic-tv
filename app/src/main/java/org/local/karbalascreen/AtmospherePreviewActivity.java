package org.local.karbalascreen;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

/** Read-only, synthetic atmosphere examples; no weather cache, API calls or alarms. */
public final class AtmospherePreviewActivity extends Activity {
    private ScreenView screen;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        screen=new ScreenView(this,AtmospherePolicy.Mode.PREVIEW);setContentView(screen);
    }
    @Override protected void onResume(){super.onResume();screen.start();}
    @Override protected void onPause(){screen.stop();super.onPause();}
    @Override public boolean onKeyDown(int code,KeyEvent event){
        if(code==KeyEvent.KEYCODE_DPAD_LEFT){screen.previewAtmosphere(-1,0);return true;}
        if(code==KeyEvent.KEYCODE_DPAD_RIGHT){screen.previewAtmosphere(1,0);return true;}
        if(code==KeyEvent.KEYCODE_DPAD_UP){screen.previewAtmosphere(0,1);return true;}
        if(code==KeyEvent.KEYCODE_DPAD_DOWN){screen.previewAtmosphere(0,-1);return true;}
        return super.onKeyDown(code,event);
    }
}
