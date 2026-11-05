package org.local.karbalascreen;
import java.util.*;
public final class ContentCursorTest {
 static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
 static final class Page implements ContentCursor.Page {
  final String record,page; Page(String r,String p){record=r;page=p;}
  public String recordId(){return record;} public String pageId(){return page;}
 }
 static final class Memory implements ContentCursor.Store {
  ContentCursor.State state; int reads,writes;
  public ContentCursor.State read(){reads++;return state;}
  public boolean write(ContentCursor.State next){state=next;writes++;return true;}
 }
 public static void main(String[]args){
  List<Page> pages=Arrays.asList(new Page("q1","q1p1"),new Page("q1","q1p2"),new Page("h1","h1p1"),new Page("q1","q1p1"));
  Memory memory=new Memory();ContentCursor live=new ContentCursor(memory,true);
  check(live.restore(pages,"v1")==0,"First launch starts at first page");
  check(live.save(pages,1,"v1"),"Last shown fragment persists synchronously");
  check(new ContentCursor(memory,true).restore(pages,"v1")==1,"Restart resumes same fragment");
  int next=ScreenPolicy.rotate(1,1,pages.size());live.save(pages,next,"v1");
  check(new ContentCursor(memory,true).restore(pages,"v1")==2,"Rotation persists the newly shown item");
  live.save(pages,3,"v1");check(live.restore(pages,"v1")==3,"Repeated category turn retains its position");
  List<Page> reordered=Arrays.asList(pages.get(2),pages.get(0),pages.get(1));
  check(live.restore(reordered,"v1")==1,"Dua toggle/reordering migrates by stable page identity");
  live.save(pages,1,"v1");
  check(live.restore(Arrays.asList(new Page("other","other"),pages.get(1)),"v2")==1,"Corpus update finds retained page");
  check(live.restore(Arrays.asList(new Page("q1","newpart")),"v2")==0,"Repagination resumes source record");
  check(live.restore(Arrays.asList(new Page("gone","gone")),"v2")==0,"Removed source resets safely");
  int reads=memory.reads,writes=memory.writes;ContentCursor preview=new ContentCursor(memory,false);
  check(preview.restore(pages,"preview")==0,"Preview starts independently");
  check(!preview.save(pages,2,"preview"),"Preview never persists");
  check(memory.reads==reads&&memory.writes==writes,"Preview must neither read nor write live progress");
  check(live.restore(Collections.emptyList(),"v1")==0,"Empty library safe");
  System.out.println("Content cursor begin/rotate/restart/migration/preview tests passed");
 }
}
