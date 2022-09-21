package com.fimbleenterprises.whereyouat.utils;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth.*;

import org.junit.Test;

public class MyGeoUtilTest {

    @Test
    public void calculateBearing() {
    }

    private double radian = -113.2;

    @Test
    public void calculateBearingFromRadian() {

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

        assertThat(prettyBearing).isEqualTo("SW");

    }
}