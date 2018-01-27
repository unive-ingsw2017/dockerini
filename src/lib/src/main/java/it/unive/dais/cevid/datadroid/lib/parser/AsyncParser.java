package it.unive.dais.cevid.datadroid.lib.parser;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.util.List;

import it.unive.dais.cevid.datadroid.lib.parser.progress.ProgressStepper;

/**
 * Interfaccia che rappresenta un parser asincrono.
 */
public interface AsyncParser<Data, P extends ProgressStepper> extends Parser<Data> {
    @NonNull AsyncTask<Void, P, List<Data>> getAsyncTask();
    void onPreExecute();
    void onProgressUpdate(@NonNull P p);
    void onPostExecute(@NonNull List<Data> r);
//    void publishProgress(P p);
}
