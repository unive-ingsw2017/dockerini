package it.unive.dockerini.openbikes.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.StreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MoreInfoActivity extends AppCompatActivity implements OnStreetViewPanoramaReadyCallback {

    protected static final int PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION = 501;
    private FusedLocationProviderClient fusedLocationClient;

    /**
     * Posizione del punto selezionato
     */
    LatLng posizione;

    /**
     * Posizione corrente
     */
    LatLng currentPosition = null;

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        //nascondo l'actionBar
        this.getSupportActionBar().hide();
        setContentView(R.layout.activity_more_info);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //inizializzo le textView da riempire e il bottone portami
        TextView nome, descrizione, dataAggiunta, indirizzo;
        nome = (TextView) findViewById(R.id.nome);
        descrizione = (TextView) findViewById(R.id.descrizione);
        dataAggiunta = (TextView) findViewById(R.id.dataAggiunta);
        indirizzo = (TextView) findViewById(R.id.indirizzo);
        Button portami = (Button) findViewById(R.id.button_naviga);

        //inizializzo il fragment per la street view
        StreetViewPanoramaFragment streetViewPanoramaFragment = (StreetViewPanoramaFragment) getFragmentManager().findFragmentById(R.id.streetviewpanorama);
        //richiedo il panorama street view
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(this);
        //aggiorno la currentPosition
        updateCurrentPosition();

        //prendo le info passate all'activity
        Bundle b = getIntent().getExtras();
        posizione = b.getParcelable("posizione");

        //se nome == null il dispositivo è in landscape, quindi non serve scrivere le info del punto
        if (nome != null) {
            nome.setText(getString(R.string.place_name) + b.getString("nome"));
            descrizione.setText(getString(R.string.place_category) + b.getString("descrizione"));
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            try {
                dataAggiunta.setText(getString(R.string.place_date) + new SimpleDateFormat("dd/MM/yyyy, HH:mm:ss").format(df.parse(b.getString("dataAggiunta"))));
            } catch (ParseException e) {
                dataAggiunta.setText(getString(R.string.place_date) + "Data non disponibile");
            }
            try {
                List<Address> addresses = geocoder.getFromLocation(posizione.latitude, posizione.longitude, 1);
                String ris = addresses.get(0).getCountryName();
                ris += addresses.get(0).getAdminArea() == null ? "" : ", " + addresses.get(0).getAdminArea();
                ris += addresses.get(0).getSubAdminArea() == null ? "" : ", " + addresses.get(0).getSubAdminArea();
                ris += addresses.get(0).getLocality() == null ? "" : ", " + addresses.get(0).getLocality();
                ris += addresses.get(0).getThoroughfare() == null ? "" : ", " + addresses.get(0).getThoroughfare();
                indirizzo.setText(getString(R.string.place_address) + ris);
            } catch (IOException e) {
                indirizzo.setText(getString(R.string.place_address) + "Indirizzo non disponibile");
            }

            portami.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (currentPosition != null) {
                        naviga("http://maps.google.com/maps?saddr=" + currentPosition.latitude + "," + currentPosition.longitude + "&daddr=" + posizione.latitude + "," + posizione.longitude);
                    } else {
                        //chiedo se vuole avviare il navigatore anche senza essere localizzato
                        AlertDialog.Builder adb = new AlertDialog.Builder(MoreInfoActivity.this);
                        adb.setTitle("Posizione sconosciuta");
                        adb.setMessage("OpenBikes non riesce ad effetuare la localizzazione, controlla le impostazioni del GPS. \nVuoi avviare la navigazione comunque?");
                        adb.setIcon(android.R.drawable.ic_dialog_alert);
                        adb.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                naviga("http://maps.google.com/maps?daddr=" + posizione.latitude + "," + posizione.longitude);
                            }
                        });
                        adb.setNegativeButton("NO", null);
                        adb.show();
                    }
                }
            });
        }

    }

    protected void naviga(String link) {
        //fa partire l'intent per google maps
        //se Google Maps è installata la fa partire, altrimenti utilizza il browser
        Intent navigation = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        startActivity(navigation);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
        //setto il panorama della street view
        panorama.setPosition(posizione);
        panorama.setPanningGesturesEnabled(true);
        panorama.setUserNavigationEnabled(true);
        panorama.setZoomGesturesEnabled(true);
        panorama.setStreetNamesEnabled(true);
    }

    public void updateCurrentPosition() {
        if (ActivityCompat.checkSelfPermission(MoreInfoActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MoreInfoActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MoreInfoActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_BOTH_LOCATION);
        } else {
            fusedLocationClient.getLastLocation().addOnSuccessListener(MoreInfoActivity.this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location loc) {
                    if (loc != null) {
                        currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    }
                }
            });
        }
    }

}
