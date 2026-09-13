package com.TechHurts.CodeFile;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.*;
import java.util.*;
final class DiffModel {
    static final class Row {
        final String left,right;final int before,after;final boolean changed;
        Row(String left,String right,int before,int after,boolean changed){this.left=left;this.right=right;this.before=before;this.after=after;this.changed=changed;}
    }
    final List<Row> rows=new ArrayList<>();final String patch;int added,removed;
    DiffModel(String beforeName,String before,String afterName,String after) {
        List<String> a=Arrays.asList(SourceTools.normalize(before).split("\n",-1)),b=Arrays.asList(SourceTools.normalize(after).split("\n",-1));
        if(a.size()+b.size()>20000||before.length()+after.length()>1048576)throw new IllegalArgumentException("Compare at most 1 MiB / 20,000 combined lines");
        Patch<String> delta=DiffUtils.diff(a,b);
        patch=String.join("\n",UnifiedDiffUtils.generateUnifiedDiff(beforeName,afterName,a,delta,3))+"\n";
        int ai=0,bi=0;
        for(AbstractDelta<String> d:delta.getDeltas()) {
            int pos=d.getSource().getPosition();while(ai<pos){rows.add(new Row(a.get(ai),b.get(bi),ai+1,bi+1,false));ai++;bi++;}
            List<String> old=d.getSource().getLines(),now=d.getTarget().getLines();removed+=old.size();added+=now.size();
            for(int i=0;i<Math.max(old.size(),now.size());i++)rows.add(new Row(i<old.size()?old.get(i):null,i<now.size()?now.get(i):null,i<old.size()?ai+i+1:0,i<now.size()?bi+i+1:0,true));
            ai+=old.size();bi+=now.size();
        }
        while(ai<a.size()&&bi<b.size()){rows.add(new Row(a.get(ai),b.get(bi),ai+1,bi+1,false));ai++;bi++;}
    }
}
