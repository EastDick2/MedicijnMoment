package nl.ardio.medicijnmoment;

import android.app.*;
import android.content.*;
import android.os.Build;
import java.time.ZoneId;

final class Reminders {
    static final String CHANNEL="medication";
    static AlarmManager alarms(Context c) { return c.getSystemService(AlarmManager.class); }
    static boolean exactAllowed(Context c) {
        return Build.VERSION.SDK_INT<31 || alarms(c).canScheduleExactAlarms();
    }
    static void channel(Context c) {
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Medicijnherinneringen",NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Herinneringen op je zelf ingestelde innametijden");
        channel.enableVibration(true);
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    static boolean notificationsAllowed(Context c) {
        channel(c);
        NotificationManager n=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=n.getNotificationChannel(CHANNEL);
        return n.areNotificationsEnabled() && ch.getImportance()!=NotificationManager.IMPORTANCE_NONE;
    }
    static PendingIntent alarmIntent(Context c,long due) {
        Intent i=new Intent(c,ReminderReceiver.class).setAction("REMIND").putExtra("due",due);
        return PendingIntent.getBroadcast(c,1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    @android.annotation.SuppressLint("ScheduleExactAlarm") // Guarded by exactAllowed; revocation is also caught.
    static long schedule(Context c) {
        AlarmManager a=alarms(c);
        a.cancel(alarmIntent(c,0));
        if(!exactAllowed(c) || !notificationsAllowed(c)) return -1;
        long now=System.currentTimeMillis(), next=Long.MAX_VALUE;
        Store s=new Store(c);
        for(Medicine m:s.load()) if(m.enabled) for(String t:m.times) {
            long n=Schedule.next(t,now,ZoneId.systemDefault());
            if(s.taken(m.id,t,Schedule.date(n)))
                n=Schedule.next(t,n,ZoneId.systemDefault());
            next=Math.min(next,n);
        }
        if(next==Long.MAX_VALUE) return -1;
        try { a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next,alarmIntent(c,next)); }
        catch(SecurityException e) { return -1; }
        return next;
    }
    static String tag(String id,String time,String date) { return id+"|"+time+"|"+date; }
    static void cancelNotifications(Context c) { c.getSystemService(NotificationManager.class).cancelAll(); }
    @android.annotation.SuppressLint("MissingPermission") // notificationsAllowed checks permission; notify also catches revocation.
    static void show(Context c,Medicine m,String time,String date,boolean test) {
        if(!notificationsAllowed(c)) return;
        String tag=test?"test":tag(m.id,time,date);
        PendingIntent open=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        String details=m.dose.trim().isEmpty()?m.name:m.name+" · "+m.dose;
        if(!m.note.trim().isEmpty()) details+="\n"+m.note;
        Notification.Builder b=new Notification.Builder(c,CHANNEL)
            .setSmallIcon(nl.ardio.medicijnmoment.R.drawable.ic_pill)
            .setContentTitle(test?"Testmelding MedicijnMoment":"Tijd voor je medicijnen · "+time)
            .setContentText(test?"Je meldingen werken. Dit is geen innamemoment.":details)
            .setStyle(new Notification.BigTextStyle().bigText(test?"Je meldingen werken. Dit is geen innamemoment.":details))
            .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE)
            .setCategory(Notification.CATEGORY_REMINDER);
        if(!test) {
            Intent action=new Intent(c,ReminderReceiver.class).setAction("TAKEN")
                .setData(android.net.Uri.parse("medicijnmoment://taken/"+android.net.Uri.encode(tag)))
                .putExtra("id",m.id).putExtra("time",time).putExtra("date",date);
            PendingIntent p=PendingIntent.getBroadcast(c,0,action,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
            b.addAction(new Notification.Action.Builder(null,"Ingenomen",p).build());
        }
        try { c.getSystemService(NotificationManager.class).notify(tag,0,b.build()); }
        catch(SecurityException ignored) { /* Permission may be revoked while receiving an alarm. */ }
    }
}
