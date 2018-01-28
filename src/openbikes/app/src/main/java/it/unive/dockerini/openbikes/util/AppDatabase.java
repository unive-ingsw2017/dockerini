package it.unive.dockerini.openbikes.util;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {PuntoInteresse.class, Versione.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract PuntoInteresseDao puntoInteresseDao();
    public abstract VersioneDao versioneDao();
}