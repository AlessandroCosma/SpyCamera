package com.alessandrocosma.spycamera;

import android.os.Build;
import com.google.android.things.contrib.driver.rainbowhat.RainbowHat;
import com.google.android.things.pio.Gpio;
import com.google.android.things.contrib.driver.ht16k33.AlphanumericDisplay;

import java.io.IOException;

public class BoardDefaults {
    private static final String DEVICE_RPI3 = "rpi3";
    private static final String DEVICE_IMX7D_PICO = "imx7d_pico";
    public static final int HT16K33_BRIGHTNESS_MAX = 0b00001111;
    private static Gpio ledR;
    private static Gpio ledG;
    private static Gpio ledB;

    /**
     * Return the GPIO pin that the Button is connected on.
     */
    public static String getGPIOForButton() throws IOException {
        switch (Build.DEVICE) {
            case DEVICE_RPI3:
                return "BCM21";
            case DEVICE_IMX7D_PICO:
                return "GPIO6_IO14";
            default:
                throw new IllegalStateException("Unknown Build.DEVICE " + Build.DEVICE);
        }
    }

    public static void turnOffLedR() throws IOException{
        // open RED led connection
        ledR = RainbowHat.openLedRed();
        //turn off the light
        ledR.setValue(false);
        //close the device when done
        ledR.close();
    }

    public static void turnOffLedG() throws IOException{

        ledG = RainbowHat.openLedGreen();
        ledG.setValue(false);
        ledG.close();
    }

    public static void turnOffLedB()throws IOException{

        ledB = RainbowHat.openLedBlue();
        ledB.setValue(false);
        ledB.close();

    }


    public static void turnOnLedR() throws IOException{
        // open RED led connection
        ledR = RainbowHat.openLedRed();
        //turn off the light
        ledR.setValue(true);
        //close the device when done
        ledR.close();
    }

    public static void turnOnLedG() throws IOException{

        ledG = RainbowHat.openLedGreen();
        ledG.setValue(true);
        ledG.close();
    }

    public static void turnOnLedB()throws IOException{

        ledB = RainbowHat.openLedBlue();
        ledB.setValue(true);
        ledB.close();

    }

    public static void writeText(String text) throws IOException, InterruptedException{

        // Display a string on the segment display.

        AlphanumericDisplay segment = RainbowHat.openDisplay();
        segment.setBrightness(HT16K33_BRIGHTNESS_MAX);
        segment.setEnabled(true);

        segment.display(text);
        Thread.sleep(800);

        if (text.length() > 4) {
            for (int i = 0; i < text.length() - 4; i++) {
                segment.display(text.substring(i+1));
                Thread.sleep(400);
            }
        }

        Thread.sleep(1100);
        segment.clear();
        segment.setEnabled(false);
        segment.close();
        
   }


}
