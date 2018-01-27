package it.unive.dais.cevid.datadroid.lib.parser.progress;

import android.util.Log;

import it.unive.dais.cevid.datadroid.lib.util.UnexpectedException;

/**
 * Created by spano on 30/10/2017.
 */
public class PercentProgressStepper extends ProgressStepper {

    private static final String TAG = "PercentProgressStepper";
    private final int size;
    private final double base, scale;

    public PercentProgressStepper(int size) {
        this(size, 0.0, 1.0);
    }

    public PercentProgressStepper(int size, double base, double scale) {
        this.size = size;
        this.base = base;
        this.scale = scale;
    }

    public PercentProgressStepper getSubProgressStepper(int newSize) {
        return new PercentProgressStepper(newSize, getPercent(), 1.0 / (double) size);
    }

    public double getPercent() {
        double p = (double) cnt / (double) size;
        return base + p * scale;
    }

    @Override
    public int getCurrentProgress() {
        double p = getPercent();
        if (p < 0. || p > 1.)
            throw new UnexpectedException(String.format("ProgressStepper.getPercent() return %f", p));
        int r = (int) (p * 100.);
        Log.d(TAG, String.format("getCurrentProgress(): %d", r));
        return r;
    }

}

