package net.createmod.ponder.api.editor;

import java.util.*;
import net.createmod.ponder.guide.MarkdownTutorialCompiler;

/** Multiline authoring buffer. Selection and undo are independent of Minecraft's single-line text field. */
public final class MarkdownDocument {
    private String text,saved;
    private int cursor,anchor,revision;
    private record State(String text,int cursor,int anchor) {}
    private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    public MarkdownDocument(String text){this.text=this.saved=text;}
    public String text(){return text;}
    public int cursor(){return cursor;}
    public int anchor(){return anchor;}
    public int revision(){return revision;}
    public boolean dirty(){return !text.equals(saved);}
    public void saved(){saved=text;}
    public String selected(){return text.substring(Math.min(cursor,anchor),Math.max(cursor,anchor));}
    public void select(int position,boolean extend){cursor=Math.max(0,Math.min(text.length(),position));if(!extend)anchor=cursor;}
    public void selectAll(){anchor=0;cursor=text.length();}
    public void insert(String value){
        value=value.replace("\r\n","\n").replace('\r','\n');
        int a=Math.min(cursor,anchor),b=Math.max(cursor,anchor);
        if(text.length()-b+a+value.length()>MarkdownTutorialCompiler.MAX_CHARACTERS)throw new IllegalArgumentException("Markdown page is too large");
        remember();text=text.substring(0,a)+value+text.substring(b);cursor=anchor=a+value.length();revision++;
    }
    public void erase(boolean backwards){
        if(cursor==anchor){if(backwards&&cursor>0)anchor=cursor-1;else if(!backwards&&cursor<text.length())anchor=cursor+1;else return;}
        insert("");
    }
    public void undo(){restore(undo,redo);}
    public void redo(){restore(redo,undo);}
    private void restore(Deque<State> from,Deque<State> to){if(from.isEmpty())return;to.push(new State(text,cursor,anchor));State s=from.pop();text=s.text;cursor=s.cursor;anchor=s.anchor;revision++;}
    private void remember(){undo.push(new State(text,cursor,anchor));while(undo.size()>100)undo.removeLast();redo.clear();}
    public int lineStart(int position){return text.lastIndexOf('\n',Math.max(-1,position-1))+1;}
    public int lineEnd(int position){int end=text.indexOf('\n',position);return end<0?text.length():end;}
    public void moveVertical(int delta,boolean extend){
        int start=lineStart(cursor),column=cursor-start,target=start;
        if(delta<0){if(start==0){select(0,extend);return;}target=lineStart(start-1);}
        else{int end=lineEnd(cursor);if(end==text.length()){select(end,extend);return;}target=end+1;}
        select(Math.min(lineEnd(target),target+column),extend);
    }
    public int line(){return (int)text.substring(0,cursor).chars().filter(c->c=='\n').count();}
}
