package org.local.karbalascreen;
import java.util.List;
/** Persist the page actually shown; preview owns no live reading state. */
final class ContentCursor {
    interface Page { String pageId(); String recordId(); }
    interface Store { State read(); boolean write(State state); }
    static final class State {
        final String version,page,record; final int index;
        State(String version,String page,String record,int index) {
            this.version=version; this.page=page; this.record=record; this.index=index;
        }
    }
    private final Store store; private final boolean live;
    ContentCursor(Store store,boolean live) { this.store=store;this.live=live; }
    int restore(List<? extends Page> pages,String version) {
        if(!live||pages.isEmpty())return 0;
        State saved=store.read(); if(saved==null)return 0;
        // Same-version index distinguishes repeated category turns. Verify its
        // identity too, because toggling devotional categories changes ordering.
        if(version.equals(saved.version)&&saved.index>=0&&saved.index<pages.size()
                &&pages.get(saved.index).pageId().equals(saved.page))return saved.index;
        for(int i=0;i<pages.size();i++)if(pages.get(i).pageId().equals(saved.page))return i;
        // A changed page budget may remove a fragment but retain its source.
        for(int i=0;i<pages.size();i++)if(pages.get(i).recordId().equals(saved.record))return i;
        return 0;
    }
    boolean save(List<? extends Page> pages,int index,String version) {
        if(!live||index<0||index>=pages.size())return false;
        Page page=pages.get(index);
        return store.write(new State(version,page.pageId(),page.recordId(),index));
    }
}
