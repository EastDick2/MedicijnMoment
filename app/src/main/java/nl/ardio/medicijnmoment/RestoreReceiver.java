package nl.ardio.medicijnmoment;
import android.content.*;
public class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i) { Reminders.schedule(c); }
}
