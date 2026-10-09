package nl.ardio.medicijnmoment;

import android.content.*;
import org.json.*;
import java.util.*;

final class Store {
    private final SharedPreferences prefs;
    Store(Context c) { prefs=c.getSharedPreferences("medicines",Context.MODE_PRIVATE); }
    List<Medicine> load() {
        try {
            List<Medicine> result=new ArrayList<>();
            JSONArray a=new JSONArray(prefs.getString("list","[]"));
            for(int i=0;i<a.length();i++) result.add(Medicine.fromJson(a.getJSONObject(i)));
            return result;
        } catch(JSONException e) { throw new IllegalStateException("De medicatielijst kan niet worden gelezen.",e); }
    }
    void save(List<Medicine> list) {
        try {
            JSONArray a=new JSONArray();
            for(Medicine m:list) a.put(m.toJson());
            if(!prefs.edit().putString("list",a.toString()).commit())
                throw new IllegalStateException("Opslaan mislukt.");
        } catch(JSONException e) { throw new IllegalStateException(e); }
    }
    Medicine find(String id) {
        for(Medicine m:load()) if(m.id.equals(id)) return m;
        return null;
    }
    String key(String id,String time,String date) { return "taken|"+id+"|"+time+"|"+date; }
    boolean taken(String id,String time,String date) { return prefs.getBoolean(key(id,time,date),false); }
    void mark(String id,String time,String date,boolean value) {
        if(!prefs.edit().putBoolean(key(id,time,date),value).commit())
            throw new IllegalStateException("Afvinken mislukt.");
    }
}
