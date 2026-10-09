package nl.ardio.medicijnmoment;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.util.*;

public class MainActivity extends Activity {
    private LinearLayout body;
    private Store store;
    private final Handler clock=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable() {
        @Override public void run() { render(); clock.postDelayed(this,60_000); }
    };
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) { super.onCreate(state); store=new Store(this); Reminders.channel(this); }
    @Override public void onResume() { super.onResume(); render(); clock.postDelayed(refresh,60_000); }
    @Override public void onPause() { clock.removeCallbacks(refresh); super.onPause(); }
    private TextView text(String value,int size,boolean bold) {
        TextView v=new TextView(this); v.setText(value); v.setTextSize(size);
        v.setTextColor(Color.rgb(27,51,48)); v.setPadding(0,dp(6),0,dp(6));
        if(bold) v.setTypeface(null,Typeface.BOLD); return v;
    }
    private Button button(String title,Runnable action) {
        Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setOnClickListener(v->action.run()); return b;
    }
    private LinearLayout panel() {
        LinearLayout p=new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setPadding(dp(16),dp(12),dp(16),dp(12));
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.WHITE); bg.setCornerRadius(dp(16)); p.setBackground(bg);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,dp(8),0,dp(8)); body.addView(p,lp); return p;
    }
    private void message(String m) { Toast.makeText(this,m,Toast.LENGTH_LONG).show(); }
    private void render() {
        ScrollView scroll=new ScrollView(this);
        body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(20),dp(20),dp(24));
        scroll.addView(body); scroll.setBackgroundColor(Color.rgb(243,247,246));
        scroll.setOnApplyWindowInsetsListener((view,insets)-> {
            if(Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets;
        });
        setContentView(scroll); scroll.requestApplyInsets();
        body.addView(text("MedicijnMoment",30,true));
        body.addView(text("Je eigen schema. Elke dag een herinnering.",16,false));
        long next=Reminders.schedule(this);
        LinearLayout status=panel();
        boolean notify=Reminders.notificationsAllowed(this),exact=Reminders.exactAllowed(this);
        status.addView(text(notify && exact ? "Herinneringen ingeschakeld" : "Herinneringen nog niet actief",18,true));
        if(!notify) {
            status.addView(text("Sta meldingen toe om herinneringen te ontvangen.",15,false));
            status.addView(button("Meldingen toestaan",this::requestNotifications));
        }
        if(!exact) {
            status.addView(text("Sta nauwkeurige alarmen toe voor je gekozen tijdstippen.",15,false));
            status.addView(button("Tijdstippen toestaan",()-> {
                if(Build.VERSION.SDK_INT>=31) launch(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));
            }));
        }
        if(next>0) status.addView(text("Volgende herinnering: "+Schedule.display(next),15,false));
        else if(notify && exact) status.addView(text("Voeg een medicijn toe of activeer een schema.",15,false));
        status.addView(button("Stuur testmelding",()-> {
            if(!Reminders.notificationsAllowed(this)) { requestNotifications(); return; }
            Reminders.show(this,new Medicine("test","Test","","",true,Collections.emptyList()),"","",true);
            message("Testmelding verstuurd. Controleer het meldingenscherm.");
        }));
        status.addView(button("Meldings- en geluidsinstellingen",()->launch(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,Reminders.CHANNEL))));
        body.addView(button("+ Medicijn toevoegen",()->edit(null)));
        body.addView(text("Mijn medicijnen",22,true));
        List<Medicine> medicines=store.load();
        if(medicines.isEmpty()) body.addView(text("Je lijst is nog leeg. Voeg een naam en één of meer tijden toe.",16,false));
        for(Medicine m:medicines) {
            LinearLayout card=panel(); card.addView(text(m.name,20,true));
            if(!m.dose.trim().isEmpty()) card.addView(text(m.dose,16,false));
            card.addView(text(String.join("  ·  ",m.times),17,true));
            if(!m.note.trim().isEmpty()) card.addView(text(m.note,15,false));
            Switch active=new Switch(this); active.setText(m.enabled?"Dagelijks actief":"Gepauzeerd"); active.setChecked(m.enabled);
            active.setPadding(0,dp(8),0,dp(8));
            active.setOnCheckedChangeListener((v,checked)-> {
                m.enabled=checked; store.save(medicines); Reminders.cancelNotifications(this); render();
            }); card.addView(active);
            card.addView(button("Bewerken",()->edit(m)));
        }
        body.addView(text("Vandaag",22,true));
        String date=LocalDate.now().toString();
        class Item { Medicine m; String time; Item(Medicine m,String t) { this.m=m; time=t; } }
        List<Item> items=new ArrayList<>();
        for(Medicine m:medicines) if(m.enabled) for(String t:m.times) items.add(new Item(m,t));
        items.sort(Comparator.comparing(item->item.time));
        if(items.isEmpty()) body.addView(text("Geen actieve innamemomenten.",16,false));
        for(Item item:items) {
            CheckBox check=new CheckBox(this);
            check.setText(item.time+"  "+item.m.name+(item.m.dose.trim().isEmpty()?"":" · "+item.m.dose));
            check.setTextSize(16); check.setMinHeight(dp(52));
            check.setChecked(store.taken(item.m.id,item.time,date));
            check.setOnCheckedChangeListener((v,checked)-> {
                if(checked) new AlertDialog.Builder(this).setTitle("Ingenomen bevestigen")
                    .setMessage("Heb je "+item.m.name+" voor "+item.time+" vandaag ingenomen?")
                    .setPositiveButton("Ja, ingenomen",(d,w)->mark(item.m,item.time,date,true))
                    .setNegativeButton("Annuleren",(d,w)->render()).setOnCancelListener(d->render()).show();
                else mark(item.m,item.time,date,false);
            }); body.addView(check);
        }
        body.addView(text("Afvinken betekent: door jou bevestigd als ingenomen. Je lijst blijft op deze telefoon. Tijden volgen de lokale tijdzone.",13,false));
    }
    private void mark(Medicine m,String time,String date,boolean value) {
        store.mark(m.id,time,date,value);
        getSystemService(NotificationManager.class).cancel(Reminders.tag(m.id,time,date),0);
        render();
    }
    private void launch(Intent i) {
        try { startActivity(i); } catch(ActivityNotFoundException e) { message("Open de app-instellingen van MedicijnMoment op je telefoon."); }
    }
    private void requestNotifications() {
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
            && !getPreferences(MODE_PRIVATE).getBoolean("asked",false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("asked",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);
        } else launch(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(code,permissions,results); render();
    }
    private EditText field(LinearLayout form,String label,String value,boolean multiline) {
        form.addView(text(label,14,true)); EditText e=new EditText(this); e.setText(value); e.setSingleLine(!multiline);
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (multiline?android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE:0));
        form.addView(e); return e;
    }
    private void edit(Medicine original) {
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),dp(8),dp(20),dp(12));
        EditText name=field(form,"Naam medicijn",original==null?"":original.name,false);
        EditText dose=field(form,"Dosering (optioneel, bijvoorbeeld 1 tablet)",original==null?"":original.dose,false);
        EditText times=field(form,"Dagelijkse tijden (08:00, 20:00)",original==null?"":String.join(", ",original.times),false);
        form.addView(button("Tijd kiezen en toevoegen",()->new TimePickerDialog(this,(v,h,m)-> {
            String chosen=String.format(Locale.ROOT,"%02d:%02d",h,m);
            String current=times.getText().toString().trim(); times.setText(current.isEmpty()?chosen:current+", "+chosen);
        },8,0,true).show()));
        EditText note=field(form,"Opmerking (optioneel)",original==null?"":original.note,true);
        ScrollView scroll=new ScrollView(this); scroll.addView(form);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(original==null?"Medicijn toevoegen":"Medicijn bewerken")
            .setView(scroll).setPositiveButton("Opslaan",null).setNegativeButton("Annuleren",null)
            .setNeutralButton(original==null?null:"Verwijderen",(d,w)-> {
                if(original!=null) new AlertDialog.Builder(this).setTitle("Medicijn verwijderen?")
                    .setMessage("Het schema voor "+original.name+" wordt verwijderd.")
                    .setPositiveButton("Verwijderen",(confirm,which)-> {
                        List<Medicine> all=store.load(); all.removeIf(m->m.id.equals(original.id)); store.save(all);
                        Reminders.cancelNotifications(this); render();
                    }).setNegativeButton("Annuleren",null).show();
            }).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v-> {
            String n=name.getText().toString().trim();
            if(n.isEmpty()) { name.setError("Vul een naam in."); return; }
            List<String> parsed;
            try { parsed=Schedule.parseTimes(times.getText().toString()); }
            catch(IllegalArgumentException e) { times.setError(e.getMessage()); return; }
            Medicine changed=new Medicine(original==null?UUID.randomUUID().toString():original.id,n,
                dose.getText().toString().trim(),note.getText().toString().trim(),original==null || original.enabled,parsed);
            List<Medicine> all=store.load();
            if(original==null) all.add(changed);
            else for(int j=0;j<all.size();j++) if(all.get(j).id.equals(original.id)) all.set(j,changed);
            store.save(all); Reminders.cancelNotifications(this); dialog.dismiss(); render();
        })); dialog.show();
    }
}
