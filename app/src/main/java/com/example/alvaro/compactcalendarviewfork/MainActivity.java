package com.example.alvaro.compactcalendarviewfork;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView;
import com.example.alvaro.compactcalendarviewfork.calendarview.domain.Event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CompactCalendarView mCompactCalendarViewMonthly;
    private CompactCalendarView mCompactCalendarViewWeekly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date maxDate = new Date();
        Date minDate = new Date();
        try {
            minDate = sdf.parse("2016-11-04");
            maxDate = sdf.parse("2016-11-24");
        } catch (ParseException e) {
            e.printStackTrace();
        }

//        mCompactCalendarViewMonthly = (CompactCalendarView) findViewById(R.id.compactcalendar_view_monthly);
//        mCompactCalendarViewWeekly = (CompactCalendarView) findViewById(R.id.compactcalendar_view_weekly);

//        addEvents(10, 2016);

//        mCompactCalendarViewMonthly.addInactiveDays(Calendar.TUESDAY);
//        mCompactCalendarViewMonthly.setMinDateCalendar(minDate);
//        mCompactCalendarViewMonthly.shouldScrollMonth(false);
//        mCompactCalendarViewMonthly.setMaxDateCalendar(maxDate);

//        mCompactCalendarViewWeekly.addInactiveDays(Calendar.MONDAY, Calendar.TUESDAY);



    }

    private void addEvents(int month, int year) {
        Calendar currentCalender = Calendar.getInstance();
        currentCalender.set(Calendar.DAY_OF_MONTH, 1);
//        currentCalender.setFirstDayOfWeek(Calendar.MONDAY);
        Date firstDayOfMonth = currentCalender.getTime();
        for (int i = 0; i < 20; i++) {
            currentCalender.setTime(firstDayOfMonth);
            if (month > -1) {
                currentCalender.set(Calendar.MONTH, month);
            }
            if (year > -1) {
                currentCalender.set(Calendar.ERA, GregorianCalendar.AD);
                currentCalender.set(Calendar.YEAR, year);
            }
            currentCalender.add(Calendar.DATE, i);
//            setToMidnight(currentCalender);
            long timeInMillis = currentCalender.getTimeInMillis();

            List<Event> events = getEvents(timeInMillis, i);

            mCompactCalendarViewWeekly.addEvents(events);
            mCompactCalendarViewMonthly.addEvents(events);
        }
    }

    private List<Event> getEvents(long timeInMillis, int day) {
        if (day < 2) {
            return Arrays.asList(new Event(Color.argb(255, 169, 68, 65), timeInMillis, "Event at " + new Date(timeInMillis)));
        } else if ( day > 2 && day <= 6) {
            return Arrays.asList(
                    new Event(ContextCompat.getColor(this, R.color.status_missing), timeInMillis, "Event at " + new Date(timeInMillis)));
        } else {
            return Arrays.asList(
                    new Event(ContextCompat.getColor(this, R.color.status_fully_approved), timeInMillis, "Event at " + new Date(timeInMillis)));
        }
    }
}
