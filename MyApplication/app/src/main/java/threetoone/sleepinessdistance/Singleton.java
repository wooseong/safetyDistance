package threetoone.sleepinessdistance;

import android.widget.ImageView;
import android.widget.TextView;

public class Singleton {
    private boolean switchValue;
    private boolean windowManagerCreated = false;
    private int strabimusCount;
    private double acceleration;
    private int velocity;
    private double safetyDistance;
    private double objectDistance;
    private TextView speedTextView,sleepTextView;
    private ImageView mainDistanceImage;
    private ImageView windowDistanceImage;

    public boolean getSwitchValue() {
        return switchValue;
    }

    public boolean getWindowManagerCreated() { return windowManagerCreated; }

    public int getStrabimusCount() {
        return strabimusCount;
    }

    public int getVelocity() { return velocity; }

    public double getAcceleration() { return acceleration; }

    public double getSafetyDistance() { return safetyDistance; }

    public double getObjectDistance() { return objectDistance; }

    public TextView getSpeedTextView() { return speedTextView;}

    public TextView getSleepTextView() { return sleepTextView;}

    public ImageView getMainDistanceImage() { return mainDistanceImage; }

    public ImageView getWindowDistanceImage() { return windowDistanceImage; }

    public void setSwitchValue(boolean switchValue) {
        this.switchValue = switchValue;
    }

    public void setWindowManagerCreated(boolean windowManagerCreated) { this.windowManagerCreated = windowManagerCreated; }

    public void setStrabimusCount(int strabimusCount) {
        this.strabimusCount = strabimusCount;
    }

    public void setVelocity(int velocity) { this.velocity = velocity; }

    public void setAcceleration(double acceleration) { this. acceleration = acceleration; }

    public void setSafetyDistance(double safetyDistance) { this.safetyDistance = safetyDistance; }

    public void setObjectDistance(double objectDistance) { this.objectDistance = objectDistance; }

    public void setSpeedTextView(TextView speedTextView) { this.speedTextView = speedTextView; }

    public void setSleepTextView(TextView sleepTextView) { this.sleepTextView = sleepTextView; }

    public void setMainDistanceImage(ImageView mainDistanceImage) { this.mainDistanceImage = mainDistanceImage; }

    public void setWindowDistanceImage(ImageView windowDistanceImage) { this.windowDistanceImage = windowDistanceImage; }

    private static Singleton instance = null;

    public static synchronized Singleton getInstance() {
        if (instance == null) {
            instance = new Singleton();
        }
        return instance;
    }
}