package edu.bsc.exam;
public final class Html {
 private Html() {}
 public static String escape(String s) { if(s==null)return ""; return s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
