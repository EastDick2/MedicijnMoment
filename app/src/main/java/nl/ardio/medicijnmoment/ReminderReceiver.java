package nl.ardio.medicijnmoment;

import android.app.NotificationManager;
import android.content.*;
import java.time.*;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) {
        Store s=new Store(c);
        if("TAKEN".equals(i.getAction())) {
            String id=i.getStringExtra("id"),t=i.getStringExtra("time"),d=i.getStringExtra("date");
            Medicine m=s.find(id);
            if(m!=null && m.times.contains(t) && d!=null) s.mark(id,t,d,true);
            c.getSystemService(NotificationManager.class).cancel(Reminders.tag(id,t,d),0);
            Reminders.schedule(c);
            return;
        }
        if(!"REMIND".equals(i.getAction())) return;
        long due=i.getLongExtra("due",0);
        Reminders.schedule(c);
        if(due<=0 || due>System.currentTimeMillis()+1000) return;
        ZoneId zone=ZoneId.systemDefault();
        LocalDate date=Instant.ofEpochMilli(due).atZone(zone).toLocalDate();
        // Deliver every medicine for this occurrence; equal times share one system alarm.
        for(Medicine m:s.load()) if(m.enabled) for(String t:m.times) {
            if(Schedule.onDate(t,date,zone)==due && !s.taken(m.id,t,date.toString()))
                Reminders.show(c,m,t,date.toString(),false);
        }
    }
}
