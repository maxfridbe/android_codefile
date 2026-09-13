package com.TechHurts.CodeFile;

import android.content.Context;
import android.os.Build;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/** Bounded, content-free diagnostics. Never pass documents, keys, or API payloads here. */
final class DebugLog {
    private final File file;
    private final String header;
    DebugLog(Context context) {
        file=new File(context.getFilesDir(),"diagnostics.log");
        String version="unknown";
        try { version=context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName; } catch(Exception ignored) { }
        header="CodeFile diagnostic report\nPackage: "+context.getPackageName()+"\nVersion: "+version+"\nAndroid: "+Build.VERSION.RELEASE+" (API "+Build.VERSION.SDK_INT+")\nDevice: "+Build.MANUFACTURER+" "+Build.MODEL+"\nUI: native Android / Sora CodeEditor\nSource, URLs, prompts, responses, and credentials are omitted.\n\n";
    }
    synchronized void event(String event) {
        try {
            if(file.length()>128*1024) {
                File old=new File(file.getParentFile(),"diagnostics.previous.log");
                if(old.exists()) old.delete();
                file.renameTo(old);
            }
            String timestamp=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX",Locale.ROOT).format(new Date());
            try(FileOutputStream out=new FileOutputStream(file,true)) {out.write((timestamp+" "+event+"\n").getBytes(StandardCharsets.UTF_8));}
        } catch(IOException ignored) { }
    }
    synchronized File gzip(Context context) throws IOException {
        File dir=new File(context.getCacheDir(),"shares");dir.mkdirs();
        File result=new File(dir,UUID.randomUUID()+"_CodeFile-debug.log.gz");
        try(GZIPOutputStream out=new GZIPOutputStream(new FileOutputStream(result))) {
            out.write(header.getBytes(StandardCharsets.UTF_8));
            for(File log:new File[]{new File(file.getParentFile(),"diagnostics.previous.log"),file}) if(log.exists()) {
                try(FileInputStream in=new FileInputStream(log)){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}
            }
        }
        return result;
    }
}