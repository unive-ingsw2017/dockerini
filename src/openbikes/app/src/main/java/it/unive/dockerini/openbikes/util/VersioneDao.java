package it.unive.dockerini.openbikes.util;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface VersioneDao {
    @Query("SELECT * FROM versione")
    List<Versione> getAll();

    @Insert
    void insert(Versione versione);

    @Query("DELETE FROM versione")
    void deleteAll();
}