package info.nightscout.androidaps.plugins.general.automation.triggers;

import android.app.Activity;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.FragmentManager;

import com.google.common.base.Optional;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.general.automation.elements.Comparator;
import info.nightscout.androidaps.utils.DateUtil;
import info.nightscout.androidaps.utils.JsonHelper;
import info.nightscout.androidaps.utils.T;


// Trigger for time range ( Time of day actually )

public class TriggerTimeOfDay extends Trigger {
    private static Logger log = LoggerFactory.getLogger(L.AUTOMATION);
    
    // in minutes since midnight 6- means 1AM 
    private int start;
    private int end;
    private Comparator comparator = new Comparator();


    public TriggerTimeOfDay() {
        
        start = getMinSinceMidnight(DateUtil.now());
        end = getMinSinceMidnight(DateUtil.now());
    }

    private TriggerTimeOfDay(TriggerTimeOfDay triggerTimeOfDay) {
        super();
        lastRun = triggerTimeOfDay.lastRun;
        start = triggerTimeOfDay.start;
        end = triggerTimeOfDay.end;
    }

    @Override
    public boolean shouldRun() {
        int currentMinSinceMidnight = getMinSinceMidnight(System.currentTimeMillis());

        if (lastRun > DateUtil.now() - T.mins(5).msecs())
            return false;

        boolean doRun = false;
        if ( start < end && start < currentMinSinceMidnight && currentMinSinceMidnight < end)
            doRun = true;

        // handle cases like 10PM to 6AM
        else if ( start > end && (start < currentMinSinceMidnight || currentMinSinceMidnight < end))
            doRun = true;

        if (doRun) {
            if (L.isEnabled(L.AUTOMATION))
                log.debug("Ready for execution: " + friendlyDescription());
            return true;
        }
        return false;
    }

    @Override
    public String toJSON() {
        JSONObject object = new JSONObject();
        JSONObject data = new JSONObject();

        // check for too big values
        if (start > 1440)
            start = getMinSinceMidnight(start);
        if (end > 1440)
            end = getMinSinceMidnight(end);

        try {
            data.put("start", getMinSinceMidnight(start));
            data.put("end", getMinSinceMidnight(end));
            data.put("lastRun", lastRun);
            object.put("type", TriggerTimeOfDay.class.getName());
            object.put("data", data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        log.debug(object.toString());
        return object.toString();
    }

    @Override
    TriggerTimeOfDay fromJSON(String data) {
        JSONObject o;
        try {
            o = new JSONObject(data);
            lastRun = JsonHelper.safeGetLong(o, "lastRun");
            start = JsonHelper.safeGetInt(o, "start");
            end = JsonHelper.safeGetInt(o, "end");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public int friendlyName() {
        return R.string.time_of_day;
    }

    @Override
    public String friendlyDescription() {
        return MainApp.gs(R.string.tod_value, DateUtil.timeString(toMilis(start)), DateUtil.timeString(toMilis(end)));
    }

    @Override
    public Optional<Integer> icon() {
        return Optional.of(R.drawable.ic_access_alarm_24dp);
    }

    TriggerTimeOfDay start(int start) {
        this.start = start;
        return this;
    }

    TriggerTimeOfDay lastRun(long lastRun) {
        this.lastRun = lastRun;
        return this;
    }

    @Override
    public Trigger duplicate() {
        return new TriggerTimeOfDay(this);
    }

    long toMilis(long minutesSinceMidnight) {
        long hours =  minutesSinceMidnight / 60;//hours
        long minutes = minutesSinceMidnight % 60;//hours
        return (hours*60*60*1000)+(minutes*60*1000);
    }

    int getMinSinceMidnight(long time) {
        // if passed argument is smaller than 1440 ( 24 h * 60 min ) that value is already converted
        if (time < 1441)
            return (int) time;
        Date date = new Date(time);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date);
        return (calendar.get(Calendar.HOUR_OF_DAY) * 60) + calendar.get(Calendar.MINUTE);
    }

    int getStart(){
        return start;
    }

    int getEnd(){
        return end;
    }

    @Override
    public void generateDialog(LinearLayout root, FragmentManager fragmentManager) {
        TextView label = new TextView(root.getContext());
        TextView startButton = new TextView(root.getContext());
        TextView endButton = new TextView(root.getContext());

        startButton.setText(DateUtil.timeString(toMilis(start)));
        endButton.setText(MainApp.gs(R.string.and) + " " + DateUtil.timeString(toMilis(end)));

        startButton.setOnClickListener(view -> {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(toMilis(start));
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view12, hourOfDay, minute, second) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        start = getMinSinceMidnight(calendar.getTimeInMillis());
                        startButton.setText(DateUtil.timeString(start));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(root.getContext())
            );
            tpd.setThemeDark(true);
            tpd.dismissOnPause(true);
            Activity a = scanForActivity(root.getContext());
            if (a != null)
                tpd.show(a.getFragmentManager(), "TimePickerDialog");
        });
        endButton.setOnClickListener(view -> {
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(toMilis(end));
            TimePickerDialog tpd = TimePickerDialog.newInstance(
                    (view12, hourOfDay, minute, second) -> {
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        calendar.set(Calendar.MINUTE, minute);
                        end = getMinSinceMidnight(calendar.getTimeInMillis());
                        endButton.setText(MainApp.gs(R.string.and) + " " + DateUtil.timeString(end));
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(root.getContext())
            );
            tpd.setThemeDark(true);
            tpd.dismissOnPause(true);
            Activity a = scanForActivity(root.getContext());
            if (a != null)
                tpd.show(a.getFragmentManager(), "TimePickerDialog");
        });

        int px = MainApp.dpToPx(10);
        label.setText(MainApp.gs(R.string.thanspecifiedtime));
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        startButton.setPadding(px, px, px, px);
        endButton.setPadding(px, px, px, px);

        LinearLayout l = new LinearLayout(root.getContext());
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        l.addView(label);
        l.addView(startButton);
        l.addView(endButton);
        root.addView(l);
    }
}
