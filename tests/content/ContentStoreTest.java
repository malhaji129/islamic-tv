package org.local.karbalascreen;
import android.content.Context;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import android.content.SharedPreferences;
public final class ContentStoreTest {
    static void check(boolean ok,String reason) { if(!ok)throw new AssertionError(reason); }
    static final class Preferences implements SharedPreferences {
        final Map<String,Object> disk=new HashMap<>(); int commits,reads;
        public String getString(String key,String fallback){reads++;return (String)disk.getOrDefault(key,fallback);}
        public int getInt(String key,int fallback){reads++;return (Integer)disk.getOrDefault(key,fallback);}
        public Editor edit(){return new Editor(){
            final Map<String,Object> pending=new HashMap<>();
            public Editor putString(String key,String value){pending.put(key,value);return this;}
            public Editor putInt(String key,int value){pending.put(key,value);return this;}
            public boolean commit(){disk.putAll(pending);commits++;return true;}
        };}
    }
    public static void main(String[] args) throws Exception {
        Context context=new Context(Paths.get("app/src/main/assets"));
        List<ContentStore.Item> items=ContentStore.load(context,true);
        check(items.size()>16342,"Real 4.9MB asset must load, including pagination; got "+items.size());
        Set<String> quran=new HashSet<>(), sayings=new HashSet<>();
        for(ContentStore.Item item:items) {
            if(item.kind.equals("quran")) quran.add(item.source);
            if(item.kind.equals("hadith")) sayings.add(item.recordId());
        }
        check(quran.size()==6236,"Complete Quran must survive production asset loading");
        check(sayings.size()==10096,"All verified sayings must survive production asset loading");
        List<ContentStore.Item> quranOnly=ContentStore.load(context,false);
        check(context.getAssets().opens==1,"Restart and devotional toggles must reuse the parsed corpus");
        for(ContentStore.Item item:quranOnly)check(item.kind.equals("quran"),"Disabled devotionals excluded");
        check(new HashSet<>(quranOnly.stream().map(ContentStore.Item::pageId).collect(java.util.stream.Collectors.toList())).size()==quranOnly.size(),"Quran-only rotation contains each page once");
        String summary=ContentStore.summary(context);
        check(summary.contains("6236")&&summary.contains("10096")&&summary.contains("10"),"Settings counts come from the small manifest");
        check(Files.size(Paths.get("app/src/main/assets/content-manifest.json"))<4096,"Settings metadata stays small");
        Preferences prefs=new Preferences();
        check(ContentProgress.restore(prefs,items,true)==0,"Missing preference starts at first page");
        check(ContentProgress.save(prefs,items,41,true),"Production adapter must commit current fragment");
        check(prefs.commits==1,"Save must synchronously commit once");
        check(ContentProgress.restore(prefs,items,true)==41,"A fresh production cursor resumes saved fragment");
        int reads=prefs.reads,commits=prefs.commits;
        check(ContentProgress.restore(prefs,items,false)==0,"Preview cursor starts independently");
        check(!ContentProgress.save(prefs,items,100,false),"Preview save is rejected");
        check(prefs.reads==reads&&prefs.commits==commits,"Preview never touches preferences");
        try(InputStream input=Files.newInputStream(Paths.get("app/src/main/assets/content.json"))){
            try{StreamText.read(input);throw new AssertionError("HTTP response cap accidentally expanded");}catch(IOException expected){}
        }
        try{ContentAssetText.read(new ByteArrayInputStream(new byte[ContentAssetText.MAX_BYTES+1]));throw new AssertionError("Oversize asset must reject");}catch(IOException expected){}
        System.out.println("Real-asset ContentStore test passed: "+items.size()+" display pages");
    }
}
