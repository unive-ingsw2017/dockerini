package it.unive.dais.cevid.datadroid.lib.parser.progress;

import android.support.annotation.NonNull;

import it.unive.dais.cevid.datadroid.lib.util.Function;

/**
 * Created by spano on 19/12/2017.
 */
public interface Handle<T> extends AutoCloseable {

    default void release() {
        try {
            close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    <R> R apply(@NonNull Function<T, R> f);

}
