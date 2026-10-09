package nl.ardio.medicijnmoment;

import org.json.*;
import java.util.*;

final class Medicine {
    String id, name, dose, note;
    boolean enabled;
    List<String> times;
    Medicine(String id, String name, String dose, String note, boolean enabled, List<String> times) {
        this.id=id; this.name=name; this.dose=dose; this.note=note;
        this.enabled=enabled; this.times=times;
    }
    static Medicine fromJson(JSONObject j) throws JSONException {
        List<String> times = new ArrayList<>();
        JSONArray a=j.getJSONArray("times");
        for(int i=0;i<a.length();i++) times.add(a.getString(i));
        return new Medicine(j.getString("id"),j.getString("name"),j.optString("dose"),
            j.optString("note"),j.optBoolean("enabled",true),times);
    }
    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id",id).put("name",name).put("dose",dose)
            .put("note",note).put("enabled",enabled).put("times",new JSONArray(times));
    }
}
