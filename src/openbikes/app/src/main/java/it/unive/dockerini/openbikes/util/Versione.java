package it.unive.dockerini.openbikes.util;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

@Entity
public class Versione {
    @PrimaryKey
    public int versione;

    public Versione(int versione){
        this.versione = versione;
    }
}