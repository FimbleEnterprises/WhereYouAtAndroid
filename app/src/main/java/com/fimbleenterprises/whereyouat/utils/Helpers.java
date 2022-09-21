package com.fimbleenterprises.whereyouat.utils;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.POWER_SERVICE;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.jetbrains.annotations.NonNls;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * My old ass Java helper methods converted to Kotlin.  Haven't used most of this in years so
 * test thoroughly before put in production!
 */
@SuppressWarnings("unused")
public class Helpers {

    public static class Application {

        private static final String TAG = "Application";

        public static float getAppVersion(Context context) {
            try {
                PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                return Float.parseFloat(pInfo.versionName);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return 0;
            }
        }

        public static void openAppSettings(Context context) {

            Uri packageUri = Uri.fromParts( "package", context.getPackageName(), null );

            Intent applicationDetailsSettingsIntent = new Intent();

            applicationDetailsSettingsIntent.setAction( Settings.ACTION_APPLICATION_DETAILS_SETTINGS );
            applicationDetailsSettingsIntent.setData( packageUri );
            applicationDetailsSettingsIntent.addCategory(Intent.CATEGORY_DEFAULT);
            applicationDetailsSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
            applicationDetailsSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );
            applicationDetailsSettingsIntent.addFlags( Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS );

            context.startActivity( applicationDetailsSettingsIntent );

        }

        public static void startActivity(Context context, Object activityClass) {
            Intent intent = new Intent(context, activityClass.getClass());
            context.startActivity(intent);
        }

        public static void startActivityForResult(Activity sourceActivity, Object destActivityClass, int requestCode) {
            sourceActivity.startActivityForResult(new Intent(sourceActivity, destActivityClass.getClass()), requestCode);
        }

        public static void restartApplication(Context c) {
            try {
                //check if the context is given
                if (c != null) {
                    //fetch the packagemanager so we can get the default launch activity
                    // (you can replace this intent with any other activity if you want
                    PackageManager pm = c.getPackageManager();
                    //check if we got the PackageManager
                    if (pm != null) {
                        //create the intent with the default start activity for your application
                        Intent mStartActivity = pm.getLaunchIntentForPackage(
                                c.getPackageName()
                        );
                        if (mStartActivity != null) {
                            mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            //create a pending intent so the application is restarted after System.exit(0) was called.
                            // We use an AlarmManager to call this intent in 100ms
                            int mPendingIntentId = 223344;
                            PendingIntent mPendingIntent = PendingIntent
                                    .getActivity(c, mPendingIntentId, mStartActivity,
                                            PendingIntent.FLAG_CANCEL_CURRENT | FLAG_IMMUTABLE);
                            AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                            //kill the application
                            System.exit(0);
                        } else {
                            Log.e(TAG, "Was not able to restart application, mStartActivity null");
                        }
                    } else {
                        Log.e(TAG, "Was not able to restart application, PM null");
                    }
                } else {
                    Log.e(TAG, "Was not able to restart application, Context null");
                }
            } catch (Exception ex) {
                Log.e(TAG, "Was not able to restart application");
            }
        }

        /**
         * Checks if the app is currently ignoring battery optimizations.  Important for many
         * things such as starting a foreground service from a background thread (broadcast
         * receiver etc.).  NOTE: We have no restrictions if this method returns TRUE.
         * @param context A context sufficient to call getSystemService(POWER_SERVICE)
         * @return True if the app is saying fuck all to Android's protections.
         */
        public static Boolean isIgnoringBatteryOptimizations(Context context)  {
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            return pm.isIgnoringBatteryOptimizations(packageName);
        }

        /**
         * Sends the user to device settings regarding this app and battery optimization.
         * PREEETY sure this needs the following permission in the manifest:
         * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
         * @param context A context sufficient to launch an activity.
         */
        public static void sendToAppSettings(Activity context) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            context.startActivity(intent);
        }

        /**
         * Shows the application details for the app package name supplied (ex. com.dropbox.android)
         * @param context A valid context (not sure what valid means in this context if I'm honest...)
         * @param THE_APP_PACKAGE_NAME Ex: com.microsoft.skydrive
         */
        public static void showAppInfo(Context context, String THE_APP_PACKAGE_NAME) {
            Intent showSettings = new Intent();
            showSettings.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uriAppSettings = Uri.fromParts("package", THE_APP_PACKAGE_NAME, null);
            showSettings.setData(uriAppSettings);
            context.startActivity(showSettings);
        }

        /**
         * Attempts to focus and show the keyboard for an edittext.
         * @param editText The edittext to focus.
         * @param context A valid context
         */
        public static void showKeyboard(EditText editText, Context context) {
            try {
                editText.requestFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public static void hideSoftKeyboard(EditText editText, Context context) {
            try {
                editText.clearFocus();
                InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Bitmaps {
        public static Bitmap getBitmapFromResource(Context context, @DrawableRes int resource) {
            return BitmapFactory.decodeResource(context.getResources(),
                    resource);
        }

        public static BitmapDescriptor getBitmapDescriptorFromDrawable(Drawable drawable) {
            Canvas canvas = new Canvas();
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            canvas.setBitmap(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bitmap);
        }

        public static File createPngFileFromString(String string, String fileName, File savein) throws IOException {

           fileName = fileName.replace(".txt",".png");
            if (!fileName.endsWith(".png")) {
                fileName += ".png";
            }

            final Rect bounds = new Rect();
            TextPaint textPaint = new TextPaint() {
                {
                    setColor(Color.WHITE);
                    setTextAlign(Paint.Align.LEFT);
                    setTextSize(20f);
                    setAntiAlias(true);
                }
            };
            textPaint.getTextBounds(string, 0, string.length(), bounds);
            StaticLayout mTextLayout = new StaticLayout(string, textPaint,
                    bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            int maxWidth = -1;
            for (int i = 0; i < mTextLayout.getLineCount(); i++) {
                if (maxWidth < mTextLayout.getLineWidth(i)) {
                    maxWidth = (int) mTextLayout.getLineWidth(i);
                }
            }
            final Bitmap bmp = Bitmap.createBitmap(maxWidth , mTextLayout.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.BLACK);// just adding black background
            final Canvas canvas = new Canvas(bmp);
            mTextLayout.draw(canvas);
            File outputFile = new File(savein, fileName);
            FileOutputStream stream = new FileOutputStream(outputFile); //create your FileOutputStream here
            bmp.compress(Bitmap.CompressFormat.PNG, 85, stream);
            bmp.recycle();
            stream.close();
            return outputFile;
        }

        public static File createJpegFileFromString(String string, String fileName, File savein) throws IOException {

            fileName = fileName.replace(".txt",".jpeg");
            if (!fileName.endsWith(".jpeg")) {
                fileName += ".jpeg";
            }

            final Rect bounds = new Rect();
            TextPaint textPaint = new TextPaint() {
                {
                    setColor(Color.WHITE);
                    setTextAlign(Paint.Align.LEFT);
                    setTextSize(20f);
                    setAntiAlias(true);
                }
            };
            textPaint.getTextBounds(string, 0, string.length(), bounds);
            StaticLayout mTextLayout = new StaticLayout(string, textPaint,
                    bounds.width(), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            int maxWidth = -1;
            for (int i = 0; i < mTextLayout.getLineCount(); i++) {
                if (maxWidth < mTextLayout.getLineWidth(i)) {
                    maxWidth = (int) mTextLayout.getLineWidth(i);
                }
            }
            final Bitmap bmp = Bitmap.createBitmap(maxWidth , mTextLayout.getHeight(),
                    Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.BLACK);// just adding black background
            final Canvas canvas = new Canvas(bmp);
            mTextLayout.draw(canvas);
            File outputFile = new File(savein, fileName);
            FileOutputStream stream = new FileOutputStream(outputFile); //create your FileOutputStream here
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, stream);
            bmp.recycle();
            stream.close();
            return outputFile;
        }

        /**
         * Converts any view to a bitmap.
         */
        public static Bitmap saveScrollViewAsImage(ScrollView scrollView) {
            Bitmap bitmap = Bitmap.createBitmap(
                    scrollView.getChildAt(0).getWidth(),
                    scrollView.getChildAt(0).getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bitmap);
            scrollView.getChildAt(0).draw(c);
            return bitmap;
        }

        public static Bitmap saveViewAsImage(View view) {
            view.setDrawingCacheEnabled(true);
            view.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
            view.buildDrawingCache(true);
            Bitmap saveBm = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);
            return saveBm;
        }

        /**
         * Saves a bitmap to a png file
         * @param bmp The bitmap to save
         * @param file A file to create
         * @return The created file
         */
        public static File bitmapToFile(Bitmap bmp, File file) {
            try (FileOutputStream out = new FileOutputStream(file)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
                return file;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class Colors {
        public static final String YELLOW = "#EFC353";
        public static final String MEDISTIM_ORANGE = "#AAF37021";
        public static final String GREEN = "#2D9B01";
        public static final String RED = "#FF0000";
        public static final String MAROON = "#7F0000";
        public static final String SOFT_BLACK = "#3C4F5F";
        public static final String BLUE = "#0026FF";
        public static final String DISABLED_GRAY = "#808080";

        public static int getColor(String color) {
            return Color.parseColor(color);
        }
    }

    public static class BytesAndBits {
        public static long convertBytesToKb(long total) {
            return total / (1024);
        }

        public static long convertBytesToMb(long total) {
            return total / (1024 * 1024);
        }

        public static long convertBytesToGb(long total) {
            return total / (1024 * 1024 * 1024);
        }
    }

    public static class DatesAndTimes {

        private static final Map<String, String> DATE_FORMAT_REGEXPS = new HashMap<String, String>() {{
            put("^\\d{8}$", "yyyyMMdd");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}$", "dd-MM-yyyy");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}$", "yyyy-MM-dd");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}$", "MM/dd/yyyy");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}$", "yyyy/MM/dd");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}$", "dd MMM yyyy");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}$", "dd MMMM yyyy");
            put("^\\d{12}$", "yyyyMMddHHmm");
            put("^\\d{8}\\s\\d{4}$", "yyyyMMdd HHmm");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}$", "dd-MM-yyyy HH:mm");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy-MM-dd HH:mm");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}$", "MM/dd/yyyy HH:mm");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}$", "yyyy/MM/dd HH:mm");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMM yyyy HH:mm");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}$", "dd MMMM yyyy HH:mm");
            put("^\\d{14}$", "yyyyMMddHHmmss");
            put("^\\d{8}\\s\\d{6}$", "yyyyMMdd HHmmss");
            put("^\\d{1,2}-\\d{1,2}-\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd-MM-yyyy HH:mm:ss");
            put("^\\d{4}-\\d{1,2}-\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy-MM-dd HH:mm:ss");
            put("^\\d{1,2}/\\d{1,2}/\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "MM/dd/yyyy HH:mm:ss");
            put("^\\d{4}/\\d{1,2}/\\d{1,2}\\s\\d{1,2}:\\d{2}:\\d{2}$", "yyyy/MM/dd HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{3}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMM yyyy HH:mm:ss");
            put("^\\d{1,2}\\s[a-z]{4,}\\s\\d{4}\\s\\d{1,2}:\\d{2}:\\d{2}$", "dd MMMM yyyy HH:mm:ss");
        }};

        /**
         * Converts a Crm formatted string representing a date to a DateTime object.
         * @param datetime The date to attempt to convert
         * @return A DateTime object if successful, null if not.
         */
        public static DateTime parseCrmDateTime(String datetime) {
            try {
                DateTimeFormatter format = DateTimeFormat.forPattern("M/d/yyyy h:mm tt");
                return DateTimeFormat.forPattern("M/d/yyyy h:mm tt").parseDateTime(datetime);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        /**
         * Converts a Crm formatted string representing a date to a DateTime object.
         * @param date The date to attempt to convert
         * @return A DateTime object if successful, null if not.
         */
        public static DateTime parseCrmDateOnly(String date) {
            try {
                DateTimeFormatter format = DateTimeFormat.forPattern("M/d/yyyy");
                return DateTimeFormat.forPattern("M/d/yyyy").parseDateTime(date);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String toCrmDate(DateTime datetime) {
            try {
                DateTimeFormatter format = DateTimeFormat.forPattern("M/d/yyyy");
                return datetime.toString(format);
            } catch (Exception e) {
                e.printStackTrace();
                return datetime.toLocalDateTime().toString();
            }
        }

        /**
         * Converts the supplied milisecond value into minutes
         **/
        public static int convertMilisToMinutes(double milis) {
            return (int) milis / (1000 * 60);
        }

        /**
         * Returns the current week of the year from 1 - 52 (e.g. 23)
         **/
        public static int returnDayOfYear(DateTime date) {

            Calendar c = Calendar.getInstance();
            c.setMinimalDaysInFirstWeek(7);//anything more than 1 will work in this year
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                c.setTime(Objects.requireNonNull(
                        sdf.parse(
                                date.getDayOfMonth()
                                + "/"
                                + date.getMonthOfYear()
                                + "/"
                                + date.getYearOfCentury())));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return c.get(Calendar.DAY_OF_YEAR);
        }

        /**
         * Returns the current week of the year from 1 - 52 (e.g. 23)
         **/
        public static int returnWeekOfYear(DateTime date) {

            Calendar c = Calendar.getInstance();
            c.setMinimalDaysInFirstWeek(2);//anything more than 1 will work in this year
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                c.setTime(Objects.requireNonNull(sdf.parse(
                        date.getDayOfMonth()
                                + "/"
                                + date.getMonthOfYear() + "/"
                                + date.getYearOfCentury())));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return c.get(Calendar.WEEK_OF_YEAR);
        }

        /**
         * Returns the current week of the year from 1 - 52 (e.g. 23)
         **/
        public static int returnMonthOfYear(DateTime date) {

            Calendar c = Calendar.getInstance();
            c.setMinimalDaysInFirstWeek(7);//anything more than 1 will work in this year
            DateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                c.setTime(Objects.requireNonNull(sdf.parse(date.getDayOfMonth() + "/" + date
                        .getMonthOfYear() + "/" + date.getYearOfCentury())));
            } catch (ParseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return c.get(Calendar.MONTH);
        }

        public static int daysBetween(DateTime start, DateTime end) {
            return Days.daysBetween(start, end).getDays();
        }

        public static int daysBetween(DateTime start) {
            return Days.daysBetween(start, DateTime.now()).getDays();
        }

        public static String returnMonthName(int monthNumber, boolean abbreviateMonthName) {
            String monthString = "";

            switch (monthNumber) {
                case 1:
                    monthString = "January";
                    break;
                case 2:
                    monthString = "Febuary";
                    break;

                case 3:
                    monthString = "March";
                    break;
                case 4:
                    monthString = "April";
                    break;
                case 5:
                    monthString = "May";
                    break;
                case 6:
                    monthString = "June";
                    break;
                case 7:
                    monthString = "July";
                    break;
                case 8:
                    monthString = "August";
                    break;
                case 9:
                    monthString = "September";
                    break;
                case 10:
                    monthString = "October";
                    break;
                case 11:
                    monthString = "November";
                    break;
                case 12:
                    monthString = "December";
                    break;
            }

            if (abbreviateMonthName) {
                monthString = monthString.substring(0, 3);
            }

            return monthString;
        }

        public static String getPrettyDate(DateTime now) {

            String day = String.valueOf(now.getDayOfMonth());
            String month = String.valueOf(now.getMonthOfYear());
            String year = String.valueOf(now.getYear());

            return month + "/" + day + "/" + year;

        }

        public static DateTime parseDateTime(String strDate) {
            DateTimeFormatter df = DateTimeFormat.forPattern("M/d/yyyy h:mm a");
            try {
                return df.parseDateTime(strDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new DateTime();
        }

        public static DateTime parseDate(String strDate) {
            DateTimeFormatter df = DateTimeFormat.forPattern("M/d/yyyy");
            try {
                return df.parseDateTime(strDate);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new DateTime();
        }

        public static String getPrettyDateAndTime(DateTime now) {

            if (now == null) { return null; }

            String day = String.valueOf(now.getDayOfMonth());
            String month = String.valueOf(now.getMonthOfYear());
            String year = String.valueOf(now.getYear());
            String amPm = "am";

            int intHour = now.getHourOfDay();
            if (intHour == 12) {
                amPm = "pm";
            }
            if (intHour > 12) {
                intHour = intHour - 12;
                amPm = "pm";
            }
            String hour = String.valueOf(intHour);
            int intMinutes = now.getMinuteOfHour();
            String minutes = String.valueOf(intMinutes);
            int intSeconds = now.getSecondOfMinute();
            String seconds = String.valueOf(intSeconds);
            if (intSeconds < 10) {
                seconds = "0" + seconds;
            }


            switch (intMinutes) {
                case 0:
                    minutes = "00";
                    break;
                case 1:
                    minutes = "01";
                    break;
                case 2:
                    minutes = "02";
                    break;
                case 3:
                    minutes = "03";
                    break;
                case 4:
                    minutes = "04";
                    break;
                case 5:
                    minutes = "05";
                    break;
                case 6:
                    minutes = "06";
                    break;
                case 7:
                    minutes = "07";
                    break;
                case 8:
                    minutes = "08";
                    break;
                case 9:
                    minutes = "09";
                    break;
            }

            return month + "/" + day + "/" + year + " " + hour + ":" + minutes + ":" + seconds + " " + amPm;

        }

        public static String getPrettyDateAndTime(DateTime now, boolean convertUtcToLocal, Boolean includeSeconds) {

            // We do nothing with null values
            if (now == null) { return null; }

            // If the supplied datetime is UTC (which we have to sloppily assume) we can extract
            // the timezone and use it to convert the supplied datetime to the device's local zone.
            if (convertUtcToLocal) {
                DateTimeZone timeZone = now.getZone();
                now = new DateTime(timeZone.convertUTCToLocal(now.getMillis()));
            }

            String day = String.valueOf(now.getDayOfMonth());
            String month = String.valueOf(now.getMonthOfYear());
            String year = String.valueOf(now.getYear());
            String amPm = "am";

            int intHour = now.getHourOfDay();
            if (intHour == 12) {
                amPm = "pm";
            }
            if (intHour > 12) {
                intHour = intHour - 12;
                amPm = "pm";
            }
            String hour = String.valueOf(intHour);
            int intMinutes = now.getMinuteOfHour();
            String minutes = String.valueOf(intMinutes);
            int intSeconds = now.getSecondOfMinute();
            String seconds = String.valueOf(intSeconds);
            if (intSeconds < 10) {
                seconds = "0" + seconds;
            }

            switch (intMinutes) {
                case 0:
                    minutes = "00";
                    break;
                case 1:
                    minutes = "01";
                    break;
                case 2:
                    minutes = "02";
                    break;
                case 3:
                    minutes = "03";
                    break;
                case 4:
                    minutes = "04";
                    break;
                case 5:
                    minutes = "05";
                    break;
                case 6:
                    minutes = "06";
                    break;
                case 7:
                    minutes = "07";
                    break;
                case 8:
                    minutes = "08";
                    break;
                case 9:
                    minutes = "09";
                    break;
            }

            if (includeSeconds) {
                return month + "/" + day + "/" + year + " " + hour + ":" + minutes + ":" + seconds + " " + amPm;
            }

            return month + "/" + day + "/" + year + " " + hour + ":" + minutes + " " + amPm;

        }public static String getPrettyDateAndTime(DateTime now, boolean convertUtcToLocal, Boolean includeSeconds, Boolean includeMs) {

            // We do nothing with null values
            if (now == null) { return null; }

            // If the supplied datetime is UTC (which we have to sloppily assume) we can extract
            // the timezone and use it to convert the supplied datetime to the device's local zone.
            if (convertUtcToLocal) {
                DateTimeZone timeZone = now.getZone();
                now = new DateTime(timeZone.convertUTCToLocal(now.getMillis()));
            }

            String day = String.valueOf(now.getDayOfMonth());
            String month = String.valueOf(now.getMonthOfYear());
            String year = String.valueOf(now.getYear());
            String amPm = "am";

            int intHour = now.getHourOfDay();
            if (intHour == 12) {
                amPm = "pm";
            }
            if (intHour > 12) {
                intHour = intHour - 12;
                amPm = "pm";
            }
            String hour = String.valueOf(intHour);
            int intMinutes = now.getMinuteOfHour();
            String minutes = String.valueOf(intMinutes);
            int intSeconds = now.getSecondOfMinute();
            String seconds = String.valueOf(intSeconds);
            if (intSeconds < 10) {
                seconds = "0" + seconds;
            }
            int intMillis = now.getMillisOfSecond();
            String milliseconds = String.valueOf(intMillis);

            switch (intMinutes) {
                case 0:
                    minutes = "00";
                    break;
                case 1:
                    minutes = "01";
                    break;
                case 2:
                    minutes = "02";
                    break;
                case 3:
                    minutes = "03";
                    break;
                case 4:
                    minutes = "04";
                    break;
                case 5:
                    minutes = "05";
                    break;
                case 6:
                    minutes = "06";
                    break;
                case 7:
                    minutes = "07";
                    break;
                case 8:
                    minutes = "08";
                    break;
                case 9:
                    minutes = "09";
                    break;
            }

            if (includeSeconds) {
                if (includeMs) {
                    return month + "/" + day + "/" + year + " " + hour + ":" + minutes + ":" + seconds + ":" + milliseconds +  " " + amPm;
                }
                return month + "/" + day + "/" + year + " " + hour + ":" + minutes + ":" + seconds + " " + amPm;
            }

            return month + "/" + day + "/" + year + " " + hour + ":" + minutes + " " + amPm;

        }

        // This method returns today's date as a short date string
        public static String getTodaysDateAsString() {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            String formattedDate = df.format(c.getTime());

            Log.d("GetTodaysDate", "Today's date is: '" + formattedDate + "'");

            return formattedDate;
        }

        // This method returns yesterday's date as a short date string
        public static String getYesterdaysDateAsString() {

            // Get today as a Calendar
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            // Subtract 1 day
            c.add(Calendar.DATE, -1);
            String formattedDate = df.format(c.getTime());

            Log.d("GetYesterdaysDate", "Yesterday's date is: '" + formattedDate + "'");

            return formattedDate;
        }

        // This method returns the first day of the week as a short date string
        public static String getFirstDayOfWeek() {

            // get today and clear time of day
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of
            // day !
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            // get start of this week as a formal date
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());

            // instantiate a formatter
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

            // format the formal date
            String formattedDate = df.format(cal.getTime());

            // log the result
            Log.d("getFirstOfWeek()", "First day of this week is: '" + formattedDate + "'");

            // return the result
            return formattedDate;

        }

        // This method returns the first day of the month as a short date string
        public static String getFirstDayOfMonth() {

            // get today and clear time of day
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0); // ! clear would not reset the hour of
            // day !
            cal.clear(Calendar.MINUTE);
            cal.clear(Calendar.SECOND);
            cal.clear(Calendar.MILLISECOND);

            // get start of this week as a formal date
            cal.set(Calendar.DAY_OF_MONTH, 1);

            // instantiate a formatter
            SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

            // format the formal date
            String formattedDate = df.format(cal.getTime());

            // log the result
            Log.d("getFirstOfMonth()", "First day of this month is: '" + formattedDate + "'");

            // return the result
            return formattedDate;
        }

        public static DateTime getFirstOfYear() {

            return new DateTime(DateTime.now().getYear(), 1, 1, 0,0);
        }

        public static DateTime getLastOfYear() {
            return new DateTime(DateTime.now().getYear(), 12, 31, 0, 0);
        }

        public static DateTime getFirstOfMonth() {

            return new DateTime(DateTime.now().getYear(), DateTime.now().getMonthOfYear(), 1, 0,0);
        }

        public static DateTime getLastOfMonth() {

            return new DateTime(DateTime.now().getYear(), DateTime.now().getMonthOfYear(),
                    getDaysInMonth(DateTime.now().getYear(), DateTime.now().getMonthOfYear()), 0,0);
        }

        public static DateTime getFirstOfYear(int year) {

            return new DateTime(DateTime.now().getYear(), 1, 1, 0,0);
        }

        public static DateTime getLastOfYear(int year) {
            return new DateTime(DateTime.now().getYear(), 12, 31, 0, 0);
        }

        public static DateTime getFirstOfMonth(int year, int month) {

            return new DateTime(year, month, 1, 0,0);
        }

        public static DateTime getLastOfMonth(int year, int month) {

            return new DateTime(year, month, getDaysInMonth(year, month),0 ,0);
        }

        public static int getDaysInMonth(int year, int month) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, (month - 1));
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH); // <-- the result!
        }

        /**
         * Returns the last calendar day of the supplied month and year.
         * @param month The month to evaluate.
         * @param year The year to evaluate.
         * @return The last possible day of the month as a DateTime object (as of Midnight of that day).
         */
        public static DateTime getLastDayOfMonthAsDateTimeObject(int month, int year) {
            DateTime dateTime = new DateTime(year, month, 1, 0, 0);
            return dateTime.dayOfMonth().withMaximumValue();
        }

        /**
         * Returns the last calendar day of the supplied month and year.
         * @param month The month to evaluate.
         * @param year The year to evaluate.
         * @return The calendar date as a pretty string (e.g. 12/23/2002)
         */
        public static String getLastDayOfMonthAsPrettyString(int month, int year) {
            String result;
            // month = month + 1; // Zero based month index
            if (month == 0) {
                month = 1;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            Date convertedDate = null;
            String dateString = month + "/1/" + year;
            try {
                convertedDate = dateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Calendar c = Calendar.getInstance();
            c.setTime(Objects.requireNonNull(convertedDate));
            c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
            String d, m, y;
            d = String.valueOf(c.get(Calendar.DAY_OF_MONTH));
            m = String.valueOf(c.get(Calendar.MONTH) + 1);
            y = String.valueOf(c.get(Calendar.YEAR));
            result = m + "/" + d + "/" + y;
            return result;
        }

        /**
         * Returns the string fullname of the requested month number
         *
         * @param monthNumber The month number as an int
         * @return String value of the month number.
         */
        public static String getMonthName(int monthNumber) {
            String prettyMonth;
            switch (monthNumber) {
                case 1:
                    prettyMonth ="January";
                    break;
                case 2:
                    prettyMonth ="February";
                    break;
                case 3:
                    prettyMonth ="March";
                    break;
                case 4:
                    prettyMonth ="April";
                    break;
                case 5:
                    prettyMonth ="May";
                    break;
                case 6:
                    prettyMonth ="June";
                    break;
                case 7:
                    prettyMonth ="July";
                    break;
                case 8:
                    prettyMonth ="August";
                    break;
                case 9:
                    prettyMonth ="September";
                    break;
                case 10:
                    prettyMonth ="October";
                    break;
                case 11:
                    prettyMonth ="November";
                    break;
                case 12:
                    prettyMonth ="December";
                    break;
                default:
                    prettyMonth = "";
                    break;
            }
            return prettyMonth;
        }

        public static String getPrettyDate2(DateTime dateTime) {
            String monthName = getMonthName(dateTime.getMonthOfYear()).trim();
            String year = Integer.toString(dateTime.getYear());
            String day = Integer.toString(dateTime.getDayOfMonth());
            return monthName + " " + day + ", " + year;
        }

        /**
         * Determine SimpleDateFormat pattern matching with the given date string. Returns null if
         * format is unknown. You can simply extend DateUtil with more formats if needed.
         * @param dateString The date string to determine the SimpleDateFormat pattern for.
         * @return The matching SimpleDateFormat pattern, or null if format is unknown.
         * @see SimpleDateFormat
         */
        public static String determineDateFormat(String dateString) {
            for (String regexp : DATE_FORMAT_REGEXPS.keySet()) {
                if (dateString.toLowerCase().matches(regexp)) {
                    return DATE_FORMAT_REGEXPS.get(regexp);
                }
            }
            return null; // Unknown format.
        }
    }

    public static class Sms {

        interface SmsListener {
            void onSent();
            void onDelivered();
        }

        public static final String SENT_ACTION = "SENT_ACTION";
        public static final String DELIVERY_ACTION = "DELIVERY_ACTION";

        public static void sendSms(Activity activity, String number, String msgBody) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, msgBody)));
        }

        public static void sendSms(Activity activity, String number) {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", number, null)));
        }

        /**
         * Shoots a text message with sent and delivered updates
         * @param activity A valid activity that can register a receiver
         * @param number The SMS target device
         * @param msg The fucking message to send
         * @param listener A listener for the sent and delivered updates.
         */
        public static void sendSmsWithListener(final Activity activity, String number, String msg, final SmsListener listener) {

            // set pendingIntent for sent & delivered
            PendingIntent sentIntent = PendingIntent.getBroadcast(activity, 100, new
                    Intent(SENT_ACTION), FLAG_IMMUTABLE);
            PendingIntent deliveryIntent = PendingIntent.getBroadcast(activity, 200, new
                    Intent(DELIVERY_ACTION), FLAG_IMMUTABLE);

            activity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d("SMS ", "sent");
                    listener.onSent();
                    activity.unregisterReceiver(this);
                }
            }, new IntentFilter(SENT_ACTION));

            activity.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    Log.d("SMS ", "delivered");
                    listener.onDelivered();
                    activity.unregisterReceiver(this);
                }
            }, new IntentFilter(DELIVERY_ACTION));

            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, msg, sentIntent,deliveryIntent);
        }

    }

    public static class Numbers {

        /**
         * Checks if a number is numeric (kind of an expensive operation so if it needs to be done a
         * ton then roll a different way)
         **/
        public static boolean isNumeric(String str) {
            try {
                float d = Float.parseFloat(str);
            } catch (NumberFormatException nfe) {
                return false;
            }
            return true;
        }

        public static int makeRandomInt() {
            String strLng = Long.toString(System.currentTimeMillis());
            String subStrLng = strLng.substring(5);
            return Integer.parseInt(subStrLng);
        }

        public static double formatAsTwoDecimalPointNumber(double number, RoundingMode roundingMode) {
            DecimalFormat df2 = new DecimalFormat("#.##");
            df2.setRoundingMode(roundingMode);
            return Double.parseDouble(df2.format(number));
        }

        public static double formatAsTwoDecimalPointNumber(double number) {
            DecimalFormat df2 = new DecimalFormat("#.##");
            df2.setRoundingMode(RoundingMode.HALF_UP);
            return Double.parseDouble(df2.format(number));
        }

        public static double formatAsThreeDecimalPointNumber(double number) {
            DecimalFormat df2 = new DecimalFormat("#.###");
            df2.setRoundingMode(RoundingMode.HALF_UP);
            return Double.parseDouble(df2.format(number));
        }

        /**
         * Digits higher than 10 will be replaced with 10.
         */
        public static double formatAsXDecimalPointNumber(double number, int digits) {
            if (digits > 10) {
                digits = 10;
            }
            DecimalFormat df2 = new DecimalFormat("#");
            switch (digits) {
                case 2:
                    df2 = new DecimalFormat("#.##");
                    break;
                case 3:
                    df2 = new DecimalFormat("#.###");
                    break;
                case 4:
                    df2 = new DecimalFormat("#.####");
                    break;
                case 5:
                    df2 = new DecimalFormat("#.#####");
                    break;
                case 6:
                    df2 = new DecimalFormat("#.######");
                    break;
                case 7:
                    df2 = new DecimalFormat("#.#######");
                    break;
                case 8:
                    df2 = new DecimalFormat("#.########");
                    break;
                case 9:
                    df2 = new DecimalFormat("#.#########");
                    break;
                case 10:
                    df2 = new DecimalFormat("#.##########");
                    break;
            }

            df2.setRoundingMode(RoundingMode.HALF_UP);
            return Double.parseDouble(df2.format(number));
        }

        public static double formatAsOneDecimalPointNumber(double number, RoundingMode roundingMode) {
            DecimalFormat df2 = new DecimalFormat("#.#");
            df2.setRoundingMode(roundingMode);
            return Double.parseDouble(df2.format(number));
        }

        public static double formatAsOneDecimalPointNumber(double number) {
            DecimalFormat df2 = new DecimalFormat("#.#");
            // df2.setRoundingMode(roundingMode);
            return Double.parseDouble(df2.format(number));
        }

        public static int formatAsZeroDecimalPointNumber(double number, RoundingMode roundingMode) {
            DecimalFormat df2 = new DecimalFormat("#");
            df2.setRoundingMode(roundingMode);
            return Integer.parseInt(df2.format(number));
        }

        public static int formatAsZeroDecimalPointNumber(double number) {
            DecimalFormat df2 = new DecimalFormat("#");
            df2.setRoundingMode(RoundingMode.HALF_UP);
            return Integer.parseInt(df2.format(number));
        }

        public static String convertToCurrency(double amount) {
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            return nf.format(amount);
        }

        public static String convertToCurrency(double amount, boolean includeSymbol) {
            String symbol = Currency.getInstance(Locale.getDefault()).getSymbol();
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            if (includeSymbol) {
                return nf.format(amount);
            } else {
                return nf.format(amount).replace(symbol, "");
            }
        }

        public static String convertToPercentage(double value) {
            NumberFormat numberFormat = NumberFormat.getPercentInstance();
            numberFormat.setMaximumFractionDigits(1);
            return numberFormat.format(value);
        }

        public static String convertToPercentage(double value, boolean includeSymbol) {
            NumberFormat numberFormat = NumberFormat.getPercentInstance();
            numberFormat.setMaximumFractionDigits(1);
            if (includeSymbol) {
                return numberFormat.format(value);
            } else {
                return numberFormat.format(value).replace("%","");
            }
        }

        public static int getRandom(int low, int high) {
            Random r = new Random();
            return r.nextInt((high + 1) - low) + low;
        }

        public static boolean isEven(int number) {
            return (number % 2) == 0;
        }

        /**
         * Will return a pretty string representation of seconds elapsed expressed in roughly the
         * minutes elapsed.  This will go up to 15 minutes and then report, "More than 15 minutes ago.
         * @param seconds The seconds to consider.
         * @return The elapsed time in approximate minutes.
         */
        public static String secondsAgo(int seconds) {
            if (seconds <= 2) {
                return "Just now";
            } else if (seconds < 60) {
                return seconds + " seconds ago";
            } else if (seconds <= 90) {
                return "About a minute ago";
            } else if (seconds < 120) {
                return "A minute and a half ago";
            } else if (seconds < 180) {
                return "2 minutes ago";
            } else if (seconds < 240) {
                return "3 minutes ago";
            } else if (seconds < 300) {
                return "4 minutes ago";
            } else if (seconds < 360) {
                return "5 minutes ago";
            } else if (seconds < 420) {
                return "6 minutes ago";
            } else if (seconds < 500) {
                return "7 minutes ago";
            } else if (seconds < 560) {
                return "8 minutes ago";
            } else if (seconds < 620) {
                return "9 minutes ago";
            } else if (seconds < 700) {
                return "10 minutes ago";
            } else if (seconds < 760) {
                return "11 minutes ago";
            } else if (seconds < 820) {
                return "12 minutes ago";
            } else if (seconds < 900) {
                return "13 minutes ago";
            } else if (seconds < 960) {
                return "14 minutes ago";
            } else if (seconds < 1020) {
                return "15 minutes ago";
            } else {
                return "More than 15 minutes ago";
            }
        }
    }

    public static class Email {
        /**
         * Launches a dialog to open an googleEmail while populating the to, subject and body fields.  User
         * must still press send.
         *
         * @param recipients example: new String[]{"recipient@example.com"}
         * @param body       The body of the googleEmail message
         * @param subject    The subject of the googleEmail message
         * @param context    The sending method's context
         */
        public static void sendEmail(String[] recipients, String body, String subject, Context context) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, recipients);
            i.putExtra(Intent.EXTRA_BCC, recipients);
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_HTML_TEXT, body);
            try {
                context.startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "There are no googleEmail clients installed.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Launches a dialog to open an googleEmail while populating the to, subject and body fields.  User
         * must still press send.
         *
         * @param recipients example: new String[]{"recipient@example.com"}
         * @param body       The body of the googleEmail message
         * @param subject    The subject of the googleEmail message
         * @param context    The sending method's context
         */
        public static void sendEmail(String[] recipients, String body, String subject, Context context,
                                     File attachment) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, recipients);
            i.putExtra(Intent.EXTRA_BCC, recipients);
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_HTML_TEXT, body);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + attachment));
            try {
                context.startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "There are no googleEmail clients installed.", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * Launches a dialog to open an googleEmail while populating the to, subject and body fields.  User
         * must still press send.
         *
         * @param recipients example: new String[]{"recipient@example.com"}
         * @param body       The body of the googleEmail message
         * @param subject    The subject of the googleEmail message
         * @param context    The sending method's context
         */
        public static void sendEmail(String[] recipients, String body, String subject, Context context,
                                     File attachment, boolean copyMe) {

            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, recipients);
            if (copyMe) {
                i.putExtra(Intent.EXTRA_BCC, recipients);
            }
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_HTML_TEXT, body);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + attachment));
            try {
                context.startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "There are no googleEmail clients installed.", Toast.LENGTH_SHORT).show();
            }
        }

        public static void sendEmail(String[] recipients, String[] bccRecipients, String body, String subject, Context context) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_EMAIL, recipients);
            i.putExtra(Intent.EXTRA_BCC, bccRecipients);
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_HTML_TEXT, body);
            try {
                context.startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "There are no googleEmail clients installed.", Toast.LENGTH_SHORT).show();
            }
        }

        public static void sendEmail(@NonNls String body, String subject, Context context) {
            Intent i = new Intent(Intent.ACTION_SEND);
            // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            i.setType("message/rfc822");
            i.putExtra(Intent.EXTRA_SUBJECT, subject);
            i.putExtra(Intent.EXTRA_TEXT, body);
            i.putExtra(Intent.EXTRA_HTML_TEXT, body);
            try {
                context.startActivity(Intent.createChooser(i, "Send mail..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static class Battery {

        /**
         * Returns whether or not the device is plugged ino to AC/DC power or USB
         **/
        public static boolean deviceIsPluggedIn(Context context) {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        }

        /**
         * Returns an integer which can be compared to the BatteryManager constants.  1 == AC power and 2 == USB power
         **/
        public static int deviceIsPluggedInto(Context context) {
            Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            return intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        }
    }

    public static class Debug {

        private static final String TAG = "Debug";

        public static final String LOG_FILE_NAME = "medibuddy_debug_log";
        public static final String TEMP_LOG_FILE_NAME = "temp_medibuddy_debug_log";

        /***
         * Reads the device's logcat log and returns it as a string.
         * @return String object representing the logcat information.
         */
        public static String sendLogcat(final Context context) {
            Toast.makeText(context, "Please wait while I gather debugging data...", Toast.LENGTH_LONG).show();

            final StringBuilder text = new StringBuilder();
            try {
                final File logFile = new File(Environment.getExternalStorageDirectory(), LOG_FILE_NAME);
                Log.d(TAG, "Gathering logcat data...");

                new OutputStreamWriter(context.openFileOutput(TEMP_LOG_FILE_NAME, MODE_PRIVATE));
                final File tempFile = new File(context.getFilesDir(), TEMP_LOG_FILE_NAME);
                Runtime.getRuntime().exec("logcat -d -v time -f " + tempFile.getAbsolutePath());

                try {
                    Log.d(TAG, "Reading logcat file...");
                    BufferedReader br = new BufferedReader(new FileReader(tempFile));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line);
                        text.append('\n');
                    }
                    br.close();
                    Log.d(TAG, "Finished reading - appending it to our log file...");
                    boolean deleted = tempFile.delete();

                    FileWriter fw = new FileWriter(logFile, true);
                    fw.write(text + "\n\n");
                    fw.close();
                    Log.d(TAG, "Finished appending - creating googleEmail intent...");
                    Email.sendEmail(new String[]{"matt.weber@medistimusa.com"}, "LogCat stuff, yo.", "LogCat data " +
                            "from MediBuddy", context, logFile);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    public static class Animations {

        public enum AnimationType {
            WOBBLER, PULSE, PULSE_HARDER
        }

        public static void pulseAnimation(View target) {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(target,
                    PropertyValuesHolder.ofFloat("scaleX", 1.00f),
                    PropertyValuesHolder.ofFloat("scaleY", 1.15f));
            scaleDown.setDuration(750);

            scaleDown.setRepeatCount(250);
            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

            scaleDown.start();
        }

        public static void pulseAnimation(View target, float scaleX, float scaleY, int repeatCount, int scaleDownDuration) {
            ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(target,
                    PropertyValuesHolder.ofFloat("scaleX", scaleX),
                    PropertyValuesHolder.ofFloat("scaleY", scaleY));
            scaleDown.setDuration(scaleDownDuration);

            if (repeatCount > 0) {
                scaleDown.setRepeatCount(repeatCount);
            }

            scaleDown.setRepeatMode(ObjectAnimator.REVERSE);

            scaleDown.start();
        }

        public static void fadeOut(View view, int duration, Animation.AnimationListener callback) {
            Animation fade = new AlphaAnimation(1, 0);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            animation.setAnimationListener(callback);

            view.setAnimation(animation);
        }

        public static void fadeIn(View view, int duration, Animation.AnimationListener callback) {
            Animation fade = new AlphaAnimation(0, 1);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            animation.setAnimationListener(callback);

            view.setAnimation(animation);
        }

        public static void fadeOut(View view, int duration) {
            Animation fade = new AlphaAnimation(1, 0);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            view.setAnimation(animation);
        }

        public static void fadeIn(View view, int duration) {
            Animation fade = new AlphaAnimation(0, 1);
            fade.setInterpolator(new AccelerateInterpolator()); //and this
            fade.setStartOffset(1000);
            fade.setDuration(duration);

            AnimationSet animation = new AnimationSet(false); //change to false
            animation.addAnimation(fade);

            view.setAnimation(animation);
        }

        /*public static void animateView(View view, Context context, AnimationType animationType) {
            int resourceId;
            switch (animationType) {
                case WOBBLER:
                    resourceId = R.anim.wobbler;
                    break;
                case PULSE_HARDER:
                    resourceId = R.anim.pulse_harder;
                    break;
                default:
                    resourceId = R.anim.pulse;
                    break;
            }
            final Animation b = AnimationUtils.loadAnimation(context, resourceId);
            b.reset();
            b.setRepeatCount(Animation.INFINITE);
            view.startAnimation(b);
        }*/

        public static Animation outToLeftAnimation() {
            Animation outtoLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            outtoLeft.setDuration(175);
            outtoLeft.setInterpolator(new AccelerateInterpolator());
            return outtoLeft;
        }

        public static Animation inFromLeftAnimation() {
            Animation inFromLeft = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            inFromLeft.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {

                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            inFromLeft.setDuration(175);
            inFromLeft.setInterpolator(new AccelerateInterpolator());
            return inFromLeft;
        }

        public static Animation outToRightAnimation() {
            Animation outtoRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            outtoRight.setDuration(175);
            outtoRight.setInterpolator(new AccelerateInterpolator());
            return outtoRight;
        }

        public static Animation inFromRightAnimation() {
            Animation inFromRight = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
            );
            inFromRight.setDuration(175);
            inFromRight.setInterpolator(new AccelerateInterpolator());
            return inFromRight;
        }

        public static Animation outToTop() {
            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 1.0f);
            animation.setDuration(175);
            animation.setInterpolator(new AccelerateInterpolator());
            return animation;
        }

        public static Animation inFromTop() {
            Animation animation = new TranslateAnimation(
                    Animation.RELATIVE_TO_PARENT, +0.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f,
                    Animation.RELATIVE_TO_PARENT, 1.0f,
                    Animation.RELATIVE_TO_PARENT, 0.0f);
            animation.setDuration(175);
            animation.setInterpolator(new AccelerateInterpolator());
            return animation;
        }
    }

    public static class Views {


    }

    public static class Strings {

        public static SpannableString makeUnderlined(String txt) {
            SpannableString content = new SpannableString(txt);
            content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
            return content;
        }

        public static byte[] decodeBase64(String encodedData) {
            return Base64.getDecoder().decode(encodedData.getBytes());
        }

        public static String encodeBase64(byte[] encodeMe) {
            byte[] encodedBytes = Base64.getEncoder().encode(encodeMe);
            return new String(encodedBytes);
        }

        public static String decodeBase64AsString(String encodedData) {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedData.getBytes());
            return new String(decodedBytes);
        }

        public static String encodeBase64(String encodeMe) {
            byte[] encodedBytes = Base64.getEncoder().encode(encodeMe.getBytes());
            return new String(encodedBytes);
        }

        public static class Views {
            /**
             * This class will detect left and right swipes and fire an event if detected for each view
             * added to the pool to be evaluated.
             */
            public static class MySwipeHandler {

                ArrayList<View> views = new ArrayList<>();
                // View view;
                GestureDetector gestureDetector;
                MySwipeListener mySwipeListener;
                private static final String TAG = "MySwipeListener";
                Context context;

                public interface MySwipeListener {
                    void onSwipeLeft();

                    void onSwipeRight();
                }

                public MySwipeHandler(ArrayList<View> views, MySwipeListener listener) {
                    this.views = views;
                    for (View view : this.views) {
                        this.addView(view);
                    }
                    this.mySwipeListener = listener;
                }

                public MySwipeHandler(View view, MySwipeListener listener) {
                    this.views = new ArrayList<>();
                    this.views.add(view);
                    this.addView(view);
                    this.mySwipeListener = listener;
                }

                public MySwipeHandler(MySwipeListener listener) {
                    this.mySwipeListener = listener;
                    this.gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {
                        @Override
                        public boolean onDown(MotionEvent motionEvent) {
                            return false;
                        }

                        @Override
                        public void onShowPress(MotionEvent motionEvent) {

                        }

                        @Override
                        public boolean onSingleTapUp(MotionEvent motionEvent) {
                            return false;
                        }

                        @Override
                        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                            return false;
                        }

                        @Override
                        public void onLongPress(MotionEvent motionEvent) {

                        }

                        @Override
                        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                            Log.i(TAG, "onFling !");
                            int SWIPE_MIN_DISTANCE = 120;
                            final int SWIPE_MAX_OFF_PATH = 250;
                            final int SWIPE_THRESHOLD_VELOCITY = 200;
                            try {
                                if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_PATH) {
                                    return false;
                                }
                                // right to left swipe
                                if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE
                                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                                    onLeftSwipe();
                                }
                                // left to right swipe
                                else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE
                                        && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
                                    onRightSwipe();
                                }
                            } catch (Exception ignored) { }
                            return true;
                        }
                    });
                }

                public void addView(View view) {
                    this.views.add(view);

                    for (View v : this.views) {
                        v.setOnTouchListener((view1, motionEvent) -> {
                            gestureDetector.onTouchEvent(motionEvent);
                            if (view1 != null) {
                                view1.performClick();
                            }
                            return false;
                        });
                    }
                }

                private void onLeftSwipe() {
                    Log.i(TAG, "onLeftSwipe ");
                    mySwipeListener.onSwipeLeft();
                }

                private void onRightSwipe() {
                    Log.i(TAG, "onRightSwipe ");
                    mySwipeListener.onSwipeRight();
                }
            }
        }

        public static class Permissions extends AppCompatActivity {

            private static final String TAG = "Permissions";

            /**
             * Checks if the specified permission is currently granted
             *
             * @param type The permission to evaluate
             * @return A boolean result
             */
            public static boolean isGranted(Context context, PermissionType type) {
                String permission = Permission.getPermission(type);
                int res = context.checkCallingOrSelfPermission(permission);
                return (res == PackageManager.PERMISSION_GRANTED);
            }

            /**
             * A simple container to house permissions that will be requested of the OS
             */
            public static class RequestContainer {
                private final ArrayList<String> permissions;

                public RequestContainer() {
                    permissions = new ArrayList<>();
                }

                /**
                 * Adds a permission string to the list if it isn't already present
                 */
                public void add(PermissionType permissionType) {
                    if (!exists(permissionType)) {
                        this.permissions.add(Permission.getPermission(permissionType));
                    }
                }

                /**
                 * Checks if a permission is already in the list.
                 *
                 * @param permissionType The permission to check for
                 * @return a bool
                 */
                public boolean exists(PermissionType permissionType) {
                    for (String p : this.permissions) {
                        if (p.equals(Permission.getPermission(permissionType))) {
                            return true;
                        }
                    }
                    return false;
                }

                /**
                 * Removes a permission from the list
                 */
                public void remove(PermissionType permissionType) {
                    for (int i = 0; i < this.permissions.size(); i++) {
                        String perm = this.permissions.get(i);
                        if (perm.equals(Permission.getPermission(permissionType))) {
                            this.permissions.remove(i);
                            return;
                        }
                    }
                }

                /**
                 * Converts the permissions list to a string array consumable by the OS' permission request methodology
                 *
                 * @return The permissions as a string array.
                 */
                public String[] toArray() {
                    String[] array = new String[permissions.size()];
                    for (int i = 0; i < this.permissions.size(); i++) {
                        array[i] = this.permissions.get(i);
                    }
                    return array;
                }

            }

            /**
             * An enumeration of permission names to (more easily) enable strongly typed permission handling
             */
            public enum PermissionType {
                ACCEPT_HANDOVER,
                ACCESS_BACKGROUND_LOCATION,
                ACCESS_CHECKIN_PROPERTIES,
                ACCESS_COARSE_LOCATION,
                ACCESS_FINE_LOCATION,
                ACCESS_LOCATION_EXTRA_COMMANDS,
                ACCESS_MEDIA_LOCATION,
                ACCESS_NETWORK_STATE,
                ACCESS_NOTIFICATION_POLICY,
                ACCESS_WIFI_STATE,
                ACCOUNT_MANAGER,
                ACTIVITY_RECOGNITION,
                ADD_VOICEMAIL,
                ANSWER_PHONE_CALLS,
                BATTERY_STATS,
                BIND_ACCESSIBILITY_SERVICE,
                BIND_APPWIDGET,
                BIND_AUTOFILL_SERVICE,
                BIND_CALL_REDIRECTION_SERVICE,
                BIND_CARRIER_MESSAGING_CLIENT_SERVICE,

                BIND_CARRIER_MESSAGING_SERVICE,
                BIND_CARRIER_SERVICES,
                BIND_CHOOSER_TARGET_SERVICE,
                BIND_CONDITION_PROVIDER_SERVICE,
                BIND_DEVICE_ADMIN,
                BIND_DREAM_SERVICE,
                BIND_INCALL_SERVICE,
                BIND_INPUT_METHOD,
                BIND_MIDI_DEVICE_SERVICE,
                BIND_NFC_SERVICE,
                BIND_NOTIFICATION_LISTENER_SERVICE,
                BIND_PRINT_SERVICE,
                BIND_QUICK_SETTINGS_TILE,
                BIND_REMOTEVIEWS,
                BIND_SCREENING_SERVICE,
                BIND_TELECOM_CONNECTION_SERVICE,
                BIND_TEXT_SERVICE,
                BIND_TV_INPUT,
                BIND_VISUAL_VOICEMAIL_SERVICE,
                BIND_VOICE_INTERACTION,
                BIND_VPN_SERVICE,
                BIND_VR_LISTENER_SERVICE,
                BIND_WALLPAPER,
                BLUETOOTH,
                BLUETOOTH_ADMIN,
                BLUETOOTH_PRIVILEGED,
                BODY_SENSORS,
                BROADCAST_PACKAGE_REMOVED,
                BROADCAST_SMS,
                BROADCAST_STICKY,
                BROADCAST_WAP_PUSH,
                CALL_COMPANION_APP,
                CALL_PHONE,
                CALL_PRIVILEGED,
                CAMERA,
                CAPTURE_AUDIO_OUTPUT,
                CHANGE_COMPONENT_ENABLED_STATE,
                CHANGE_CONFIGURATION,
                CHANGE_NETWORK_STATE,
                CHANGE_WIFI_MULTICAST_STATE,
                CHANGE_WIFI_STATE,
                CLEAR_APP_CACHE,
                CONTROL_LOCATION_UPDATES,
                DELETE_CACHE_FILES,
                DELETE_PACKAGES,
                DIAGNOSTIC,
                DISABLE_KEYGUARD,
                DUMP,
                EXPAND_STATUS_BAR,
                FACTORY_TEST,
                FOREGROUND_SERVICE,
                GET_ACCOUNTS,
                GET_ACCOUNTS_PRIVILEGED,
                GET_PACKAGE_SIZE,

                GET_TASKS,
                GLOBAL_SEARCH,
                INSTALL_LOCATION_PROVIDER,
                INSTALL_PACKAGES,
                INSTALL_SHORTCUT,
                INSTANT_APP_FOREGROUND_SERVICE,
                INTERNET,
                KILL_BACKGROUND_PROCESSES,
                LOCATION_HARDWARE,
                MANAGE_DOCUMENTS,
                MANAGE_OWN_CALLS,
                MASTER_CLEAR,
                MEDIA_CONTENT_CONTROL,
                MODIFY_AUDIO_SETTINGS,
                MODIFY_PHONE_STATE,
                MOUNT_FORMAT_FILESYSTEMS,
                MOUNT_UNMOUNT_FILESYSTEMS,
                NFC,
                NFC_TRANSACTION_EVENT,
                PACKAGE_USAGE_STATS,

                PERSISTENT_ACTIVITY,

                PROCESS_OUTGOING_CALLS,
                READ_CALENDAR,
                READ_CALL_LOG,
                READ_CONTACTS,
                READ_EXTERNAL_STORAGE,

                READ_INPUT_STATE,
                READ_LOGS,
                READ_PHONE_NUMBERS,
                READ_PHONE_STATE,
                READ_SMS,
                READ_SYNC_SETTINGS,
                READ_SYNC_STATS,
                READ_VOICEMAIL,
                REBOOT,
                RECEIVE_BOOT_COMPLETED,
                RECEIVE_MMS,
                RECEIVE_SMS,
                RECEIVE_WAP_PUSH,
                RECORD_AUDIO,
                REORDER_TASKS,
                REQUEST_COMPANION_RUN_IN_BACKGROUND,
                REQUEST_COMPANION_USE_DATA_IN_BACKGROUND,
                REQUEST_DELETE_PACKAGES,
                REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                REQUEST_INSTALL_PACKAGES,
                REQUEST_PASSWORD_COMPLEXITY,

                RESTART_PACKAGES,
                SEND_RESPOND_VIA_MESSAGE,
                SEND_SMS,
                SET_ALARM,
                SET_ALWAYS_FINISH,
                SET_ANIMATION_SCALE,
                SET_DEBUG_APP,

                SET_PREFERRED_APPLICATIONS,
                SET_PROCESS_LIMIT,
                SET_TIME,
                SET_TIME_ZONE,
                SET_WALLPAPER,
                SET_WALLPAPER_HINTS,
                SIGNAL_PERSISTENT_PROCESSES,
                SMS_FINANCIAL_TRANSACTIONS,
                STATUS_BAR,
                SYSTEM_ALERT_WINDOW,
                TRANSMIT_IR,
                UNINSTALL_SHORTCUT,
                UPDATE_DEVICE_STATS,
                USE_BIOMETRIC,

                USE_FINGERPRINT,
                USE_FULL_SCREEN_INTENT,
                USE_SIP,
                VIBRATE,
                WAKE_LOCK,
                WRITE_APN_SETTINGS,
                WRITE_CALENDAR,
                WRITE_CALL_LOG,
                WRITE_CONTACTS,
                WRITE_EXTERNAL_STORAGE,
                WRITE_GSERVICES,
                WRITE_SECURE_SETTINGS,
                WRITE_SETTINGS,
                WRITE_SYNC_SETTINGS,
                WRITE_VOICEMAIL,
            }

            public static class Permission {

                /**
                 * Returns the Android permission string as stipulated in the Manifest class
                 *
                 * @param value The permission type to find a string value for
                 * @return The official permission string ex: "android.permission.ACCESS_BACKGROUND_LOCATION"
                 */
                public static String getPermission(PermissionType value) {
                    switch (value) {
                        case ACCEPT_HANDOVER:
                            return ACCEPT_HANDOVER;
                        case ACCESS_BACKGROUND_LOCATION:
                            return ACCESS_BACKGROUND_LOCATION;
                        case ACCESS_CHECKIN_PROPERTIES:
                            return ACCESS_CHECKIN_PROPERTIES;
                        case ACCESS_COARSE_LOCATION:
                            return ACCESS_COARSE_LOCATION;
                        case ACCESS_FINE_LOCATION:
                            return ACCESS_FINE_LOCATION;
                        case ACCESS_LOCATION_EXTRA_COMMANDS:
                            return ACCESS_LOCATION_EXTRA_COMMANDS;
                        case ACCESS_MEDIA_LOCATION:
                            return ACCESS_MEDIA_LOCATION;
                        case ACCESS_NETWORK_STATE:
                            return ACCESS_NETWORK_STATE;
                        case ACCESS_NOTIFICATION_POLICY:
                            return ACCESS_NOTIFICATION_POLICY;
                        case ACCESS_WIFI_STATE:
                            return ACCESS_WIFI_STATE;
                        case ACCOUNT_MANAGER:
                            return ACCOUNT_MANAGER;
                        case ACTIVITY_RECOGNITION:
                            return ACTIVITY_RECOGNITION;
                        case ADD_VOICEMAIL:
                            return ADD_VOICEMAIL;
                        case ANSWER_PHONE_CALLS:
                            return ANSWER_PHONE_CALLS;
                        case BATTERY_STATS:
                            return BATTERY_STATS;
                        case BIND_ACCESSIBILITY_SERVICE:
                            return BIND_ACCESSIBILITY_SERVICE;
                        case BIND_APPWIDGET:
                            return BIND_APPWIDGET;
                        case BIND_AUTOFILL_SERVICE:
                            return BIND_AUTOFILL_SERVICE;
                        case BIND_CALL_REDIRECTION_SERVICE:
                            return BIND_CALL_REDIRECTION_SERVICE;
                        case BIND_CARRIER_MESSAGING_CLIENT_SERVICE:
                            return BIND_CARRIER_MESSAGING_CLIENT_SERVICE;
                        case BIND_CARRIER_SERVICES:
                            return BIND_CARRIER_SERVICES;
                        case BIND_CHOOSER_TARGET_SERVICE:
                            return BIND_CHOOSER_TARGET_SERVICE;
                        case BIND_CONDITION_PROVIDER_SERVICE:
                            return BIND_CONDITION_PROVIDER_SERVICE;
                        case BIND_DEVICE_ADMIN:
                            return BIND_DEVICE_ADMIN;
                        case BIND_DREAM_SERVICE:
                            return BIND_DREAM_SERVICE;
                        case BIND_INCALL_SERVICE:
                            return BIND_INCALL_SERVICE;
                        case BIND_INPUT_METHOD:
                            return BIND_INPUT_METHOD;
                        case BIND_MIDI_DEVICE_SERVICE:
                            return BIND_MIDI_DEVICE_SERVICE;
                        case BIND_NFC_SERVICE:
                            return BIND_NFC_SERVICE;
                        case BIND_NOTIFICATION_LISTENER_SERVICE:
                            return BIND_NOTIFICATION_LISTENER_SERVICE;
                        case BIND_PRINT_SERVICE:
                            return BIND_PRINT_SERVICE;
                        case BIND_QUICK_SETTINGS_TILE:
                            return BIND_QUICK_SETTINGS_TILE;
                        case BIND_REMOTEVIEWS:
                            return BIND_REMOTEVIEWS;
                        case BIND_SCREENING_SERVICE:
                            return BIND_SCREENING_SERVICE;
                        case BIND_TELECOM_CONNECTION_SERVICE:
                            return BIND_TELECOM_CONNECTION_SERVICE;
                        case BIND_TEXT_SERVICE:
                            return BIND_TEXT_SERVICE;
                        case BIND_TV_INPUT:
                            return BIND_TV_INPUT;
                        case BIND_VISUAL_VOICEMAIL_SERVICE:
                            return BIND_VISUAL_VOICEMAIL_SERVICE;
                        case BIND_VOICE_INTERACTION:
                            return BIND_VOICE_INTERACTION;
                        case BIND_VPN_SERVICE:
                            return BIND_VPN_SERVICE;
                        case BIND_VR_LISTENER_SERVICE:
                            return BIND_VR_LISTENER_SERVICE;
                        case BIND_WALLPAPER:
                            return BIND_WALLPAPER;
                        case BLUETOOTH:
                            return BLUETOOTH;
                        case BLUETOOTH_ADMIN:
                            return BLUETOOTH_ADMIN;
                        case BLUETOOTH_PRIVILEGED:
                            return BLUETOOTH_PRIVILEGED;
                        case BODY_SENSORS:
                            return BODY_SENSORS;
                        case BROADCAST_PACKAGE_REMOVED:
                            return BROADCAST_PACKAGE_REMOVED;
                        case BROADCAST_SMS:
                            return BROADCAST_SMS;
                        case BROADCAST_STICKY:
                            return BROADCAST_STICKY;
                        case BROADCAST_WAP_PUSH:
                            return BROADCAST_WAP_PUSH;
                        case CALL_COMPANION_APP:
                            return CALL_COMPANION_APP;
                        case CALL_PHONE:
                            return CALL_PHONE;
                        case CALL_PRIVILEGED:
                            return CALL_PRIVILEGED;
                        case CAMERA:
                            return CAMERA;
                        case CAPTURE_AUDIO_OUTPUT:
                            return CAPTURE_AUDIO_OUTPUT;
                        case CHANGE_COMPONENT_ENABLED_STATE:
                            return CHANGE_COMPONENT_ENABLED_STATE;
                        case CHANGE_CONFIGURATION:
                            return CHANGE_CONFIGURATION;
                        case CHANGE_NETWORK_STATE:
                            return CHANGE_NETWORK_STATE;
                        case CHANGE_WIFI_MULTICAST_STATE:
                            return CHANGE_WIFI_MULTICAST_STATE;
                        case CHANGE_WIFI_STATE:
                            return CHANGE_WIFI_STATE;
                        case CLEAR_APP_CACHE:
                            return CLEAR_APP_CACHE;
                        case CONTROL_LOCATION_UPDATES:
                            return CONTROL_LOCATION_UPDATES;
                        case DELETE_CACHE_FILES:
                            return DELETE_CACHE_FILES;
                        case DELETE_PACKAGES:
                            return DELETE_PACKAGES;
                        case DIAGNOSTIC:
                            return DIAGNOSTIC;
                        case DISABLE_KEYGUARD:
                            return DISABLE_KEYGUARD;
                        case DUMP:
                            return DUMP;
                        case EXPAND_STATUS_BAR:
                            return EXPAND_STATUS_BAR;
                        case FACTORY_TEST:
                            return FACTORY_TEST;
                        case FOREGROUND_SERVICE:
                            return FOREGROUND_SERVICE;
                        case GET_ACCOUNTS:
                            return GET_ACCOUNTS;
                        case GET_ACCOUNTS_PRIVILEGED:
                            return GET_ACCOUNTS_PRIVILEGED;
                        case GET_PACKAGE_SIZE:
                            return GET_PACKAGE_SIZE;
                        case GLOBAL_SEARCH:
                            return GLOBAL_SEARCH;
                        case INSTALL_LOCATION_PROVIDER:
                            return INSTALL_LOCATION_PROVIDER;
                        case INSTALL_PACKAGES:
                            return INSTALL_PACKAGES;
                        case INSTALL_SHORTCUT:
                            return INSTALL_SHORTCUT;
                        case INSTANT_APP_FOREGROUND_SERVICE:
                            return INSTANT_APP_FOREGROUND_SERVICE;
                        case INTERNET:
                            return INTERNET;
                        case KILL_BACKGROUND_PROCESSES:
                            return KILL_BACKGROUND_PROCESSES;
                        case LOCATION_HARDWARE:
                            return LOCATION_HARDWARE;
                        case MANAGE_DOCUMENTS:
                            return MANAGE_DOCUMENTS;
                        case MANAGE_OWN_CALLS:
                            return MANAGE_OWN_CALLS;
                        case MASTER_CLEAR:
                            return MASTER_CLEAR;
                        case MEDIA_CONTENT_CONTROL:
                            return MEDIA_CONTENT_CONTROL;
                        case MODIFY_AUDIO_SETTINGS:
                            return MODIFY_AUDIO_SETTINGS;
                        case MODIFY_PHONE_STATE:
                            return MODIFY_PHONE_STATE;
                        case MOUNT_FORMAT_FILESYSTEMS:
                            return MOUNT_FORMAT_FILESYSTEMS;
                        case MOUNT_UNMOUNT_FILESYSTEMS:
                            return MOUNT_UNMOUNT_FILESYSTEMS;
                        case NFC:
                            return NFC;
                        case NFC_TRANSACTION_EVENT:
                            return NFC_TRANSACTION_EVENT;
                        case PACKAGE_USAGE_STATS:
                            return PACKAGE_USAGE_STATS;
                        case READ_CALENDAR:
                            return READ_CALENDAR;
                        case READ_CALL_LOG:
                            return READ_CALL_LOG;
                        case READ_CONTACTS:
                            return READ_CONTACTS;
                        case READ_LOGS:
                            return READ_LOGS;
                        case READ_PHONE_NUMBERS:
                            return READ_PHONE_NUMBERS;
                        case READ_PHONE_STATE:
                            return READ_PHONE_STATE;
                        case READ_SMS:
                            return READ_SMS;
                        case READ_SYNC_SETTINGS:
                            return READ_SYNC_SETTINGS;
                        case READ_SYNC_STATS:
                            return READ_SYNC_STATS;
                        case READ_VOICEMAIL:
                            return READ_VOICEMAIL;
                        case REBOOT:
                            return REBOOT;
                        case RECEIVE_BOOT_COMPLETED:
                            return RECEIVE_BOOT_COMPLETED;
                        case RECEIVE_MMS:
                            return RECEIVE_MMS;
                        case RECEIVE_SMS:
                            return RECEIVE_SMS;
                        case RECEIVE_WAP_PUSH:
                            return RECEIVE_WAP_PUSH;
                        case RECORD_AUDIO:
                            return RECORD_AUDIO;
                        case REORDER_TASKS:
                            return REORDER_TASKS;
                        case REQUEST_COMPANION_RUN_IN_BACKGROUND:
                            return REQUEST_COMPANION_RUN_IN_BACKGROUND;
                        case REQUEST_COMPANION_USE_DATA_IN_BACKGROUND:
                            return REQUEST_COMPANION_USE_DATA_IN_BACKGROUND;
                        case REQUEST_DELETE_PACKAGES:
                            return REQUEST_DELETE_PACKAGES;
                        case REQUEST_IGNORE_BATTERY_OPTIMIZATIONS:
                            return REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
                        case REQUEST_INSTALL_PACKAGES:
                            return REQUEST_INSTALL_PACKAGES;
                        case REQUEST_PASSWORD_COMPLEXITY:
                            return REQUEST_PASSWORD_COMPLEXITY;
                        case SEND_RESPOND_VIA_MESSAGE:
                            return SEND_RESPOND_VIA_MESSAGE;
                        case SEND_SMS:
                            return SEND_SMS;
                        case SET_ALARM:
                            return SET_ALARM;
                        case SET_ALWAYS_FINISH:
                            return SET_ALWAYS_FINISH;
                        case SET_ANIMATION_SCALE:
                            return SET_ANIMATION_SCALE;
                        case SET_DEBUG_APP:
                            return SET_DEBUG_APP;
                        case SET_PROCESS_LIMIT:
                            return SET_PROCESS_LIMIT;
                        case SET_TIME:
                            return SET_TIME;
                        case SET_TIME_ZONE:
                            return SET_TIME_ZONE;
                        case SET_WALLPAPER:
                            return SET_WALLPAPER;
                        case SET_WALLPAPER_HINTS:
                            return SET_WALLPAPER_HINTS;
                        case SIGNAL_PERSISTENT_PROCESSES:
                            return SIGNAL_PERSISTENT_PROCESSES;
                        case SMS_FINANCIAL_TRANSACTIONS:
                            return SMS_FINANCIAL_TRANSACTIONS;
                        case STATUS_BAR:
                            return STATUS_BAR;
                        case SYSTEM_ALERT_WINDOW:
                            return SYSTEM_ALERT_WINDOW;
                        case TRANSMIT_IR:
                            return TRANSMIT_IR;
                        case UNINSTALL_SHORTCUT:
                            return UNINSTALL_SHORTCUT;
                        case UPDATE_DEVICE_STATS:
                            return UPDATE_DEVICE_STATS;
                        case USE_BIOMETRIC:
                            return USE_BIOMETRIC;
                        case USE_FULL_SCREEN_INTENT:
                            return USE_FULL_SCREEN_INTENT;
                        case USE_SIP:
                            return USE_SIP;
                        case VIBRATE:
                            return VIBRATE;
                        case WAKE_LOCK:
                            return WAKE_LOCK;
                        case WRITE_APN_SETTINGS:
                            return WRITE_APN_SETTINGS;
                        case WRITE_CALENDAR:
                            return WRITE_CALENDAR;
                        case WRITE_CALL_LOG:
                            return WRITE_CALL_LOG;
                        case WRITE_CONTACTS:
                            return WRITE_CONTACTS;
                        case WRITE_EXTERNAL_STORAGE:
                            return WRITE_EXTERNAL_STORAGE;
                        case WRITE_GSERVICES:
                            return WRITE_GSERVICES;
                        case WRITE_SECURE_SETTINGS:
                            return WRITE_SECURE_SETTINGS;
                        case WRITE_SETTINGS:
                            return WRITE_SETTINGS;
                        case WRITE_SYNC_SETTINGS:
                            return WRITE_SYNC_SETTINGS;
                        case WRITE_VOICEMAIL:
                            return WRITE_VOICEMAIL;
                        default:
                            return READ_EXTERNAL_STORAGE;
                    }
                }

                public static final String ACCEPT_HANDOVER = "android.permission.ACCEPT_HANDOVER";
                public static final String ACCESS_BACKGROUND_LOCATION = "android.permission.ACCESS_BACKGROUND_LOCATION";
                public static final String ACCESS_CHECKIN_PROPERTIES = "android.permission.ACCESS_CHECKIN_PROPERTIES";
                public static final String ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION";
                public static final String ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION";
                public static final String ACCESS_LOCATION_EXTRA_COMMANDS = "android.permission.ACCESS_LOCATION_EXTRA_COMMANDS";
                public static final String ACCESS_MEDIA_LOCATION = "android.permission.ACCESS_MEDIA_LOCATION";
                public static final String ACCESS_NETWORK_STATE = "android.permission.ACCESS_NETWORK_STATE";
                public static final String ACCESS_NOTIFICATION_POLICY = "android.permission.ACCESS_NOTIFICATION_POLICY";
                public static final String ACCESS_WIFI_STATE = "android.permission.ACCESS_WIFI_STATE";
                public static final String ACCOUNT_MANAGER = "android.permission.ACCOUNT_MANAGER";
                public static final String ACTIVITY_RECOGNITION = "android.permission.ACTIVITY_RECOGNITION";
                public static final String ADD_VOICEMAIL = "com.android.voicemail.permission.ADD_VOICEMAIL";
                public static final String ANSWER_PHONE_CALLS = "android.permission.ANSWER_PHONE_CALLS";
                public static final String BATTERY_STATS = "android.permission.BATTERY_STATS";
                public static final String BIND_ACCESSIBILITY_SERVICE = "android.permission.BIND_ACCESSIBILITY_SERVICE";
                public static final String BIND_APPWIDGET = "android.permission.BIND_APPWIDGET";
                public static final String BIND_AUTOFILL_SERVICE = "android.permission.BIND_AUTOFILL_SERVICE";
                public static final String BIND_CALL_REDIRECTION_SERVICE = "android.permission.BIND_CALL_REDIRECTION_SERVICE";
                public static final String BIND_CARRIER_MESSAGING_CLIENT_SERVICE = "android.permission.BIND_CARRIER_MESSAGING_CLIENT_SERVICE";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String BIND_CARRIER_MESSAGING_SERVICE = "android.permission.BIND_CARRIER_MESSAGING_SERVICE";
                public static final String BIND_CARRIER_SERVICES = "android.permission.BIND_CARRIER_SERVICES";
                public static final String BIND_CHOOSER_TARGET_SERVICE = "android.permission.BIND_CHOOSER_TARGET_SERVICE";
                public static final String BIND_CONDITION_PROVIDER_SERVICE = "android.permission.BIND_CONDITION_PROVIDER_SERVICE";
                public static final String BIND_DEVICE_ADMIN = "android.permission.BIND_DEVICE_ADMIN";
                public static final String BIND_DREAM_SERVICE = "android.permission.BIND_DREAM_SERVICE";
                public static final String BIND_INCALL_SERVICE = "android.permission.BIND_INCALL_SERVICE";
                public static final String BIND_INPUT_METHOD = "android.permission.BIND_INPUT_METHOD";
                public static final String BIND_MIDI_DEVICE_SERVICE = "android.permission.BIND_MIDI_DEVICE_SERVICE";
                public static final String BIND_NFC_SERVICE = "android.permission.BIND_NFC_SERVICE";
                public static final String BIND_NOTIFICATION_LISTENER_SERVICE = "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE";
                public static final String BIND_PRINT_SERVICE = "android.permission.BIND_PRINT_SERVICE";
                public static final String BIND_QUICK_SETTINGS_TILE = "android.permission.BIND_QUICK_SETTINGS_TILE";
                public static final String BIND_REMOTEVIEWS = "android.permission.BIND_REMOTEVIEWS";
                public static final String BIND_SCREENING_SERVICE = "android.permission.BIND_SCREENING_SERVICE";
                public static final String BIND_TELECOM_CONNECTION_SERVICE = "android.permission.BIND_TELECOM_CONNECTION_SERVICE";
                public static final String BIND_TEXT_SERVICE = "android.permission.BIND_TEXT_SERVICE";
                public static final String BIND_TV_INPUT = "android.permission.BIND_TV_INPUT";
                public static final String BIND_VISUAL_VOICEMAIL_SERVICE = "android.permission.BIND_VISUAL_VOICEMAIL_SERVICE";
                public static final String BIND_VOICE_INTERACTION = "android.permission.BIND_VOICE_INTERACTION";
                public static final String BIND_VPN_SERVICE = "android.permission.BIND_VPN_SERVICE";
                public static final String BIND_VR_LISTENER_SERVICE = "android.permission.BIND_VR_LISTENER_SERVICE";
                public static final String BIND_WALLPAPER = "android.permission.BIND_WALLPAPER";
                public static final String BLUETOOTH = "android.permission.BLUETOOTH";
                public static final String BLUETOOTH_ADMIN = "android.permission.BLUETOOTH_ADMIN";
                public static final String BLUETOOTH_PRIVILEGED = "android.permission.BLUETOOTH_PRIVILEGED";
                public static final String BODY_SENSORS = "android.permission.BODY_SENSORS";
                public static final String BROADCAST_PACKAGE_REMOVED = "android.permission.BROADCAST_PACKAGE_REMOVED";
                public static final String BROADCAST_SMS = "android.permission.BROADCAST_SMS";
                public static final String BROADCAST_STICKY = "android.permission.BROADCAST_STICKY";
                public static final String BROADCAST_WAP_PUSH = "android.permission.BROADCAST_WAP_PUSH";
                public static final String CALL_COMPANION_APP = "android.permission.CALL_COMPANION_APP";
                public static final String CALL_PHONE = "android.permission.CALL_PHONE";
                public static final String CALL_PRIVILEGED = "android.permission.CALL_PRIVILEGED";
                public static final String CAMERA = "android.permission.CAMERA";
                public static final String CAPTURE_AUDIO_OUTPUT = "android.permission.CAPTURE_AUDIO_OUTPUT";
                public static final String CHANGE_COMPONENT_ENABLED_STATE = "android.permission.CHANGE_COMPONENT_ENABLED_STATE";
                public static final String CHANGE_CONFIGURATION = "android.permission.CHANGE_CONFIGURATION";
                public static final String CHANGE_NETWORK_STATE = "android.permission.CHANGE_NETWORK_STATE";
                public static final String CHANGE_WIFI_MULTICAST_STATE = "android.permission.CHANGE_WIFI_MULTICAST_STATE";
                public static final String CHANGE_WIFI_STATE = "android.permission.CHANGE_WIFI_STATE";
                public static final String CLEAR_APP_CACHE = "android.permission.CLEAR_APP_CACHE";
                public static final String CONTROL_LOCATION_UPDATES = "android.permission.CONTROL_LOCATION_UPDATES";
                public static final String DELETE_CACHE_FILES = "android.permission.DELETE_CACHE_FILES";
                public static final String DELETE_PACKAGES = "android.permission.DELETE_PACKAGES";
                public static final String DIAGNOSTIC = "android.permission.DIAGNOSTIC";
                public static final String DISABLE_KEYGUARD = "android.permission.DISABLE_KEYGUARD";
                public static final String DUMP = "android.permission.DUMP";
                public static final String EXPAND_STATUS_BAR = "android.permission.EXPAND_STATUS_BAR";
                public static final String FACTORY_TEST = "android.permission.FACTORY_TEST";
                public static final String FOREGROUND_SERVICE = "android.permission.FOREGROUND_SERVICE";
                public static final String GET_ACCOUNTS = "android.permission.GET_ACCOUNTS";
                public static final String GET_ACCOUNTS_PRIVILEGED = "android.permission.GET_ACCOUNTS_PRIVILEGED";
                public static final String GET_PACKAGE_SIZE = "android.permission.GET_PACKAGE_SIZE";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String GET_TASKS = "android.permission.GET_TASKS";
                public static final String GLOBAL_SEARCH = "android.permission.GLOBAL_SEARCH";
                public static final String INSTALL_LOCATION_PROVIDER = "android.permission.INSTALL_LOCATION_PROVIDER";
                public static final String INSTALL_PACKAGES = "android.permission.INSTALL_PACKAGES";
                public static final String INSTALL_SHORTCUT = "com.android.launcher.permission.INSTALL_SHORTCUT";
                public static final String INSTANT_APP_FOREGROUND_SERVICE = "android.permission.INSTANT_APP_FOREGROUND_SERVICE";
                public static final String INTERNET = "android.permission.INTERNET";
                public static final String KILL_BACKGROUND_PROCESSES = "android.permission.KILL_BACKGROUND_PROCESSES";
                public static final String LOCATION_HARDWARE = "android.permission.LOCATION_HARDWARE";
                public static final String MANAGE_DOCUMENTS = "android.permission.MANAGE_DOCUMENTS";
                public static final String MANAGE_OWN_CALLS = "android.permission.MANAGE_OWN_CALLS";
                public static final String MASTER_CLEAR = "android.permission.MASTER_CLEAR";
                public static final String MEDIA_CONTENT_CONTROL = "android.permission.MEDIA_CONTENT_CONTROL";
                public static final String MODIFY_AUDIO_SETTINGS = "android.permission.MODIFY_AUDIO_SETTINGS";
                public static final String MODIFY_PHONE_STATE = "android.permission.MODIFY_PHONE_STATE";
                public static final String MOUNT_FORMAT_FILESYSTEMS = "android.permission.MOUNT_FORMAT_FILESYSTEMS";
                public static final String MOUNT_UNMOUNT_FILESYSTEMS = "android.permission.MOUNT_UNMOUNT_FILESYSTEMS";
                public static final String NFC = "android.permission.NFC";
                public static final String NFC_TRANSACTION_EVENT = "android.permission.NFC_TRANSACTION_EVENT";
                public static final String PACKAGE_USAGE_STATS = "android.permission.PACKAGE_USAGE_STATS";
                @Deprecated
                public static final String PROCESS_OUTGOING_CALLS = "android.permission.PROCESS_OUTGOING_CALLS";
                public static final String READ_CALENDAR = "android.permission.READ_CALENDAR";
                public static final String READ_CALL_LOG = "android.permission.READ_CALL_LOG";
                public static final String READ_CONTACTS = "android.permission.READ_CONTACTS";
                public static final String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String READ_INPUT_STATE = "android.permission.READ_INPUT_STATE";
                public static final String READ_LOGS = "android.permission.READ_LOGS";
                public static final String READ_PHONE_NUMBERS = "android.permission.READ_PHONE_NUMBERS";
                public static final String READ_PHONE_STATE = "android.permission.READ_PHONE_STATE";
                public static final String READ_SMS = "android.permission.READ_SMS";
                public static final String READ_SYNC_SETTINGS = "android.permission.READ_SYNC_SETTINGS";
                public static final String READ_SYNC_STATS = "android.permission.READ_SYNC_STATS";
                public static final String READ_VOICEMAIL = "com.android.voicemail.permission.READ_VOICEMAIL";
                public static final String REBOOT = "android.permission.REBOOT";
                public static final String RECEIVE_BOOT_COMPLETED = "android.permission.RECEIVE_BOOT_COMPLETED";
                public static final String RECEIVE_MMS = "android.permission.RECEIVE_MMS";
                public static final String RECEIVE_SMS = "android.permission.RECEIVE_SMS";
                public static final String RECEIVE_WAP_PUSH = "android.permission.RECEIVE_WAP_PUSH";
                public static final String RECORD_AUDIO = "android.permission.RECORD_AUDIO";
                public static final String REORDER_TASKS = "android.permission.REORDER_TASKS";
                public static final String REQUEST_COMPANION_RUN_IN_BACKGROUND = "android.permission.REQUEST_COMPANION_RUN_IN_BACKGROUND";
                public static final String REQUEST_COMPANION_USE_DATA_IN_BACKGROUND = "android.permission.REQUEST_COMPANION_USE_DATA_IN_BACKGROUND";
                public static final String REQUEST_DELETE_PACKAGES = "android.permission.REQUEST_DELETE_PACKAGES";
                public static final String REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS";
                public static final String REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES";
                public static final String REQUEST_PASSWORD_COMPLEXITY = "android.permission.REQUEST_PASSWORD_COMPLEXITY";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String RESTART_PACKAGES = "android.permission.RESTART_PACKAGES";
                public static final String SEND_RESPOND_VIA_MESSAGE = "android.permission.SEND_RESPOND_VIA_MESSAGE";
                public static final String SEND_SMS = "android.permission.SEND_SMS";
                public static final String SET_ALARM = "com.android.alarm.permission.SET_ALARM";
                public static final String SET_ALWAYS_FINISH = "android.permission.SET_ALWAYS_FINISH";
                public static final String SET_ANIMATION_SCALE = "android.permission.SET_ANIMATION_SCALE";
                public static final String SET_DEBUG_APP = "android.permission.SET_DEBUG_APP";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String SET_PREFERRED_APPLICATIONS = "android.permission.SET_PREFERRED_APPLICATIONS";
                public static final String SET_PROCESS_LIMIT = "android.permission.SET_PROCESS_LIMIT";
                public static final String SET_TIME = "android.permission.SET_TIME";
                public static final String SET_TIME_ZONE = "android.permission.SET_TIME_ZONE";
                public static final String SET_WALLPAPER = "android.permission.SET_WALLPAPER";
                public static final String SET_WALLPAPER_HINTS = "android.permission.SET_WALLPAPER_HINTS";
                public static final String SIGNAL_PERSISTENT_PROCESSES = "android.permission.SIGNAL_PERSISTENT_PROCESSES";
                public static final String SMS_FINANCIAL_TRANSACTIONS = "android.permission.SMS_FINANCIAL_TRANSACTIONS";
                public static final String STATUS_BAR = "android.permission.STATUS_BAR";
                public static final String SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";
                public static final String TRANSMIT_IR = "android.permission.TRANSMIT_IR";
                public static final String UNINSTALL_SHORTCUT = "com.android.launcher.permission.UNINSTALL_SHORTCUT";
                public static final String UPDATE_DEVICE_STATS = "android.permission.UPDATE_DEVICE_STATS";
                public static final String USE_BIOMETRIC = "android.permission.USE_BIOMETRIC";
                /**
                 * @deprecated
                 */
                @Deprecated
                public static final String USE_FINGERPRINT = "android.permission.USE_FINGERPRINT";
                public static final String USE_FULL_SCREEN_INTENT = "android.permission.USE_FULL_SCREEN_INTENT";
                public static final String USE_SIP = "android.permission.USE_SIP";
                public static final String VIBRATE = "android.permission.VIBRATE";
                public static final String WAKE_LOCK = "android.permission.WAKE_LOCK";
                public static final String WRITE_APN_SETTINGS = "android.permission.WRITE_APN_SETTINGS";
                public static final String WRITE_CALENDAR = "android.permission.WRITE_CALENDAR";
                public static final String WRITE_CALL_LOG = "android.permission.WRITE_CALL_LOG";
                public static final String WRITE_CONTACTS = "android.permission.WRITE_CONTACTS";
                public static final String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
                public static final String WRITE_GSERVICES = "android.permission.WRITE_GSERVICES";
                public static final String WRITE_SECURE_SETTINGS = "android.permission.WRITE_SECURE_SETTINGS";
                public static final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
                public static final String WRITE_SYNC_SETTINGS = "android.permission.WRITE_SYNC_SETTINGS";
                public static final String WRITE_VOICEMAIL = "com.android.voicemail.permission.WRITE_VOICEMAIL";
            }
        }
    }
}
