package com.TechHurts.CodeFile;
import android.content.Context;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.langs.textmate.*;
import io.github.rosemoe.sora.langs.textmate.registry.*;
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel;
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver;
import org.eclipse.tm4e.core.registry.IThemeSource;
final class NativeSyntax {
    private JSONObject catalog; private boolean loaded;
    void load(Context context) throws Exception {
        if(loaded)return;
        FileProviderRegistry.getInstance().addFileProvider(new AssetsFileResolver(context.getAssets()));
        for(String theme:new String[]{"darcula","quietlight","abyss","solarized_drak"}) {
            String path="textmate/"+theme+".json";
            ThemeRegistry.getInstance().loadTheme(new ThemeModel(IThemeSource.fromInputStream(context.getAssets().open(path),path,null),theme));
        }
        ThemeRegistry.getInstance().setTheme("darcula");
        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json");
        try(InputStream in=context.getAssets().open("languages.json");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1)out.write(buffer,0,count);catalog=new JSONObject(out.toString("UTF-8"));}
        loaded=true;
    }
    List<String> names(){List<String> result=new ArrayList<>();result.add("plain");if(catalog!=null)catalog.keys().forEachRemaining(result::add);Collections.sort(result);return result;}
    String detect(String name) {
        if(catalog==null)return "plain";
        for(String key:names()) {
            JSONObject value=catalog.optJSONObject(key);
            if(value==null)continue;
            for(String type:new String[]{"filenames","extensions"}) {
                JSONArray items=value.optJSONArray(type);
                if(items==null)continue;
                for(int i=0;i<items.length();i++) {
                    String item=items.optString(i);
                    if(type.equals("filenames")?name.equals(item):name.toLowerCase(Locale.ROOT).endsWith(item.toLowerCase(Locale.ROOT)))return key;
                }
            }
        }
        return "plain";
    }
    void apply(CodeEditor editor,String language,String theme) throws Exception {
        ThemeRegistry.getInstance().setTheme(theme);
        editor.setColorScheme(TextMateColorScheme.create(ThemeRegistry.getInstance()));
        JSONObject item=catalog==null?null:catalog.optJSONObject(language);
        try { editor.setEditorLanguage(item==null?null:TextMateLanguage.create(item.getString("scope"),false)); } catch(Exception e) { throw new IllegalStateException("Grammar unavailable: "+language+" "+item,e); }
    }
}
