package it.unive.dockerini.openbikes.util;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

@Entity
public class PuntoInteresse {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String latitudine;
    public String longitudine;
    public String mTitle;
    public String mSnippet;
    public String dataAggiunta;

    public PuntoInteresse(String latitudine, String longitudine, String mTitle, String mSnippet, String dataAggiunta){
        this.latitudine = latitudine;
        this.longitudine = longitudine;
        this.mTitle = mTitle;
        this.mSnippet = mSnippet;
        this.dataAggiunta = dataAggiunta;
    }
}