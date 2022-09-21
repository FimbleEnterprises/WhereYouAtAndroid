package com.fimbleenterprises.whereyouat.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;

@SuppressWarnings("unused")
public class MyGeoUtil {

    /**
     * Returns a Location object from the supplied LatLng object.
     * Note that only lat and lng are really populated.
     **/
    public static Location createLocFromLatLng(LatLng ll) {
        Location location = new Location("");
        location.setLatitude(ll.latitude);
        location.setLongitude(ll.longitude);
        location.setTime(System.currentTimeMillis());
        location.setAccuracy(0);

        return location;
    }

    public static String calculateBearing(float bearing) {

        String prettyBearing = "";

        if ((bearing >= 337.5 && bearing <= 360) || (bearing >= 0 && bearing < 22.5)) {
            prettyBearing = "N";
        }

        if (bearing >= 22.5 && bearing < 67.5) {
            prettyBearing = "NE";
        }

        if (bearing >= 67.5 && bearing < 112.5) {
            prettyBearing = "E";
        }

        if (bearing >= 112.5 && bearing < 157.5) {
            prettyBearing = "SE";
        }

        if (bearing >= 157.5 && bearing < 202.5) {
            prettyBearing = "S";
        }

        if (bearing >= 202.5 && bearing < 247.5) {
            prettyBearing = "SW";
        }

        if (bearing >= 247.5 && bearing < 292.5) {
            prettyBearing = "W";
        }

        if (bearing >= 292.5 && bearing < 337.5) {
            prettyBearing = "NW";
        }

        return prettyBearing;
    }

    /**
     * Returns an abbreviated cardinal direction as a string.
     * @param radian A value between -180 to 180
     * @return N, NE, E, SE, S, SW, W, NW
     */
    public static String calculateBearingFromRadian(float radian) {

        String prettyBearing = "";

        // Cardinal direction: NORTH and SOUTH
        if ((radian >= -22.5) && (radian <= 22.5)) {
            prettyBearing = "N";
        }
        if ((radian <= -157.5) || (radian >= 157.5)) {
            prettyBearing = "S";
        }

        // Cardinal direction: WESTERLY DIRS
        if (radian < 0) {
            if (radian < -22.5 && radian >= -67.5) {
                prettyBearing = "NW";
            }
            if (radian < -67.5 && radian >= -112.5) {
                prettyBearing = "W";
            }
            if (radian < -112.5 && radian >= -157.5) {
                prettyBearing = "SW";
            }
        }

        // Cardinal direction: EASTERLY DIRS
        if (radian > 0) {
            if (radian > 22.5 && radian <= 67.5) {
                prettyBearing = "NE";
            }
            if (radian > 67.5 && radian <= 112.5) {
                prettyBearing = "E";
            }
            if (radian > 112.5 && radian <= 157.5) {
                prettyBearing = "SE";
            }
        }

        return prettyBearing;
    }

    /**
     * Returns an integer between the values of 0 and 100 which represents a percentage.  Higher is more accurate
     **/
    public static int getCurrentAccAsPct(float accuracy) {
        float a = accuracy;
        if (a > 100f) {
            a = 100f;
        }
        float d = a / 100f; // should rslt in a decimal between 0 and 1.  Higher is worse.
        float pct = 1f - d;
        float rslt = pct * 100;
        return (int) rslt;
    }

    /**
     * Takes the supplied meters value and converts it to either miles or kilometers.
     * If you supply true to the appendToMakePretty parameter it will append the correct
     * measurement unit to the end of the result (e.g. "miles" or "km"
     **/
    public static float convertMetersToMiles(double meters, int decimalCount) {

        if (meters == 0) {
            return 0f;
        }

        double kilometers = meters / 1000d;
        double feet = (meters * 3.280839895d);
        double miles = (feet / 5280d);

        DecimalFormat df = new DecimalFormat("#.#");
        df.setMaximumFractionDigits(decimalCount);
        String result;

        result = df.format((miles));

        return Float.parseFloat(result);
    }

    /**
     * Takes the supplied meters value and converts it to either miles or kilometers.
     * If you supply true to the appendToMakePretty parameter it will append the correct
     * measurement unit to the end of the result (e.g. "miles" or "km"
     **/
    public static float convertMilesToMeters(float miles, int decimalCount) {

        float meters = (miles * 1609.34f);

        DecimalFormat df = new DecimalFormat("#.#");
        df.setMaximumFractionDigits(decimalCount);
        String result = df.format((meters));

        return Float.parseFloat(result);
    }

    /**
     * Takes the supplied meters value and converts it to either miles or kilometers.
     * If you supply true to the appendToMakePretty parameter it will append the correct
     * measurement unit to the end of the result (e.g. "miles" or "km"
     **/
    public static String convertMetersToMiles(double meters, boolean appendToMakePretty) {

        if (meters == 0) {
            return "0";
        }

        double kilometers = meters / 1000d;
        double feet = (meters * 3.280839895d);
        double miles = (feet / 5280d);

        DecimalFormat df = new DecimalFormat("#.#");
        String result;

        result = df.format((miles));
        if (appendToMakePretty) {
            result += " miles";
        }

        return result;
    }

    /**
     * Takes the supplied meters value and converts it to either miles or kilometers.
     * If you supply true to the appendToMakePretty parameter it will append the correct
     * measurement unit to the end of the result (e.g. "miles" or "km"
     **/
    public static String convertMetersToFeet(double meters, Context context, boolean appendToMakePretty) {

        if (meters == 0) {
            return "0";
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String measUnit = prefs.getString("MEASUREUNIT", "IMPERIAL");
        double feet = (meters * 3.280839895);
        DecimalFormat df = new DecimalFormat("#.#");

        String result = "";

        if (measUnit.equals("IMPERIAL")) {
            result = df.format((feet));
            if (appendToMakePretty) {
                result += " feet";
            }
        }
        return result;
    }

    /**
     * Returns a speed in MPH or KPH (depends on user's settings) for the supplied meters per second value
     * <br/><br/>
     * Returns the value as a String in a #.# format.
     * <br/><br/>
     * If the user specifies true for 'appendAppropriateMetric' then either " mph" or " kph" will be appended to the back of the result.
     **/
    public static String getSpeedInMph(float metersPerSecond, Context appContext, boolean appendLetters,
                                       boolean returnLotsOfDecimalPlaces) {

        String rslt;

        try {
            double kmPerHour = ((metersPerSecond * 3600) / 1000);
            double milesPerHour = (metersPerSecond) / (1609.344 / 3600);
            double feetPerSecond = (milesPerHour * 5280) / 3600;

            DecimalFormat df = new DecimalFormat("#.##");

            String decimalMask = "";

            if (returnLotsOfDecimalPlaces) {
                df.setMaximumFractionDigits(8);
            }

            String mph = (df.format(milesPerHour));
            String fps = (df.format(feetPerSecond));
            String kph = (df.format(kmPerHour));
            String mps = (df.format(metersPerSecond));

            // Assign the mph to the value we're going to return
            rslt = mph;

            // If the user wants to append the mph value to the returned string then we oblige here
            if (appendLetters) {
                rslt += " mph";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "0";
        }

        return rslt;

    }

    public static float getSpeedInMph(float metersPerSecond, boolean includeTwoDecimalPlaces) {
        int decimalPlaces = 0;
        if (includeTwoDecimalPlaces) decimalPlaces = 2;
        String spdString = getSpeedInMph(metersPerSecond, false, decimalPlaces);
        return Float.parseFloat(spdString);
    }

    public static float getSpeedInMph(float metersPerSecond, int decimals) {
        String spdString = getSpeedInMph(metersPerSecond, false, decimals);
        return Float.parseFloat(spdString);
    }


    /**
     * Returns a speed in MPH or KPH (depends on user's settings) for the supplied meters per second value
     * <br/><br/>
     * Returns the value as a String in a #.# format.
     * <br/><br/>
     * If the user specifies true for 'appendAppropriateMetric' then either " mph" or " kph" will be appended to the back of the result.
     **/
    public static String getSpeedInMph(float metersPerSecond, boolean appendLetters,
                                       int decimalPlaces) {
        String rslt;

        try {
            double kmPerHour = ((metersPerSecond * 3600) / 1000);
            double milesPerHour = (metersPerSecond) / (1609.344 / 3600);
            double feetPerSecond = (milesPerHour * 5280) / 3600;

            DecimalFormat df = new DecimalFormat("#.##");

            df.setMaximumFractionDigits(decimalPlaces);


            String mph = (df.format(milesPerHour));
            String fps = (df.format(feetPerSecond));
            String kph = (df.format(kmPerHour));
            String mps = (df.format(metersPerSecond));

            // Assign the mph to the value we're going to return
            rslt = mph;

            // If the user wants to append the mph value to the returned string then we oblige here
            if (appendLetters) {
                rslt += " mph";
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return "0";
        }

        return rslt;
    }

    /**
     * Returns the supplied float meters/sec into integer miles/hr
     *
     */
    public static int getSpeedInMph(float metersPerSecond) {
        double dblSpd = (metersPerSecond) / (1609.344 / 3600);
        return (int) dblSpd;
    }

    /**
     * Calculates the distance between two points in miles as a string
     *
     * @param a           Point A (LatLng)
     * @param b           Point B (LatLng)
     * @param appendMiles Whether or not to append, " Miles" to the end of the result
     * @return Distance in miles (as the crow flies)
     */
    public static String getDistanceBetweenInMiles(Location a, Location b, boolean appendMiles) {
        Location loc1 = new Location("");
        loc1.setLatitude(a.getLatitude());
        loc1.setLongitude(a.getLongitude());

        Location loc2 = new Location("");
        loc2.setLatitude(b.getLatitude());
        loc2.setLongitude(b.getLongitude());

        float distanceInMeters = loc1.distanceTo(loc2);

        return convertMetersToMiles(distanceInMeters, appendMiles);
    }

}
