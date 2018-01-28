package it.unive.dockerini.openbikes.util;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface PuntoInteresseDao {
    @Query("SELECT * FROM puntointeresse")
    List<PuntoInteresse> getAll();

    @Insert
    void insert(PuntoInteresse puntoInteresse);

    @Query("DELETE FROM puntointeresse")
    void deleteAll();
}