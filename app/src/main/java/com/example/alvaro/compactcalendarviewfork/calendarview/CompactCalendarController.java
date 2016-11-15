package com.example.alvaro.compactcalendarviewfork.calendarview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.widget.OverScroller;

import com.example.alvaro.compactcalendarviewfork.R;
import com.example.alvaro.compactcalendarviewfork.calendarview.domain.Event;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.FILL_LARGE_INDICATOR;
import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.MONTHLY;
import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.NO_FILL_LARGE_INDICATOR;
import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.SMALL_INDICATOR;
import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.WEEKLY;


class CompactCalendarController {

    private final String TAG = CompactCalendarController.class.getSimpleName();

    public boolean scroll = true;

    public static final int IDLE = 0;
    public static final int EXPOSE_CALENDAR_ANIMATION = 1;
    public static final int EXPAND_COLLAPSE_CALENDAR = 2;
    public static final int ANIMATE_INDICATORS = 3;
    private static final int VELOCITY_UNIT_PIXELS_PER_SECOND = 1000;
    private static final int LAST_FLING_THRESHOLD_MILLIS = 300;
    private static final int DAYS_IN_WEEK = 7;
    private static final float SNAP_VELOCITY_DIP_PER_SECOND = 400;
    private static final float ANIMATION_SCREEN_SET_DURATION_MILLIS = 700;

    private int eventIndicatorStyle = SMALL_INDICATOR;
    private boolean currentDayIndicator = true;
    private int currentDayIndicatorStyle = FILL_LARGE_INDICATOR;
    private int currentSelectedDayIndicatorStyle = FILL_LARGE_INDICATOR;
    private boolean defaultSelectedPresentDay = true;
    private boolean inactiveWeekend = false;
    private int calendarFormat = MONTHLY;
    private int paddingWidth = 40;
    private int paddingHeight = 40;
    private int textHeight;
    private int textWidth;
    private int widthPerDay;
    private int monthsScrolledSoFar;
    private int heightPerDay;
    private int textSize = 30;
    private int width;
    private int height;
    private int paddingRight;
    private int paddingLeft;
    private int maximumVelocity;
    private int densityAdjustedSnapVelocity;
    private int distanceThresholdForAutoScroll;
    private int targetHeight;
    private int animationStatus = 0;
    private float xIndicatorOffset;
    private float multiDayIndicatorStrokeWidth;
    private float bigCircleIndicatorRadius;
    private float smallIndicatorRadius;
    private float growFactor = 0f;
    private float screenDensity = 1;
    private float growfactorIndicator;
    private float distanceX;
    private long lastAutoScrollFromFling;

    private boolean shouldShowMondayAsFirstDay = true;
    private boolean useThreeLetterAbbreviation = false;
    private boolean isSmoothScrolling;
    private boolean isScrolling;
    private boolean shouldDrawDaysHeader = true;
    private boolean shouldDrawIndicatorsBelowSelectedDays = false;

    private CompactCalendarView.CompactCalendarViewListener listener;
    private VelocityTracker velocityTracker = null;
    private Direction currentDirection = Direction.NONE;
    private Date currentDate = new Date();
    private Locale locale;
    public Calendar currentCalender;
    private Calendar todayCalender;
    private Calendar calendarWithFirstDay;
    private Calendar eventsCalendar;
    private Calendar maxDateCalendar;
    private Calendar minDateCalendar;
    private EventsContainer eventsContainer;
    private PointF accumulatedScrollOffset = new PointF();
    private OverScroller scroller;
    private Paint dayPaint = new Paint();
    private Paint background = new Paint();
    private Rect textSizeRect;
    private String[] dayColumnNames;
    private List<Integer> inactiveDays = new ArrayList<>();

    // colors
    private int multiEventIndicatorColor;
    private int currentDayBackgroundColor;
    private int calenderTextColor;
    private int currentSelectedDayBackgroundColor;
    private int calenderBackgroundColor = Color.WHITE;
    private boolean shouldScroll = true;
    private TimeZone timeZone;

    private enum Direction {
        NONE, HORIZONTAL, VERTICAL
    }

    CompactCalendarController(Paint dayPaint, OverScroller scroller, Rect textSizeRect, AttributeSet attrs,
                              Context context, int currentDayBackgroundColor, int calenderTextColor,
                              int currentSelectedDayBackgroundColor, VelocityTracker velocityTracker,
                              int multiEventIndicatorColor,
                              Locale locale, TimeZone timeZone) {
        this.dayPaint = dayPaint;
        this.scroller = scroller;
        this.textSizeRect = textSizeRect;
        this.currentDayBackgroundColor = currentDayBackgroundColor;
        this.calenderTextColor = calenderTextColor;
        this.currentSelectedDayBackgroundColor = currentSelectedDayBackgroundColor;
        this.velocityTracker = velocityTracker;
        this.multiEventIndicatorColor = multiEventIndicatorColor;
        this.locale = locale;
        this.timeZone = timeZone;
        loadAttributes(attrs, context);
        this.eventsContainer = new EventsContainer(Calendar.getInstance(), calendarFormat);
        init(context);
    }

    private void loadAttributes(AttributeSet attrs, Context context) {
        if (attrs != null && context != null) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CompactCalendarView, 0, 0);
            try {
                currentDayBackgroundColor = typedArray.getColor(R.styleable.CompactCalendarView_compactCalendarCurrentDayBackgroundColor, currentDayBackgroundColor);
                calenderTextColor = typedArray.getColor(R.styleable.CompactCalendarView_compactCalendarTextColor, calenderTextColor);
                currentSelectedDayBackgroundColor = typedArray.getColor(R.styleable.CompactCalendarView_compactCalendarCurrentSelectedDayBackgroundColor, currentSelectedDayBackgroundColor);
                calenderBackgroundColor = typedArray.getColor(R.styleable.CompactCalendarView_compactCalendarBackgroundColor, calenderBackgroundColor);
                multiEventIndicatorColor = typedArray.getColor(R.styleable.CompactCalendarView_compactCalendarMultiEventIndicatorColor, multiEventIndicatorColor);
                textSize = typedArray.getDimensionPixelSize(R.styleable.CompactCalendarView_compactCalendarTextSize,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSize, context.getResources().getDisplayMetrics()));
                targetHeight = typedArray.getDimensionPixelSize(R.styleable.CompactCalendarView_compactCalendarTargetHeight,
                        (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, targetHeight, context.getResources().getDisplayMetrics()));
                eventIndicatorStyle = typedArray.getInt(R.styleable.CompactCalendarView_compactCalendarEventIndicatorStyle, SMALL_INDICATOR);
                currentDayIndicator = typedArray.getBoolean(R.styleable.CompactCalendarView_compactCalendarCurrentDayIndicator, true);
                currentDayIndicatorStyle = typedArray.getInt(R.styleable.CompactCalendarView_compactCalendarCurrentDayIndicatorStyle, FILL_LARGE_INDICATOR);
                currentSelectedDayIndicatorStyle = typedArray.getInt(R.styleable.CompactCalendarView_compactCalendarCurrentSelectedDayIndicatorStyle, FILL_LARGE_INDICATOR);
                defaultSelectedPresentDay = typedArray.getBoolean(R.styleable.CompactCalendarView_compactCalendarDefaultSelectedPresentDay, true);
                calendarFormat = typedArray.getInt(R.styleable.CompactCalendarView_compactCalendarFormat, MONTHLY);
                inactiveWeekend = typedArray.getBoolean(R.styleable.CompactCalendarView_compactCalendarInactiveWeekend, false);
            } finally {
                typedArray.recycle();
            }
        }
    }

    private void init(Context context) {
        currentCalender = Calendar.getInstance(timeZone, locale);
        todayCalender = Calendar.getInstance(timeZone, locale);
        calendarWithFirstDay = Calendar.getInstance(timeZone, locale);
        eventsCalendar = Calendar.getInstance(timeZone, locale);

        // make setMinimalDaysInFirstWeek same across android versions
        eventsCalendar.setMinimalDaysInFirstWeek(1);
        calendarWithFirstDay.setMinimalDaysInFirstWeek(1);
        todayCalender.setMinimalDaysInFirstWeek(1);
        currentCalender.setMinimalDaysInFirstWeek(1);

        eventsCalendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendarWithFirstDay.setFirstDayOfWeek(Calendar.MONDAY);
        todayCalender.setFirstDayOfWeek(Calendar.MONDAY);
        currentCalender.setFirstDayOfWeek(Calendar.MONDAY);

        setUseWeekDayAbbreviation(false);
        dayPaint.setTextAlign(Paint.Align.CENTER);
        dayPaint.setStyle(Paint.Style.STROKE);
        dayPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        dayPaint.setTypeface(Typeface.SANS_SERIF);
        dayPaint.setTextSize(textSize);
        dayPaint.setColor(calenderTextColor);
        dayPaint.getTextBounds("31", 0, "31".length(), textSizeRect);
        textHeight = textSizeRect.height() * 3;
        textWidth = textSizeRect.width() * 2;

        todayCalender.setTime(currentDate);
        setToMidnight(todayCalender);

        currentCalender.setTime(currentDate);
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 0);

        if (!defaultSelectedPresentDay && !currentDayIndicator) {
            setCalenderToFirstActiveDay(currentCalender, currentDate, -monthsScrolledSoFar, 0);
        }

        if (inactiveWeekend) {
            addInactiveDay(Calendar.SATURDAY);
            addInactiveDay(Calendar.SUNDAY);
        }

        initScreenDensityRelatedValues(context);

        xIndicatorOffset = 3.5f * screenDensity;

        //scale small indicator by screen density
        smallIndicatorRadius = 2.5f * screenDensity;

        //just set a default growFactor to draw full calendar when initialised
        growFactor = Integer.MAX_VALUE;
    }

    private void initScreenDensityRelatedValues(Context context) {
        if (context != null) {
            screenDensity = context.getResources().getDisplayMetrics().density;
            final ViewConfiguration configuration = ViewConfiguration
                    .get(context);
            densityAdjustedSnapVelocity = (int) (screenDensity * SNAP_VELOCITY_DIP_PER_SECOND);
            maximumVelocity = configuration.getScaledMaximumFlingVelocity();

            final DisplayMetrics dm = context.getResources().getDisplayMetrics();
            multiDayIndicatorStrokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, dm);
        }
    }

    private void setCalenderToFirstDay(Calendar calendarWithFirstDay, Date currentDate, int scrollOffset, int dateOffset) {
        if (calendarFormat == MONTHLY) {
            setMonthOffset(calendarWithFirstDay, currentDate, scrollOffset, dateOffset);
            calendarWithFirstDay.set(Calendar.DAY_OF_MONTH, 1);
        } else {
            setWeekOffset(calendarWithFirstDay, currentDate, scrollOffset, dateOffset);
            calendarWithFirstDay.set(Calendar.DAY_OF_WEEK, this.calendarWithFirstDay.getFirstDayOfWeek());
        }
    }

    private void setCalenderToFirstActiveDay(Calendar calendarWithFirstDay, Date currentDate, int scrollOffset, int dateOffset) {

        boolean activePeriod = true;
        Calendar currentCalendar = Calendar.getInstance();
        currentCalendar.setTime(currentDate);

        if (calendarFormat == MONTHLY) {

            setMonthOffset(calendarWithFirstDay, currentDate, scrollOffset, dateOffset);
            calendarWithFirstDay.set(Calendar.DAY_OF_MONTH, 1);

            currentCalendar.set(Calendar.DAY_OF_MONTH, currentCalendar.getMaximum(Calendar.DAY_OF_MONTH));
            if (minDateCalendar != null && currentCalendar.getTimeInMillis() <= minDateCalendar.getTimeInMillis()) {
                while (inactiveDays.size() != 7 && isInactiveDate(calendarWithFirstDay)) {
                    calendarWithFirstDay.add(Calendar.DATE, 1);
                }
            }

        } else {
            setWeekOffset(calendarWithFirstDay, currentDate, scrollOffset, dateOffset);
            calendarWithFirstDay.set(Calendar.DAY_OF_WEEK, this.calendarWithFirstDay.getFirstDayOfWeek());
        }

        while (inactiveDays.size() != 7 && isInactiveDate(calendarWithFirstDay)) {
            calendarWithFirstDay.add(Calendar.DATE, 1);
        }
    }

    private void setMonthOffset(Calendar calendarWithFirstDayOfMonth, Date currentDate, int scrollOffset, int monthOffset) {
        calendarWithFirstDayOfMonth.setTime(currentDate);
        calendarWithFirstDayOfMonth.add(Calendar.MONTH, scrollOffset + monthOffset);
        calendarWithFirstDayOfMonth.set(Calendar.HOUR_OF_DAY, 0);
        calendarWithFirstDayOfMonth.set(Calendar.MINUTE, 0);
        calendarWithFirstDayOfMonth.set(Calendar.SECOND, 0);
        calendarWithFirstDayOfMonth.set(Calendar.MILLISECOND, 0);
    }

    private void setWeekOffset(Calendar calendarWithFirstDayOfWeek, Date currentDate, int scrollOffset, int weekOffset) {
        calendarWithFirstDayOfWeek.setTime(currentDate);
        calendarWithFirstDayOfWeek.add(Calendar.WEEK_OF_YEAR, scrollOffset + weekOffset);
        calendarWithFirstDayOfWeek.set(Calendar.HOUR_OF_DAY, 0);
        calendarWithFirstDayOfWeek.set(Calendar.MINUTE, 0);
        calendarWithFirstDayOfWeek.set(Calendar.SECOND, 0);
        calendarWithFirstDayOfWeek.set(Calendar.MILLISECOND, 0);
    }

    void shouldDrawIndicatorsBelowSelectedDays(boolean shouldDrawIndicatorsBelowSelectedDays) {
        this.shouldDrawIndicatorsBelowSelectedDays = shouldDrawIndicatorsBelowSelectedDays;
    }

    void setCurrentDayIndicatorStyle(int currentDayIndicatorStyle) {
        this.currentDayIndicatorStyle = currentDayIndicatorStyle;
    }

    void setEventIndicatorStyle(int eventIndicatorStyle) {
        this.eventIndicatorStyle = eventIndicatorStyle;
    }

    void setCurrentSelectedDayIndicatorStyle(int currentSelectedDayIndicatorStyle) {
        this.currentSelectedDayIndicatorStyle = currentSelectedDayIndicatorStyle;
    }

    void setTargetHeight(int targetHeight) {
        this.targetHeight = targetHeight;
    }

    float getScreenDensity() {
        return screenDensity;
    }

    float getDayIndicatorRadius() {
        return bigCircleIndicatorRadius;
    }

    void setGrowFactorIndicator(float growfactorIndicator) {
        this.growfactorIndicator = growfactorIndicator;
    }

    float getGrowFactorIndicator() {
        return growfactorIndicator;
    }

    void setAnimationStatus(int animationStatus) {
        this.animationStatus = animationStatus;
    }

    int getTargetHeight() {
        return targetHeight;
    }

    int getWidth() {
        return width;
    }

    void setListener(CompactCalendarView.CompactCalendarViewListener listener) {
        this.listener = listener;
    }

    void removeAllEvents() {
        eventsContainer.removeAllEvents();
    }

    void setShouldShowMondayAsFirstDay(boolean shouldShowMondayAsFirstDay) {
        this.shouldShowMondayAsFirstDay = shouldShowMondayAsFirstDay;
        setUseWeekDayAbbreviation(useThreeLetterAbbreviation);
        if (shouldShowMondayAsFirstDay) {
            eventsCalendar.setFirstDayOfWeek(Calendar.MONDAY);
            calendarWithFirstDay.setFirstDayOfWeek(Calendar.MONDAY);
            todayCalender.setFirstDayOfWeek(Calendar.MONDAY);
            currentCalender.setFirstDayOfWeek(Calendar.MONDAY);
        } else {
            eventsCalendar.setFirstDayOfWeek(Calendar.SUNDAY);
            calendarWithFirstDay.setFirstDayOfWeek(Calendar.SUNDAY);
            todayCalender.setFirstDayOfWeek(Calendar.SUNDAY);
            currentCalender.setFirstDayOfWeek(Calendar.SUNDAY);
        }
    }

    void setCurrentSelectedDayBackgroundColor(int currentSelectedDayBackgroundColor) {
        this.currentSelectedDayBackgroundColor = currentSelectedDayBackgroundColor;
    }

    void setCalenderBackgroundColor(int calenderBackgroundColor) {
        this.calenderBackgroundColor = calenderBackgroundColor;
    }

    void setCurrentDayBackgroundColor(int currentDayBackgroundColor) {
        this.currentDayBackgroundColor = currentDayBackgroundColor;
    }

    void showNext() {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentCalender.getTime(), 0, 1);
        setCurrentDate(calendarWithFirstDay.getTime());
        performMonthScrollCallback();
    }

    void showPrevious() {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentCalender.getTime(), 0, -1);
        setCurrentDate(calendarWithFirstDay.getTime());
        performMonthScrollCallback();
    }

    void setLocale(TimeZone timeZone, Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("Locale cannot be null.");
        }
        if (timeZone == null) {
            throw new IllegalArgumentException("TimeZone cannot be null.");
        }
        this.locale = locale;
        this.timeZone = timeZone;
        this.eventsContainer = new EventsContainer(Calendar.getInstance(this.timeZone, this.locale), calendarFormat);
        // passing null will not re-init density related values - and that's ok
        init(null);
    }

    void setUseWeekDayAbbreviation(boolean useThreeLetterAbbreviation) {
        this.useThreeLetterAbbreviation = useThreeLetterAbbreviation;
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(locale);
        String[] dayNames = dateFormatSymbols.getShortWeekdays();
        if (dayNames == null) {
            throw new IllegalStateException("Unable to determine weekday names from default locale");
        }
        if (dayNames.length != 8) {
            throw new IllegalStateException("Expected weekday names from default locale to be of size 7 but: "
                    + Arrays.toString(dayNames) + " with size " + dayNames.length + " was returned.");
        }

        if (useThreeLetterAbbreviation) {
            if (!shouldShowMondayAsFirstDay) {
                this.dayColumnNames = new String[]{dayNames[1], dayNames[2], dayNames[3], dayNames[4], dayNames[5], dayNames[6], dayNames[7]};
            } else {
                this.dayColumnNames = new String[]{dayNames[2], dayNames[3], dayNames[4], dayNames[5], dayNames[6], dayNames[7], dayNames[1]};
            }
        } else {
            if (!shouldShowMondayAsFirstDay) {
                this.dayColumnNames = new String[]{dayNames[1].substring(0, 1), dayNames[2].substring(0, 1),
                        dayNames[3].substring(0, 1), dayNames[4].substring(0, 1), dayNames[5].substring(0, 1), dayNames[6].substring(0, 1), dayNames[7].substring(0, 1)};
            } else {
                this.dayColumnNames = new String[]{dayNames[2].substring(0, 1), dayNames[3].substring(0, 1),
                        dayNames[4].substring(0, 1), dayNames[5].substring(0, 1), dayNames[6].substring(0, 1), dayNames[7].substring(0, 1), dayNames[1].substring(0, 1)};
            }
        }
    }

    void setDayColumnNames(String[] dayColumnNames) {
        if (dayColumnNames == null || dayColumnNames.length != 7) {
            throw new IllegalArgumentException("Column names cannot be null and must contain a value for each day of the week");
        }
        this.dayColumnNames = dayColumnNames;
    }

    void setShouldDrawDaysHeader(boolean shouldDrawDaysHeader) {
        this.shouldDrawDaysHeader = shouldDrawDaysHeader;
    }

    void onMeasure(int width, int height, int paddingRight, int paddingLeft) {
        widthPerDay = (width) / DAYS_IN_WEEK;
        heightPerDay = targetHeight > 0 ? targetHeight / 7 : height / 7;
        this.width = width;
        this.distanceThresholdForAutoScroll = (int) (width * 0.50);
        this.height = height;
        this.paddingRight = paddingRight;
        this.paddingLeft = paddingLeft;

        //makes easier to find radius
        bigCircleIndicatorRadius = getInterpolatedBigCircleIndicator();

        // scale the selected day indicators slightly so that event indicators can be drawn below
        bigCircleIndicatorRadius = shouldDrawIndicatorsBelowSelectedDays && eventIndicatorStyle == CompactCalendarView.SMALL_INDICATOR ? bigCircleIndicatorRadius * 0.85f : bigCircleIndicatorRadius;
    }

    //assume square around each day of width and height = heightPerDay and get diagonal line length
    //interpolate height and radius
    //https://en.wikipedia.org/wiki/Linear_interpolation
    private float getInterpolatedBigCircleIndicator() {
        float x0 = textSizeRect.height();
        float x1 = heightPerDay; // take into account indicator offset
        float x = (x1 + textSizeRect.height()) / 2f; // pick a point which is almost half way through heightPerDay and textSizeRect
        double y1 = 0.5 * Math.sqrt((x1 * x1) + (x1 * x1));
        double y0 = 0.5 * Math.sqrt((x0 * x0) + (x0 * x0));

        return (float) (y0 + ((y1 - y0) * ((x - x0) / (x1 - x0))));
    }

    void onDraw(Canvas canvas) {
        paddingWidth = widthPerDay / 2;
        paddingHeight = heightPerDay / 2;
        calculateXPositionOffset();

        if (animationStatus == EXPOSE_CALENDAR_ANIMATION) {
            drawCalendarWhileAnimating(canvas);
        } else if (animationStatus == ANIMATE_INDICATORS) {
            drawCalendarWhileAnimatingIndicators(canvas);
        } else {
            drawCalenderBackground(canvas);
            drawScrollableCalender(canvas);
        }
    }

    private void drawCalendarWhileAnimatingIndicators(Canvas canvas) {
        dayPaint.setColor(calenderBackgroundColor);
        dayPaint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, growFactor, dayPaint);
        dayPaint.setStyle(Paint.Style.STROKE);
        dayPaint.setColor(Color.WHITE);
        drawScrollableCalender(canvas);
    }

    private void drawCalendarWhileAnimating(Canvas canvas) {
        background.setColor(calenderBackgroundColor);
        background.setStyle(Paint.Style.FILL);
        canvas.drawCircle(0, 0, growFactor, background);
        dayPaint.setStyle(Paint.Style.STROKE);
        dayPaint.setColor(Color.WHITE);
        drawScrollableCalender(canvas);
    }

    void onSingleTapConfirmed(MotionEvent e) {
        //Don't handle single tap the calendar is scrolling and is not stationary
        if (Math.abs(accumulatedScrollOffset.x) != Math.abs(width * monthsScrolledSoFar)) {
            return;
        }

        int dayColumn = Math.round((paddingLeft + e.getX() - paddingWidth - paddingRight) / widthPerDay);
        int dayRow = Math.round((e.getY() - paddingHeight) / heightPerDay);

        setCalenderToFirstDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 0);

        //Start Monday as day 1 and Sunday as day 7. Not Sunday as day 1 and Monday as day 2
        int firstDayOfMonth = getDayOfWeek(calendarWithFirstDay);

        int dayOfMonth = ((dayRow - 1) * 7 + dayColumn + 1) - firstDayOfMonth;

        if (dayOfMonth < calendarWithFirstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
                && dayOfMonth >= 0) {
            calendarWithFirstDay.add(Calendar.DATE, dayOfMonth);
            if (!isInactiveDate(calendarWithFirstDay)) {
                currentCalender.setTimeInMillis(calendarWithFirstDay.getTimeInMillis());
                performOnDayClickCallback(currentCalender.getTime());
            }
        }
    }

    private void performOnDayClickCallback(Date date) {
        if (listener != null) {
            listener.onDayClick(date);
        }
    }

    boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        //ignore scrolling callback if already smooth scrolling
        Log.d(TAG, "onScroll: " + monthsScrolledSoFar);
        if (isSmoothScrolling) {
            return true;
        }

        if (currentDirection == Direction.NONE) {
            if (Math.abs(distanceX) > Math.abs(distanceY)) {
                currentDirection = Direction.HORIZONTAL;
            } else {
                currentDirection = Direction.VERTICAL;
            }
        }

        isScrolling = true;
        this.distanceX = distanceX;
        return true;
    }

    boolean onTouch(MotionEvent event) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }

        velocityTracker.addMovement(event);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            if (!scroller.isFinished()) {
                scroller.abortAnimation();
            }
            isSmoothScrolling = false;

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            velocityTracker.addMovement(event);
            velocityTracker.computeCurrentVelocity(500);

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            handleHorizontalScrolling();
            velocityTracker.recycle();
            velocityTracker.clear();
            velocityTracker = null;
            isScrolling = false;
        }
        return false;
    }

    private void snapBackScroller() {
        float remainingScrollAfterFingerLifted1 = (accumulatedScrollOffset.x - (monthsScrolledSoFar * width));
        scroller.startScroll((int) accumulatedScrollOffset.x, 0, (int) -remainingScrollAfterFingerLifted1, 0);
    }

    private void handleHorizontalScrolling() {
        int velocityX = computeVelocity();
        handleSmoothScrolling(velocityX);

        currentDirection = Direction.NONE;
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 0);

        if (calendarFormat == MONTHLY && calendarWithFirstDay.get(Calendar.MONTH) != currentCalender.get(Calendar.MONTH)) {
            setCalenderToFirstActiveDay(currentCalender, currentDate, -monthsScrolledSoFar, 0);
        } else if (calendarFormat == WEEKLY && calendarWithFirstDay.get(Calendar.WEEK_OF_YEAR) != currentCalender.get(Calendar.WEEK_OF_YEAR)) {
            setCalenderToFirstActiveDay(currentCalender, currentDate, -monthsScrolledSoFar, 0);
        }
    }

    private int computeVelocity() {
        velocityTracker.computeCurrentVelocity(VELOCITY_UNIT_PIXELS_PER_SECOND, maximumVelocity);
        return (int) velocityTracker.getXVelocity();
    }

    private void handleSmoothScrolling(int velocityX) {
        int distanceScrolled = (int) (accumulatedScrollOffset.x - (width * monthsScrolledSoFar));
        Log.d(TAG, "handleSmoothScrolling: " + distanceScrolled);
        boolean isEnoughTimeElapsedSinceLastSmoothScroll = System.currentTimeMillis() - lastAutoScrollFromFling > LAST_FLING_THRESHOLD_MILLIS;
        if (velocityX > densityAdjustedSnapVelocity && isEnoughTimeElapsedSinceLastSmoothScroll) {
            scrollPreviousMonth();
        } else if (velocityX < -densityAdjustedSnapVelocity && isEnoughTimeElapsedSinceLastSmoothScroll) {
            scrollNextMonth();
        } else if (isScrolling && distanceScrolled > distanceThresholdForAutoScroll) {
            scrollPreviousMonth();
        } else if (isScrolling && distanceScrolled < -distanceThresholdForAutoScroll) {
            scrollNextMonth();
        } else {
            isSmoothScrolling = false;
            snapBackScroller();
        }
    }

    private void scrollNextMonth() {
        lastAutoScrollFromFling = System.currentTimeMillis();
        monthsScrolledSoFar = monthsScrolledSoFar - 1;
        performScroll();
        isSmoothScrolling = true;
        performMonthScrollCallback();
//        scroll = monthsScrolledSoFar > 3;
    }

    private void scrollPreviousMonth() {
        lastAutoScrollFromFling = System.currentTimeMillis();
        monthsScrolledSoFar = monthsScrolledSoFar + 1;
        performScroll();
        isSmoothScrolling = true;
        performMonthScrollCallback();
    }

    private void performMonthScrollCallback() {
        if (listener != null) {
            listener.onMonthScroll(getFirstDayOfCurrentMonth());
        }
    }

    private void performScroll() {
        int targetScroll = monthsScrolledSoFar * width;
        float remainingScrollAfterFingerLifted = targetScroll - accumulatedScrollOffset.x;
        scroller.startScroll((int) accumulatedScrollOffset.x, 0, (int) (remainingScrollAfterFingerLifted), 0,
                (int) (Math.abs((int) (remainingScrollAfterFingerLifted)) / (float) width * ANIMATION_SCREEN_SET_DURATION_MILLIS));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

            }
        }, (long) ANIMATION_SCREEN_SET_DURATION_MILLIS);
    }

    int getHeightPerDay() {
        return heightPerDay;
    }

    int getWeekNumberForCurrentMonth() {
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTime(currentDate);
        return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    Date getFirstDayOfCurrentMonth() {
        Calendar calendar = Calendar.getInstance(timeZone, locale);
        calendar.setTime(currentDate);
        calendar.add(Calendar.MONTH, -monthsScrolledSoFar);
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        setToMidnight(calendar);
        return calendar.getTime();
    }

    void setCurrentDate(Date dateTimeMonth) {
        distanceX = 0;
        monthsScrolledSoFar = 0;
        accumulatedScrollOffset.x = 0;
        scroller.startScroll(0, 0, 0, 0);
        currentDate = new Date(dateTimeMonth.getTime());
        currentCalender.setTime(currentDate);
        todayCalender = Calendar.getInstance(timeZone, locale);
        setToMidnight(currentCalender);
    }

    private void setToMidnight(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    void addEvent(Event event) {
        eventsContainer.addEvent(event);
    }

    void addEvents(List<Event> events) {
        eventsContainer.addEvents(events);
    }

    List<Event> getCalendarEventsFor(long epochMillis) {
        return eventsContainer.getEventsFor(epochMillis);
    }

    List<Event> getCalendarEventsForMonth(long epochMillis) {
        return eventsContainer.getEventsForMonth(epochMillis);
    }

    void removeEventsFor(long epochMillis) {
        eventsContainer.removeEventByEpochMillis(epochMillis);
    }

    void removeEvent(Event event) {
        eventsContainer.removeEvent(event);
    }

    void removeEvents(List<Event> events) {
        eventsContainer.removeEvents(events);
    }

    void setGrowProgress(float grow) {
        growFactor = grow;
    }

    float getGrowFactor() {
        return growFactor;
    }

    boolean onDown(MotionEvent e) {
        scroller.forceFinished(true);
        return true;
    }

    boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        scroller.forceFinished(true);
        return true;
    }

    boolean computeScroll() {
        if (scroller.computeScrollOffset()) {
            accumulatedScrollOffset.x = scroller.getCurrX();
            return true;
        }
        return false;
    }

    private void drawScrollableCalender(Canvas canvas) {
        drawPreviousMonth(canvas);

        drawCurrentMonth(canvas);

        drawNextMonth(canvas);
    }

    private void drawCalendar(Canvas canvas, Calendar monthToDrawCalender, int offset) {
        if (calendarFormat == MONTHLY) {
            drawMonth(canvas, monthToDrawCalender, offset);
        } else {
            drawWeek(canvas, monthToDrawCalender, offset);
        }
    }

    private void drawNextMonth(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 1);
        drawCalendar(canvas, calendarWithFirstDay, (width * (-monthsScrolledSoFar + 1)));
    }

    private void drawCurrentMonth(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 0);
        drawCalendar(canvas, calendarWithFirstDay, width * -monthsScrolledSoFar);
    }

    private void drawPreviousMonth(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, -1);
        drawCalendar(canvas, calendarWithFirstDay, (width * (-monthsScrolledSoFar - 1)));
    }

    private void drawNextWeek(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 1);
        drawWeek(canvas, calendarWithFirstDay, (width * (-monthsScrolledSoFar + 1)));
    }

    private void drawCurrentWeek(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, 0);
        drawWeek(canvas, calendarWithFirstDay, width * -monthsScrolledSoFar);
    }

    private void drawPreviousWeek(Canvas canvas) {
        setCalenderToFirstActiveDay(calendarWithFirstDay, currentDate, -monthsScrolledSoFar, -1);
        drawWeek(canvas, calendarWithFirstDay, (width * (-monthsScrolledSoFar - 1)));
    }

    private void calculateXPositionOffset() {
        if (currentDirection == Direction.HORIZONTAL) {
            accumulatedScrollOffset.x -= distanceX;
        }
    }

    private void drawCalenderBackground(Canvas canvas) {
        dayPaint.setColor(calenderBackgroundColor);
        dayPaint.setStyle(Paint.Style.FILL);
        canvas.drawRect(0, 0, width, height, dayPaint);
        dayPaint.setStyle(Paint.Style.STROKE);
        dayPaint.setColor(calenderTextColor);
    }

    void drawEventsWeekly(Canvas canvas, Calendar currentWeekToDrawCalender, int offset) {
        int currentWeek = currentWeekToDrawCalender.get(Calendar.WEEK_OF_YEAR);
        List<Events> uniqEvents = eventsContainer.getEventsForWeekAndYear(currentWeek, currentWeekToDrawCalender.get(Calendar.YEAR));

        boolean shouldDrawCurrentDayCircle = currentWeek == todayCalender.get(Calendar.MONTH);
        boolean shouldDrawSelectedDayCircle = currentWeek == currentCalender.get(Calendar.MONTH);

        int todayDayOfMonth = todayCalender.get(Calendar.DAY_OF_MONTH);
        int selectedDayOfMonth = currentCalender.get(Calendar.DAY_OF_MONTH);
        float indicatorOffset = bigCircleIndicatorRadius / 2;
        if (uniqEvents != null) {
            for (int i = 0; i < uniqEvents.size(); i++) {
                Events events = uniqEvents.get(i);
                long timeMillis = events.getTimeInMillis();
                eventsCalendar.setTimeInMillis(timeMillis);

                int dayOfWeek = getDayOfWeek(eventsCalendar) - 1;

                float xPosition = widthPerDay * dayOfWeek + paddingWidth + paddingLeft + accumulatedScrollOffset.x + offset - paddingRight;
                float yPosition = heightPerDay + paddingHeight;

                if (((animationStatus == EXPOSE_CALENDAR_ANIMATION || animationStatus == ANIMATE_INDICATORS) && xPosition >= growFactor) || yPosition >= growFactor) {
                    // only draw small event indicators if enough of the calendar is exposed
                    continue;
                } else if (animationStatus == EXPAND_COLLAPSE_CALENDAR && yPosition >= growFactor) {
                    // expanding animation, just draw event indicators if enough of the calendar is visible
                    continue;
                } else if (animationStatus == EXPOSE_CALENDAR_ANIMATION && (eventIndicatorStyle == FILL_LARGE_INDICATOR || eventIndicatorStyle == NO_FILL_LARGE_INDICATOR)) {
                    // Don't draw large indicators during expose animation, until animation is done
                    continue;
                }

                List<Event> eventsList = events.getEvents();
                int dayOfMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                boolean isSameDayAsCurrentDay = shouldDrawCurrentDayCircle && (todayDayOfMonth == dayOfMonth);
                boolean isCurrentSelectedDay = shouldDrawSelectedDayCircle && (selectedDayOfMonth == dayOfMonth);

                if (shouldDrawIndicatorsBelowSelectedDays || (!shouldDrawIndicatorsBelowSelectedDays && !isSameDayAsCurrentDay && !isCurrentSelectedDay) || animationStatus == EXPOSE_CALENDAR_ANIMATION) {
                    if (eventIndicatorStyle == FILL_LARGE_INDICATOR || eventIndicatorStyle == NO_FILL_LARGE_INDICATOR) {
                        Event event = eventsList.get(0);
                        drawEventIndicatorCircle(canvas, xPosition, yPosition, event.getColor());
                    } else {
                        yPosition += indicatorOffset;
                        // offset event indicators to draw below selected day indicators
                        // this makes sure that they do no overlap
                        if (shouldDrawIndicatorsBelowSelectedDays && (isSameDayAsCurrentDay || isCurrentSelectedDay)) {
                            yPosition += indicatorOffset;
                        }

                        if (eventsList.size() >= 3) {
                            drawEventsWithPlus(canvas, xPosition, yPosition, eventsList);
                        } else if (eventsList.size() == 2) {
                            drawTwoEvents(canvas, xPosition, yPosition, eventsList);
                        } else if (eventsList.size() == 1) {
                            drawSingleEvent(canvas, xPosition, yPosition, eventsList);
                        }
                    }
                }
            }
        }
    }

    void drawEventsMonthly(Canvas canvas, Calendar currentMonthToDrawCalender, int offset) {
        int currentMonth = currentMonthToDrawCalender.get(Calendar.MONTH);
        List<Events> uniqEvents = eventsContainer.getEventsForMonthAndYear(currentMonth, currentMonthToDrawCalender.get(Calendar.YEAR));

        boolean shouldDrawCurrentDayCircle = currentMonth == todayCalender.get(Calendar.MONTH);
        boolean shouldDrawSelectedDayCircle = currentMonth == currentCalender.get(Calendar.MONTH);

        int todayDayOfMonth = todayCalender.get(Calendar.DAY_OF_MONTH);
        int selectedDayOfMonth = currentCalender.get(Calendar.DAY_OF_MONTH);
        float indicatorOffset = bigCircleIndicatorRadius / 2;
        if (uniqEvents != null) {
            for (int i = 0; i < uniqEvents.size(); i++) {
                Events events = uniqEvents.get(i);
                long timeMillis = events.getTimeInMillis();
                eventsCalendar.setTimeInMillis(timeMillis);

                int dayOfWeek = getDayOfWeek(eventsCalendar) - 1;

                int weekNumberForMonth = eventsCalendar.get(Calendar.WEEK_OF_MONTH);
                float xPosition = widthPerDay * dayOfWeek + paddingWidth + paddingLeft + accumulatedScrollOffset.x + offset - paddingRight;
                float yPosition = weekNumberForMonth * heightPerDay + paddingHeight;

                if (((animationStatus == EXPOSE_CALENDAR_ANIMATION || animationStatus == ANIMATE_INDICATORS) && xPosition >= growFactor) || yPosition >= growFactor) {
                    // only draw small event indicators if enough of the calendar is exposed
                    continue;
                } else if (animationStatus == EXPAND_COLLAPSE_CALENDAR && yPosition >= growFactor) {
                    // expanding animation, just draw event indicators if enough of the calendar is visible
                    continue;
                } else if (animationStatus == EXPOSE_CALENDAR_ANIMATION && (eventIndicatorStyle == FILL_LARGE_INDICATOR || eventIndicatorStyle == NO_FILL_LARGE_INDICATOR)) {
                    // Don't draw large indicators during expose animation, until animation is done
                    continue;
                }

                List<Event> eventsList = events.getEvents();
                int dayOfMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                boolean isSameDayAsCurrentDay = shouldDrawCurrentDayCircle && (todayDayOfMonth == dayOfMonth);
                boolean isCurrentSelectedDay = shouldDrawSelectedDayCircle && (selectedDayOfMonth == dayOfMonth);

                if (!currentDayIndicator || shouldDrawIndicatorsBelowSelectedDays || (!shouldDrawIndicatorsBelowSelectedDays && !isSameDayAsCurrentDay && !isCurrentSelectedDay) || animationStatus == EXPOSE_CALENDAR_ANIMATION) {
                    if (eventIndicatorStyle == FILL_LARGE_INDICATOR || eventIndicatorStyle == NO_FILL_LARGE_INDICATOR) {
                        Event event = eventsList.get(0);
                        drawEventIndicatorCircle(canvas, xPosition, yPosition, event.getColor());
                    } else {
                        yPosition += indicatorOffset;
                        // offset event indicators to draw below selected day indicators
                        // this makes sure that they do no overlap
                        if (shouldDrawIndicatorsBelowSelectedDays && (isSameDayAsCurrentDay || isCurrentSelectedDay)) {
                            yPosition += indicatorOffset;
                        }

                        if (eventsList.size() >= 3) {
                            drawEventsWithPlus(canvas, xPosition, yPosition, eventsList);
                        } else if (eventsList.size() == 2) {
                            drawTwoEvents(canvas, xPosition, yPosition, eventsList);
                        } else if (eventsList.size() == 1) {
                            drawSingleEvent(canvas, xPosition, yPosition, eventsList);
                        }
                    }
                }
            }
        }
    }

    private void drawSingleEvent(Canvas canvas, float xPosition, float yPosition, List<Event> eventsList) {
        Event event = eventsList.get(0);
        drawEventIndicatorCircle(canvas, xPosition, yPosition, event.getColor());
    }

    private void drawTwoEvents(Canvas canvas, float xPosition, float yPosition, List<Event> eventsList) {
        //draw fist event just left of center
        drawEventIndicatorCircle(canvas, xPosition + (xIndicatorOffset * -1), yPosition, eventsList.get(0).getColor());
        //draw second event just right of center
        drawEventIndicatorCircle(canvas, xPosition + (xIndicatorOffset * 1), yPosition, eventsList.get(1).getColor());
    }

    //draw 2 eventsByMonthAndYearMap followed by plus indicator to show there are more than 2 eventsByMonthAndYearMap
    private void drawEventsWithPlus(Canvas canvas, float xPosition, float yPosition, List<Event> eventsList) {
        // k = size() - 1, but since we don't want to draw more than 2 indicators, we just stop after 2 iterations so we can just hard k = -2 instead
        // we can use the below loop to draw arbitrary eventsByMonthAndYearMap based on the current screen size, for example, larger screens should be able to
        // display more than 2 evens before displaying plus indicator, but don't draw more than 3 indicators for now
        for (int j = 0, k = -2; j < 3; j++, k += 2) {
            Event event = eventsList.get(j);
            float xStartPosition = xPosition + (xIndicatorOffset * k);
            if (j == 2) {
                dayPaint.setColor(multiEventIndicatorColor);
                dayPaint.setStrokeWidth(multiDayIndicatorStrokeWidth);
                canvas.drawLine(xStartPosition - smallIndicatorRadius, yPosition, xStartPosition + smallIndicatorRadius, yPosition, dayPaint);
                canvas.drawLine(xStartPosition, yPosition - smallIndicatorRadius, xStartPosition, yPosition + smallIndicatorRadius, dayPaint);
                dayPaint.setStrokeWidth(0);
            } else {
                drawEventIndicatorCircle(canvas, xStartPosition, yPosition, event.getColor());
            }
        }
    }

    private int getDayOfWeek(Calendar calendar) {

        int dayOfWeek;
        if (!shouldShowMondayAsFirstDay) {
            return calendar.get(Calendar.DAY_OF_WEEK);
        } else {
            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            dayOfWeek = dayOfWeek <= 0 ? 7 : dayOfWeek;
        }
        return dayOfWeek;
    }

    void drawWeek(Canvas canvas, Calendar weekToDrawCalender, int offset) {
        drawEventsWeekly(canvas, weekToDrawCalender, offset);

        Calendar aux = Calendar.getInstance();
        aux.setFirstDayOfWeek(weekToDrawCalender.getFirstDayOfWeek());
        aux.setTimeInMillis(weekToDrawCalender.getTimeInMillis());
        //offset by one because we want to start from Monday

        //offset by one because of 0 index based calculations
        boolean isSameWeekAsToday = weekToDrawCalender.get(Calendar.WEEK_OF_YEAR) == todayCalender.get(Calendar.WEEK_OF_YEAR);
        boolean isSameYearAsToday = weekToDrawCalender.get(Calendar.YEAR) == todayCalender.get(Calendar.YEAR);
        boolean isSameWeekAsCurrentCalendar = weekToDrawCalender.get(Calendar.WEEK_OF_YEAR) == currentCalender.get(Calendar.WEEK_OF_YEAR);
        int todayDayOfMonth = todayCalender.get(Calendar.DAY_OF_MONTH);
        boolean isAnimatingWithExpose = animationStatus == EXPOSE_CALENDAR_ANIMATION;

        boolean selectedDay = false;
        for (int column = 0; column < dayColumnNames.length; column++) {

            float xPosition = widthPerDay * column + (paddingWidth + paddingLeft + accumulatedScrollOffset.x + offset - paddingRight);

            for (int row = 0; row < 2; row++) {

                float yPosition = row * (heightPerDay + paddingHeight);
                if (xPosition >= growFactor && (isAnimatingWithExpose || animationStatus == ANIMATE_INDICATORS) || yPosition >= growFactor) {
                    // don't draw days if animating expose or indicators
                    continue;
                }

                if (row == 0) {
                    // first row, so draw the first letter of the day
                    if (shouldDrawDaysHeader) {
                        dayPaint.setColor(calenderTextColor);
                        dayPaint.setTypeface(Typeface.DEFAULT_BOLD);
                        dayPaint.setStyle(Paint.Style.FILL);
                        dayPaint.setColor(calenderTextColor);
                        canvas.drawText(dayColumnNames[column], xPosition, paddingHeight, dayPaint);
                        dayPaint.setTypeface(Typeface.DEFAULT);
                    }
                } else {

                    if (column == 0) {
                        aux.set(Calendar.DAY_OF_WEEK, aux.getFirstDayOfWeek());
                    } else {
                        aux.add(Calendar.DAY_OF_WEEK, 1);
                    }
                    int day_month = aux.get(Calendar.DAY_OF_MONTH);
                    int day_week = aux.get(Calendar.DAY_OF_WEEK);

                    if (currentDayIndicator && isSameYearAsToday && isSameWeekAsToday && todayDayOfMonth == day_month && !isAnimatingWithExpose) {
                        // TODO calculate position of circle in a more reliable way
                        drawDayCircleIndicator(currentDayIndicatorStyle, canvas, xPosition, yPosition, currentDayBackgroundColor);
                    } else if (!selectedDay && inactiveDays.size() != 7) {
                        if (currentCalender.get(Calendar.DAY_OF_MONTH) == day_month && !inactiveDays.contains(currentCalender.get(Calendar.DAY_OF_WEEK)) && isSameWeekAsCurrentCalendar && !isAnimatingWithExpose) {
                            drawDayCircleIndicator(currentSelectedDayIndicatorStyle, canvas, xPosition, yPosition, currentSelectedDayBackgroundColor);
                            selectedDay = true;
                        } else if (inactiveDays.contains(currentCalender.get(Calendar.DAY_OF_WEEK)) && !inactiveDays.contains(aux.get(Calendar.DAY_OF_WEEK))) {
                            drawDayCircleIndicator(currentSelectedDayIndicatorStyle, canvas, xPosition, yPosition, currentSelectedDayBackgroundColor);
                            selectedDay = true;
                        }
                    }

                    dayPaint.setStyle(Paint.Style.FILL);
                    dayPaint.setColor(calenderTextColor);
                    if (isInactiveDate(aux)) {
                        dayPaint.setAlpha(127);
                    }
                    canvas.drawText(String.valueOf(day_month), xPosition, yPosition, dayPaint);
                }
            }
        }
    }

    void drawMonth(Canvas canvas, Calendar monthToDrawCalender, int offset) {
        drawEventsMonthly(canvas, monthToDrawCalender, offset);

        Calendar aux = Calendar.getInstance();
        aux.setTimeInMillis(monthToDrawCalender.getTimeInMillis());
        aux.setFirstDayOfWeek(monthToDrawCalender.getFirstDayOfWeek());
        aux.set(Calendar.DAY_OF_MONTH, 1);

        //offset by one because of 0 index based calculations
        boolean isSameMonthAsToday = monthToDrawCalender.get(Calendar.MONTH) == todayCalender.get(Calendar.MONTH);
        boolean isSameYearAsToday = monthToDrawCalender.get(Calendar.YEAR) == todayCalender.get(Calendar.YEAR);
        boolean isSameMonthAsCurrentCalendar = monthToDrawCalender.get(Calendar.MONTH) == currentCalender.get(Calendar.MONTH);
        int todayDayOfMonth = todayCalender.get(Calendar.DAY_OF_MONTH);
        boolean isAnimatingWithExpose = animationStatus == EXPOSE_CALENDAR_ANIMATION;

        boolean selectedDay = false;
        int row = 0;
        int day_month = aux.get(Calendar.DAY_OF_MONTH);
        int day_week;
        do {

            float yPosition = row * heightPerDay + paddingHeight;

            for (int column = 0; column < dayColumnNames.length; column++) {
                float xPosition = widthPerDay * column + (paddingWidth + paddingLeft + accumulatedScrollOffset.x + offset - paddingRight);
                if (xPosition >= growFactor && (isAnimatingWithExpose || animationStatus == ANIMATE_INDICATORS) || yPosition >= growFactor) {
                    // don't draw days if animating expose or indicators
                    continue;
                }

                if (row == 0) {
                    // first row, so draw the first letter of the day
                    if (shouldDrawDaysHeader) {
                        dayPaint.setColor(calenderTextColor);
                        dayPaint.setTypeface(Typeface.DEFAULT_BOLD);
                        dayPaint.setStyle(Paint.Style.FILL);
                        dayPaint.setColor(calenderTextColor);
                        canvas.drawText(dayColumnNames[column], xPosition, paddingHeight, dayPaint);
                        dayPaint.setTypeface(Typeface.DEFAULT);
                    }
                } else {

                    day_month = aux.get(Calendar.DAY_OF_MONTH);
                    day_week = aux.get(Calendar.DAY_OF_WEEK);

                    if (aux.getFirstDayOfWeek() == Calendar.MONDAY) {
                        if (day_week == Calendar.SUNDAY && column != 6) {
                            continue;
                        } else if (day_week != Calendar.SUNDAY && day_week - 1 != column + 1) {
                            continue;
                        }
                    } else if (day_week != (column + 1)) {
                        continue;
                    }

                    if (currentDayIndicator && isSameYearAsToday && isSameMonthAsToday && todayDayOfMonth == day_month && !isAnimatingWithExpose) {
                        drawDayCircleIndicator(currentDayIndicatorStyle, canvas, xPosition, yPosition, currentDayBackgroundColor);
                    } else if (!selectedDay && inactiveDays.size() != 7) {
                        if (currentCalender.get(Calendar.DAY_OF_MONTH) == day_month && !inactiveDays.contains(currentCalender.get(Calendar.DAY_OF_WEEK)) && isSameMonthAsCurrentCalendar && !isAnimatingWithExpose) {
                            drawDayCircleIndicator(currentSelectedDayIndicatorStyle, canvas, xPosition, yPosition, currentSelectedDayBackgroundColor);
                            selectedDay = true;
                        } else if (inactiveDays.contains(currentCalender.get(Calendar.DAY_OF_WEEK)) && !inactiveDays.contains(aux.get(Calendar.DAY_OF_WEEK))) {
                            drawDayCircleIndicator(currentSelectedDayIndicatorStyle, canvas, xPosition, yPosition, currentSelectedDayBackgroundColor);
                            selectedDay = true;
                        }
                    }

                    dayPaint.setStyle(Paint.Style.FILL);
                    dayPaint.setColor(calenderTextColor);
                    if (isInactiveDate(aux)) {
                        dayPaint.setAlpha(127);
                    }
                    canvas.drawText(String.valueOf(day_month), xPosition, yPosition, dayPaint);

                    if (day_month != aux.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        aux.add(Calendar.DATE, 1);
                    }
                }
            }
            row++;
        } while (day_month != aux.getActualMaximum(Calendar.DAY_OF_MONTH));
    }

    private void drawDayCircleIndicator(int indicatorStyle, Canvas canvas, float x, float y, int color) {
        drawDayCircleIndicator(indicatorStyle, canvas, x, y, color, 1);
    }

    private void drawDayCircleIndicator(int indicatorStyle, Canvas canvas, float x, float y, int color, float circleScale) {
        float strokeWidth = dayPaint.getStrokeWidth();
        if (indicatorStyle == NO_FILL_LARGE_INDICATOR) {
            dayPaint.setStrokeWidth(2 * screenDensity);
            dayPaint.setStyle(Paint.Style.STROKE);
        } else {
            dayPaint.setStyle(Paint.Style.FILL);
        }
        drawCircle(canvas, x, y, color, circleScale);
        dayPaint.setStrokeWidth(strokeWidth);
        dayPaint.setStyle(Paint.Style.FILL);
    }

    // Draw Circle on certain days to highlight them
    private void drawCircle(Canvas canvas, float x, float y, int color, float circleScale) {
        dayPaint.setColor(color);
        if (animationStatus == ANIMATE_INDICATORS) {
            float maxRadius = circleScale * bigCircleIndicatorRadius * 1.4f;
            drawCircle(canvas, growfactorIndicator > maxRadius ? maxRadius : growfactorIndicator, x, y - (textHeight / 6));
        } else {
            drawCircle(canvas, circleScale * bigCircleIndicatorRadius, x, y - (textHeight / 6));
        }
    }

    private void drawEventIndicatorCircle(Canvas canvas, float x, float y, int color) {
        dayPaint.setColor(color);
        if (eventIndicatorStyle == SMALL_INDICATOR) {
            dayPaint.setStyle(Paint.Style.FILL);
            drawCircle(canvas, smallIndicatorRadius, x, y);
        } else if (eventIndicatorStyle == NO_FILL_LARGE_INDICATOR) {
            dayPaint.setStyle(Paint.Style.STROKE);
            drawDayCircleIndicator(NO_FILL_LARGE_INDICATOR, canvas, x, y, color);
        } else if (eventIndicatorStyle == FILL_LARGE_INDICATOR) {
            drawDayCircleIndicator(FILL_LARGE_INDICATOR, canvas, x, y, color);
        }
    }

    private void drawCircle(Canvas canvas, float radius, float x, float y) {
        canvas.drawCircle(x, y, radius, dayPaint);
    }

    public void clearInactiveDays() {
        this.inactiveDays.clear();
    }

    public void removeInactiveDay(int inactiveDay) {
        for (Integer day : inactiveDays) {
            if (day == inactiveDay) {
                inactiveDays.remove(day);
                break;
            }
        }
    }

    public void setInactiveDays(List<Integer> inactiveDays) {
        for (Integer inactiveDay : inactiveDays) {
            addInactiveDay(inactiveDay);
        }
    }

    public void addInactiveDay(int day) {
        if (!inactiveDays.contains(day)) {
            inactiveDays.add(day);
            fixCurrentCalendar();
        }
    }

    public List<Integer> getInactiveDays() {
        return this.inactiveDays;
    }

    private void fixCurrentCalendar() {

        if (inactiveDays.size() != 7) {
            while (inactiveDays.contains(currentCalender.get(Calendar.DAY_OF_WEEK))) {
                currentCalender.add(Calendar.DATE, 1);
            }
        }
    }

    public Calendar getMinDateCalendar(){
        return this.minDateCalendar;
    }

    public void setMinDateCalendar(Date minDate) {
        this.minDateCalendar = Calendar.getInstance();
        this.minDateCalendar.setTime(minDate);
    }

    public Calendar getMaxDateCalendar(){
        return this.maxDateCalendar;
    }

    public void setMaxDateCalendar(Date maxDate) {
        maxDateCalendar = Calendar.getInstance();
        maxDateCalendar.setTime(maxDate);
    }

    private boolean isInactiveDate(Calendar calendar) {
        boolean conditionalActiveDay = inactiveDays.contains(calendar.get(Calendar.DAY_OF_WEEK));
        boolean conditionalMinDate = minDateCalendar != null && calendar.getTimeInMillis() < minDateCalendar.getTimeInMillis();
        boolean conditionalMaxDate = maxDateCalendar != null && maxDateCalendar.getTimeInMillis() < calendar.getTimeInMillis();

        return conditionalActiveDay || conditionalMinDate || conditionalMaxDate;
    }

    public boolean shouldScroll(){
        return this.shouldScroll;
    }

    public void shouldScroll(boolean shouldDisableScroll) {
        this.shouldScroll = shouldDisableScroll;
    }
}