package it.unive.dais.cevid.datadroid.lib.parser.progress;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;

import it.unive.dais.cevid.datadroid.lib.util.Function;

/**
 * Created by spano on 05/01/2018.
 */
public class ProgressBarManager {

    protected static class Descr {
        private static int cnt = 1;
        @NonNull
        public final ProgressBar progressBar;
        private int owner = 0;

        public Descr(@NonNull ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        public String toString() {
            return String.format("[bar:%s owner:%d]", System.identityHashCode(progressBar), getOwner());
        }

        public boolean isOwner(int owner) {
            return getOwner() == owner;
        }

        public boolean hasOwner() {
            return getOwner() > 0;
        }

        public int getOwner() {
            return owner;
        }

        public void setOwner(int owner) {
            this.owner = owner;
        }

        public void cleanOwner() {
            this.owner = 0;
        }

        public static int newOwner() {
            return cnt++;
        }
    }

    private static final String TAG = "ProgressBarManager";
    @NonNull
    protected final ConcurrentLinkedQueue<Descr> q;
    @NonNull
    protected final Activity ctx;

    public ProgressBarManager(@NonNull Activity ctx, @NonNull Iterable<ProgressBar> progressBars) {
        this.ctx = ctx;
        this.q = new ConcurrentLinkedQueue<>();
        for (ProgressBar p : progressBars) {
            this.q.add(new Descr(p));
            ctx.runOnUiThread(() -> p.setVisibility(View.GONE));
        }
    }

    public ProgressBarManager(@NonNull Activity ctx, @NonNull ProgressBar progressBar) {
        this(ctx, new ProgressBar[]{progressBar});
    }


    public ProgressBarManager(Activity ctx, ProgressBar[] progressBars) {
        this(ctx, Arrays.asList(progressBars));
    }

    @NonNull
    public Handle<ProgressBar> acquire() {
        final int owner = Descr.newOwner();
        findOwned(owner);   // just allocate a descr asap
        return new Handle<ProgressBar>() {
            @Override
            public void close() {
                synchronized (q) {
                    for (Descr d : q) {
                        if (d.isOwner(owner)) {
                            Log.d(TAG, String.format("releasing descr %s", d));
                            d.cleanOwner();
                            onLastRelease(d.progressBar);
                        }
                    }
                }
            }

            @Override
            @Nullable
            public <R> R apply(@NonNull Function<ProgressBar, R> f) {
                synchronized (q) {
                    Descr d = findOwned(owner);
                    return d == null ? null : f.apply(d.progressBar);
                }
            }
        };

    }

    @Nullable
    private Descr findOwned(int owner) {
        synchronized (q) {
            for (Descr d : q) {
                if (d.isOwner(owner)) {
                    Log.d(TAG, String.format("found descr %s already owned by %d", d, owner));
                    return d;
                }
            }
            for (Descr d : q) {
                if (!d.hasOwner()) {
                    d.setOwner(owner);
                    onFirstAcquire(d.progressBar);
                    Log.d(TAG, String.format("allocated descr %s", d));
                    return d;
                }
            }
            Log.d(TAG, String.format("cannot allocate bar now (owner %d is waiting)", owner));
            return null;
        }
    }

    public void release(@NonNull Handle<ProgressBar> h) {
        h.release();
    }

    protected void onLastRelease(@NonNull ProgressBar progressBar) {
        Log.d(TAG, "parser progressbar: GONE");
        ctx.runOnUiThread(() -> progressBar.setVisibility(View.GONE));
    }

    protected void onFirstAcquire(@NonNull ProgressBar progressBar) {
        Log.d(TAG, "parser progressbar: VISIBLE");
        ctx.runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
    }

}
