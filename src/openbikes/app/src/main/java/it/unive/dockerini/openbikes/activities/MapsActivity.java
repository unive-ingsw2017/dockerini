package it.unive.dockerini.openbikes.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.maps.android.clustering.ClusterManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import it.unive.dais.cevid.datadroid.lib.parser.CsvRowParser;
import it.unive.dais.cevid.datadroid.lib.parser.RecoverableParseException;
import it.unive.dockerini.openbikes.util.MapItemCluster;

public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener,
        GoogleMap.OnCameraMoveStartedListener, GoogleMap.OnMarkerClickListener,
        ClusterManager.OnClusterItemInfoWindowClickListener<MapItemCluster> {

    protected static final int REQUEST_CHECK_SETTINGS = 500;
    protected static final int PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION = 501;
    private static final String TAG = "MapsActivity";
    protected GoogleMap gMap;

    /**
     * pulsanti sovrapposti alla mappa
     */
    protected ImageButton button_here, button_search, button_filters;

    /**
     * API per i servizi di localizzazione.
     */
    protected FusedLocationProviderClient fusedLocationClient;

    /**
     * Posizione corrente. Potrebbe essere null prima di essere calcolata la prima volta.
     */
    @Nullable
    protected LatLng currentPosition = null;

    /**
     * Gestore dei cluster
     */
    private ClusterManager<MapItemCluster> mClusterManager;

    /**
     * Lista delle categorie  a cui possono appartenere i punti
     */
    ArrayList<String> categorie = new ArrayList<>();

    /**
     * Lista di tutti i MapItemCluster da raggruppare in cluster
     */
    ArrayList<MapItemCluster> clusterItems = new ArrayList<>();

    /**
     * Lista dei filtri attivi
     */
    ArrayList<String> filters = new ArrayList<>();

    /**
     * Layout che contiene le checkbox dei filtri
     */
    LinearLayout layoutFiltri;

    /**
     * Layout che contiene i i filtri e i bottoni annulla e applica. Serve per nascondere/mostrare il form di filtraggio
     */
    LinearLayout contenitoreLayoutFiltri;

    /**
     * Ultimo MapItemCluster (corrisponde al marker) cliccato
     */
    MapItemCluster clickedClusterItem;

    /**
     * Oggetto usato er settare i limiti e lo zoom della mappa tenendo conto di tutti i punti inseriti
     */
    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

    /**
     * Dialog di avanzamento mostrato mentre i punti vengono analizzati e disegnati sulla mappa
     */
    ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        // inizializza le preferenze
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // API per i servizi di localizzazione
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        //per evitare di ricaricare la mappa ogni volta che si rientra nell'activity (es. quando si gira il dispositivo)
        if (savedInstanceState == null) {
            mapFragment.setRetainInstance(true);
        }
        mapFragment.getMapAsync(this);

        progress = new ProgressDialog(MapsActivity.this);

        button_here = (ImageButton) findViewById(R.id.button_here);
        button_search = (ImageButton) findViewById(R.id.button1);
        button_filters = (ImageButton) findViewById(R.id.button_filters);
        layoutFiltri = (LinearLayout) findViewById(R.id.layoutFiltri);
        contenitoreLayoutFiltri = (LinearLayout) findViewById(R.id.contenitoreFiltri);
        Button annullaFiltri = (Button) findViewById(R.id.annullaFiltri);
        Button confermaFiltri = (Button) findViewById(R.id.confermaFiltri);
        EditText searchview = (EditText) findViewById(R.id.searchView1);

        //quando si preme invio dopo aver scritto una località da cercare bisogna avviare l'onClick() del button_search
        searchview.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_SEARCH || (id == EditorInfo.IME_ACTION_UNSPECIFIED && keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    button_search.callOnClick();
                    return true;
                }
                return false;
            }
        });

        //ricerca della località inserita
        button_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String localita = searchview.getText().toString();
                //Geocoder di google
                Geocoder geocoder = new Geocoder(getBaseContext());
                List<Address> addresses = null;

                //nascondo la tastiera software
                View view = MapsActivity.this.getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                try {
                    //cerca un indirizzo che corrisponde alla stringa cercata
                    addresses = geocoder.getFromLocationName(localita, 1);
                    if (addresses != null && !addresses.equals("")) {
                        Address address = addresses.get(0);
                        Double home_long = address.getLongitude();
                        Double home_lat = address.getLatitude();
                        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
                        //sposto la camera fino al punto ricercato
                        gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                        gMap.animateCamera(CameraUpdateFactory.zoomTo(15));
                    }
                } catch (IOException e) {
                    //IOException del geocoder
                    Toast.makeText(MapsActivity.this, getString(R.string.geocode_error), Toast.LENGTH_SHORT).show();
                }
            }
        });

        button_filters.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contenitoreLayoutFiltri.setVisibility(LinearLayout.VISIBLE);
            }
        });

        annullaFiltri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //riporto i filtri allo stato in cui si trovavano prima che venissero mostrati
                for (int i = 0; i < layoutFiltri.getChildCount(); i++) {
                    if (layoutFiltri.getChildAt(i) instanceof CheckBox) {
                        CheckBox temp = (CheckBox) layoutFiltri.getChildAt(i);
                        temp.setChecked(filters.contains(temp.getText().toString()));
                    }
                }
                contenitoreLayoutFiltri.setVisibility(LinearLayout.INVISIBLE);
            }
        });

        confermaFiltri.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //modifico la lista filters in modo che contenga solo i filtri selezionati
                for (int i = 0; i < layoutFiltri.getChildCount(); i++) {
                    if (layoutFiltri.getChildAt(i) instanceof CheckBox) {
                        CheckBox temp = (CheckBox) layoutFiltri.getChildAt(i);
                        if (temp.isChecked() && !filters.contains(temp.getText().toString())) {
                            filters.add(temp.getText().toString());
                        } else if (!temp.isChecked()) {
                            filters.remove(temp.getText().toString());
                        }
                    }
                }

                //scorro la lista di MapItemCluster e inserisco in itemDaMostrare solo quelli che rispettano i filtri
                ArrayList<MapItemCluster> itemDaMostrare = new ArrayList<>();
                for (MapItemCluster m : clusterItems) {
                    if (m.respectsFilters(filters)) {
                        itemDaMostrare.add(m);
                    }
                }
                //tolgo tutti i punti dalla mappa
                mClusterManager.clearItems();
                //aggiungo i punti che rispettano i filtri
                mClusterManager.addItems(itemDaMostrare);
                //aggiorno la mappa
                mClusterManager.cluster();
                contenitoreLayoutFiltri.setVisibility(LinearLayout.INVISIBLE);
            }
        });
        button_here.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateCurrentPosition();
                if (currentPosition != null) {
                    Log.d(TAG, "click qui");
                    if (gMap != null)
                        gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition, getResources().getInteger(R.integer.zoomFactor_button_here)));
                }
            }
        });

        //prendo subito la posizione dell'utente
        updateCurrentPosition();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        //Evito che l'activity si ricarichi quando il dispositivo viene girato
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyMapSettings();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        gMap.clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION: {
                if (grantResults.length > 0) {
                    if (!(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        //Non sono stati garantiti i permessi necessari
                        AlertDialog.Builder adb = new AlertDialog.Builder(MapsActivity.this);
                        adb.setTitle(getString(R.string.request_permission_failed_title));
                        adb.setMessage(getString(R.string.request_permission_failed_message));
                        adb.setIcon(android.R.drawable.ic_dialog_alert);
                        adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                //chiudo l'app
                                MapsActivity.this.finish();
                                System.exit(0);
                            }
                        });
                        adb.show();
                    }
                } else {
                    //è stato aperto un secondo dialog, quindi il primo è stato chiuso senza ricevere risposta
                    //non faccio nulla
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.maps_with_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.MENU_SETTINGS:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.MENU_INFO:
                startActivity(new Intent(this, InfoActivity.class));
                break;
        }
        return false;
    }

    public void updateCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //richiede i permessi se non sono stati garantiti
            ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION);
        } else {
            //aggiorna currentPosition
            fusedLocationClient.getLastLocation().addOnSuccessListener(MapsActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location loc) {
                    if (loc != null) {
                        currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    } else {
                        Toast.makeText(MapsActivity.this, getString(R.string.localization_error), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gMap == null) {
            gMap = googleMap;
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION);
            } else {
                gMap.setMyLocationEnabled(true);
            }
            gMap.setOnMapClickListener(this);
            gMap.setOnMapLongClickListener(this);
            gMap.setOnCameraMoveStartedListener(this);
            gMap.setOnMarkerClickListener(this);

            UiSettings uis = gMap.getUiSettings();
            uis.setZoomGesturesEnabled(true);
            uis.setMyLocationButtonEnabled(false);
            uis.setCompassEnabled(true);
            uis.setZoomControlsEnabled(true);
            uis.setMapToolbarEnabled(false);

            applyMapSettings();

            init();
        }
    }

    protected void applyMapSettings() {
        if (gMap != null) {
            gMap.setMapType(SettingsActivity.getMapStyle(this));
        }
    }

    @Override
    public boolean onMarkerClick(final Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    private void init() {
        //configuro e mostro il progressDialog
        progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progress.setTitle(getString(R.string.progressDialog_title));
        progress.setMessage(getString(R.string.progressDialog_message));
        progress.setCancelable(false);
        progress.setIndeterminate(false);
        progress.show();

        /**
         * Effettuo il parsing del csv e la creazione della lista clusterItems usando un task asincrono,
         * altrimenti il thread principale non gestirebbe la grafica e l'applicazione sembrerebbe bloccata
         */
        RiempiMappaTask task = new RiempiMappaTask();
        task.execute();
    }

    protected class RiempiMappaTask extends AsyncTask<Void, Integer, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                InputStream is = getResources().openRawResource(R.raw.biciclette);
                CsvRowParser p = new CsvRowParser(new InputStreamReader(is), true, ";", null);
                List<CsvRowParser.Row> rows = p.getAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();
                int i = 0;
                //dichiaro il numero totale di punti
                publishProgress(null, rows.size());

                //Per ogni riga parsata aggiungo un MapItemCluster alla lista clusterItems
                //e aggiungo latitudine e longitudine del punto al boundsBuilder
                for (final CsvRowParser.Row r : rows) {
                    try {
                        if (!r.get("Tipo").equals("") && !categorie.contains(r.get("Tipo"))) {
                            categorie.add(r.get("Tipo"));
                        }
                        MapItemCluster itemCluster = new MapItemCluster(r.get("Latitudine"), r.get("Longitudine"), r.get("Nome").equals("") ? "Nome non specificato" : r.get("Nome"), r.get("Tipo"), r.get("Data e ora inserimento"));
                        clusterItems.add(itemCluster);
                        boundsBuilder.include(itemCluster.getPosition());
                    } catch (RecoverableParseException e) {
                        Log.e(TAG, e.getMessage());
                    }
                    i++;
                    //dico al thread principale a che punto sono arrivato
                    publishProgress(i);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //codice eseguito sul thread principale
            if (values[0] == null) {
                //setto il massimosul progressDialog
                progress.setMax(values[1]);
            } else
                //setto il progresso
                progress.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //codice eseguito sul thread principale
            super.onPostExecute(aVoid);
            //inizializzo il clusterManager
            mClusterManager = new ClusterManager<MapItemCluster>(MapsActivity.this, gMap);

            //setto il clusterManager sui listener della mappa
            gMap.setOnCameraIdleListener(mClusterManager);
            gMap.setOnMarkerClickListener(mClusterManager);

            //setto l'infoWindow adapter
            gMap.setInfoWindowAdapter(mClusterManager.getMarkerManager());

            mClusterManager.setOnClusterItemInfoWindowClickListener(MapsActivity.this);

            //codice eseguito quando viene cliccato un MapItemCluster, cioè un marker
            mClusterManager.setOnClusterItemClickListener(new ClusterManager.OnClusterItemClickListener<MapItemCluster>() {
                @Override
                public boolean onClusterItemClick(MapItemCluster item) {
                    clickedClusterItem = item;
                    return false;
                }
            });

            //aggiungo la lista di MapItemCluster al clusterManager
            mClusterManager.addItems(clusterItems);
            mClusterManager.getMarkerCollection().setOnInfoWindowAdapter(new CustomAdapterForItems());
            gMap.setOnInfoWindowClickListener(mClusterManager);

            //per ogni categoria creo una checkbox
            for (String c : categorie) {
                CheckBox cb = new CheckBox(MapsActivity.this);
                cb.setText(c);
                cb.setChecked(true);
                layoutFiltri.addView(cb);
            }

            //inizialmente tutti i filtri sono selezionati
            filters = new ArrayList<String>(categorie);

            //chiudo il progressDialog
            progress.dismiss();
            //aggiorno la mappa
            mClusterManager.cluster();
            //uso il boundsBuilder per muovere la camera in modo da comprendere tutti i marker e settare lo zoom più adatto
            gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100));
        }
    }

    /**
     * Codice eseguito quando l'utente clicca su un infoWindow di un MapItemCluster
     */
    @Override
    public void onClusterItemInfoWindowClick(MapItemCluster myItem) {
        //creo un bundle con le info relative al punto selezionato
        Intent i = new Intent(MapsActivity.this, MoreInfoActivity.class);
        Bundle b = new Bundle();
        b.putParcelable("posizione", myItem.getPosition());
        b.putString("nome", myItem.getTitle());
        b.putString("descrizione", myItem.getSnippet());
        b.putString("dataAggiunta", myItem.getDataAggiunta());
        i.putExtras(b);
        //faccio partire l'activity MoreInfoActivity
        startActivity(i);
    }

    /**
     * Classe per restituire un'infoWindow custom
     */
    protected class CustomAdapterForItems implements GoogleMap.InfoWindowAdapter {

        private final View customInfoWindow;

        CustomAdapterForItems() {
            //imposto il layuot definito nel file custom_info_window.xml
            customInfoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            //riempio l'infoWindow con le info necessarie
            TextView title = (TextView) customInfoWindow.findViewById(R.id.info_window_title);
            TextView descritpion = (TextView) customInfoWindow.findViewById(R.id.info_window_description);

            title.setText(clickedClusterItem.getTitle());
            descritpion.setText(clickedClusterItem.getSnippet());
            return customInfoWindow;
        }
    }

    //metodi vuoti

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onCameraMoveStarted(int i) {

    }

    @Override
    public void onMapClick(LatLng latLng) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }
}


