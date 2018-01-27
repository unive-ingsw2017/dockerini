package it.unive.dais.cevid.datadroid.lib.parser;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ProgressBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import it.unive.dais.cevid.datadroid.lib.parser.progress.Handle;
import it.unive.dais.cevid.datadroid.lib.parser.progress.ProgressBarManager;
import it.unive.dais.cevid.datadroid.lib.parser.progress.ProgressStepper;

/**
 * Clase astratta parametrica che rappresenta un parser di dati in senso generale, sottoclasse di AsyncTask.
 * L'utente deve ereditare questa classe ed implementarne i metodi mancanti oppure utilizzare direttamente alcune sottoclassi non astratte
 * già contenute in questa libreria.
 *
 * @param <Data> il tipo di una riga di dati (non dell'intera collezione dei dati).
 *               Da usare per rappresentare il progresso del parsing, come una progress bar.
 *               Per ignorarlo passare il tipo Void come parametro Progress a questa classe.
 * @author Alvise Spanò, Università Ca' Foscari
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAsyncParser<Data, P extends ProgressStepper> implements AsyncParser<Data, P> {

    private final MyAsyncTask asyncTask = new MyAsyncTask();

    @Nullable
    private final ProgressBarManager pbm;
    @Nullable
    private Handle<ProgressBar> handle = null;

    protected AbstractAsyncParser(@Nullable ProgressBarManager pbm) {
        this.pbm = pbm;
    }

    /**
     * Converte una URL in un {@code InputStreamReader}.
     * Questo metodo statico è utile per implementare, nelle sottoclassi di questa classe, un costruttore aggiuntivo un parametro di
     * tipo URL come, che può essere convertito in un {@code InputStreamReader} tramite questo metodo statico e passato rapidamente
     * al costruttore principale, come per esempio:
     * <blockquote><pre>
     * {@code
     * public static class MyDataParser extends AbstractAsyncParser<MapItem, Void, InputStreamReader> {
     *      protected MyDataParser(InputStreamReader rd) {
     *          super(rd);
     *      }
     * <p>
     *      protected MyDataParser(URL url) throws IOException {
     *          super(urlToReader(url));
     *      }
     * <p>
     *      protected List<MapItem> parse(InputStreamReader rd) throws IOException {
     *          // fai qualcosa usando rd
     *      }
     * }
     * }
     * </pre></blockquote>
     *
     * @param url parametro di tipo URL.
     * @return risultato di tipo InputStreamReader.
     * @throws IOException lancia questa eccezione quando sorgono problemi di I/O.
     */
    @NonNull
    protected static InputStreamReader urlToReader(@NonNull URL url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        InputStream stream = connection.getInputStream();
        return new InputStreamReader(stream);
    }

    /**
     * Metodo di cui è necessario fare override nelle sottoclassi.
     * Deve occuparsi del parsing vero e proprio.
     * //     * @param input parametro di tipo Input.
     *
     * @return ritorna una lista di oggetti di tipo FiltrableData.
     * @throws IOException lanciata se il parser incontra problemi.
     */
    @Override
    @NonNull
    public abstract List<Data> parse() throws IOException;

    @Override
    @NonNull
    public String getName() {
        return getClass().getName();
    }

    /**
     * Restituisce l'oggetto interno di tipo AsyncTask.
     *
     * @return oggetto di tipo AsyncTask.
     */
    @NonNull
    @Override
    public AsyncTask<Void, P, List<Data>> getAsyncTask() {
        return asyncTask;
    }

    @SuppressLint("StaticFieldLeak")
    protected class MyAsyncTask extends AsyncTask<Void, P, List<Data>> {
        private final AbstractAsyncParser<Data, P> enclosing = AbstractAsyncParser.this;

//        private MyAsyncTask(@NonNull AbstractAsyncParser<Data, P> enclosing) {
//            this.enclosing = enclosing;
//        }

        /**
         * Metodo interno che invoca {@code parse} all'interno di un blocco try..catch.
         * Non è necessario fare override a meno che non si desideri specificare un comportamento diverso.
         * Il metodo da definire nelle sottoclassi è {@code parse}.
         *
         * @param params nessun parametro.
         * @return la lista di dati prodotti da {@code parse}.
         */
        @Override
        @Nullable
        protected List<Data> doInBackground(Void... params) {
            final String name = enclosing.getName(), tag = enclosing.getName();
            try {
                Log.v(tag, String.format("started async parser %s", name));
                List<Data> r = enclosing.parse();
                Log.v(tag, String.format("async parser %s finished (%d elements)", name, r.size()));
                return enclosing.onPostParse(r);
            } catch (IOException e) {
                Log.e(tag, String.format("exception caught during parser %s: %s", name, e));
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected final void onPreExecute() {
            if (pbm != null) {
                handle = pbm.acquire();
                handle.apply(pb -> {
                    pb.setMax(100);
                    return null;
                });
            }
            enclosing.onPreExecute();
        }

        @Override
        protected final void onProgressUpdate(@NonNull P... ps) {
            final P p = ps[0];
            if (handle != null) {
                handle.apply(pb -> {
                    pb.setProgress(p.getCurrentProgress());
                    return null;
                });
            }
            enclosing.onProgressUpdate(p);
        }

        @Override
        protected final void onPostExecute(@NonNull List<Data> r) {
            enclosing.onPostExecute(r);
            if (handle != null) {
                handle.release();
                handle = null;
            }
        }

        /**
         * Questo metodo è solamente uno stub di {@code publishProgress}.
         * E' necessario perché {@code publishProgress} ha visibilità {@code protected} e quindi non può essere chiamato
         * dalle sottoclassi della enclosing class {@code AbstractAsyncParser}-.
         *
         * @param p varargs di tipo Progress
         */
        private void _publishProgress(@NonNull P... p) {
            this.publishProgress(p);
        }
    }

    public final void publishProgress(P p) {
        asyncTask._publishProgress(p);
    }

    @Override
    public void onPreExecute() {
    }

    @Override
    public void onProgressUpdate(@NonNull P p) {
    }

    @Override
    public void onPostExecute(@NonNull List<Data> r) {
    }

    @NonNull
    public List<Data> onPostParse(@NonNull List<Data> r) {
        return r;
    }


}
