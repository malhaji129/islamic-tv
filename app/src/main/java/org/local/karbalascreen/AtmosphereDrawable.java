package org.local.karbalascreen;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/** Small, deterministic decoration layer. All objects are reused; no image work per frame. */
final class AtmosphereDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler=new Handler(Looper.getMainLooper());
    private AtmospherePolicy.Phase phase=AtmospherePolicy.Phase.DAY;
    private AtmospherePolicy.Kind kind=AtmospherePolicy.Kind.UNKNOWN;
    private boolean visible,enabled,dust;
    private int wind;
    private LinearGradient gradient;
    private final Runnable frame=new Runnable(){public void run(){if(!needsFrames())return;invalidateSelf();handler.postDelayed(this,AtmospherePolicy.FRAME_MS);}};
    void update(AtmospherePolicy.Phase phase,AtmospherePolicy.Kind kind,int wind,boolean dust,boolean visible,boolean enabled){
        boolean phaseChanged=this.phase!=phase;
        this.phase=phase;this.kind=kind;this.wind=wind;this.dust=dust;this.visible=visible;this.enabled=enabled;
        if(phaseChanged)makeGradient();handler.removeCallbacks(frame);if(needsFrames())handler.postDelayed(frame,AtmospherePolicy.FRAME_MS);invalidateSelf();
    }
    void stop(){visible=false;handler.removeCallbacks(frame);}
    private boolean needsFrames(){return AtmospherePolicy.animate(visible,enabled,kind,wind);}
    @Override protected void onBoundsChange(Rect bounds){makeGradient();}
    private void makeGradient(){
        int top,bottom;
        switch(phase){
            case SAHAR:top=0x88091229;bottom=0x332F3750;break;
            case DAWN:top=0x66464B70;bottom=0x557C5B67;break;
            case SUNRISE:top=0x33373F66;bottom=0x667F5828;break;
            case SUNSET:top=0x55402C58;bottom=0x777F421C;break;
            case NIGHT:top=0x99061025;bottom=0x33030C18;break;
            default:top=0x22286986;bottom=0x110E3838;
        }
        gradient=new LinearGradient(0,0,0,Math.max(1,getBounds().height()),top,bottom,Shader.TileMode.CLAMP);
    }
    @Override public void draw(Canvas canvas){
        if(!visible||!enabled)return;
        Rect bounds=getBounds();float w=bounds.width(),h=bounds.height();if(w<=0||h<=0)return;
        paint.setShader(gradient);paint.setAlpha(255);canvas.drawRect(bounds,paint);paint.setShader(null);
        boolean dark=phase==AtmospherePolicy.Phase.NIGHT||phase==AtmospherePolicy.Phase.SAHAR;
        if(dark){
            paint.setColor(0x668FACC2);
            for(int i=0;i<18;i++){float x=w*((i*73+19)%97)/100f,y=h*((i*31+7)%33)/100f;canvas.drawCircle(x,y,i%3==0?1.5f:0.8f,paint);}
            paint.setColor(0x66C9D8D0);canvas.drawCircle(w*.50f,h*.16f,h*.035f,paint);
            paint.setColor(0xFF0D1A26);canvas.drawCircle(w*.512f,h*.151f,h*.032f,paint);
        }else if(kind==AtmospherePolicy.Kind.CLEAR||kind==AtmospherePolicy.Kind.PARTLY_CLOUDY){
            paint.setColor(phase==AtmospherePolicy.Phase.SUNSET?0x77D18A4D:0x66E4C982);
            canvas.drawCircle(w*.50f,h*(phase==AtmospherePolicy.Phase.DAY?.16f:.28f),h*.04f,paint);
        }
        if(kind==AtmospherePolicy.Kind.PARTLY_CLOUDY||kind==AtmospherePolicy.Kind.OVERCAST||AtmospherePolicy.rain(kind)||AtmospherePolicy.snow(kind)){
            paint.setColor(kind==AtmospherePolicy.Kind.PARTLY_CLOUDY?0x224D6474:0x44435262);
            for(int i=0;i<5;i++){float x=w*(.04f+i*.22f),y=h*(.04f+(i%2)*.035f);canvas.drawOval(x,y,x+w*.28f,y+h*.11f,paint);}
        }
        if(kind==AtmospherePolicy.Kind.FOG){paint.setColor(0x226E8290);for(int i=0;i<4;i++)canvas.drawRect(0,h*(.25f+i*.18f),w,h*(.32f+i*.18f),paint);}
        if(dust){paint.setColor(0x33917D42);canvas.drawRect(bounds,paint);paint.setColor(0x226F5E39);for(int i=0;i<3;i++)canvas.drawRect(0,h*(.35f+i*.2f),w,h*(.41f+i*.2f),paint);}
        long elapsed=SystemClock.elapsedRealtime();float travel=(elapsed%10000)/10000f;
        if(AtmospherePolicy.rain(kind)){
            boolean drizzle=kind==AtmospherePolicy.Kind.DRIZZLE||kind==AtmospherePolicy.Kind.FREEZING_DRIZZLE;
            paint.setColor(kind==AtmospherePolicy.Kind.FREEZING_RAIN||kind==AtmospherePolicy.Kind.FREEZING_DRIZZLE?0x5568B9CC:0x447AA7BA);paint.setStrokeWidth(Math.max(1,w/1400));
            int count=drizzle?10:22;
            for(int i=0;i<count;i++){float x=w*((i*37%101)/101f),y=h*((i*17/29f+travel*3)%1);canvas.drawLine(x,y,x-w*.008f,y+h*(drizzle?.012f:.032f),paint);}
        }
        if(AtmospherePolicy.snow(kind)||kind==AtmospherePolicy.Kind.HAIL_THUNDER){
            paint.setColor(0x669ABEC4);int count=kind==AtmospherePolicy.Kind.SNOW_GRAINS?12:20;
            for(int i=0;i<count;i++){float x=w*((i*43%101)/101f)+w*.008f*(float)Math.sin(i+travel*6),y=h*((i*19/31f+travel)%1);canvas.drawCircle(x,y,h*(kind==AtmospherePolicy.Kind.SNOW_GRAINS?.0015f:.003f),paint);}
        }
        if(wind>0){paint.setColor(0x337CABA9);paint.setStrokeWidth(Math.max(1,w/1300));for(int i=0;i<(wind==2?10:6);i++){float x=w*((i*.19f+travel*(wind==2?5:3))%1),y=h*(.18f+(i%7)*.1f);canvas.drawLine(x,y,x+w*.045f,y-h*.008f,paint);}}
        if(AtmospherePolicy.thunder(kind)){
            long age=elapsed%AtmospherePolicy.THUNDER_PERIOD_MS;
            if(age<1250){int alpha=Math.round(AtmospherePolicy.THUNDER_MAX_ALPHA*(1-age/1250f));paint.setColor(Color.argb(alpha,170,191,205));canvas.drawRect(bounds,paint);}
        }
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
