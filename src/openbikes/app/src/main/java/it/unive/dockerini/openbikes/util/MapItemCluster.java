package it.unive.dockerini.openbikes.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;
import java.util.List;

public class MapItemCluster implements ClusterItem {
    final LatLng mPosition;
    final String mTitle;
    final String mSnippet;
    final String dataAggiunta;

    public MapItemCluster(String lat, String lng, String title, String snippet, String dataOraAggiunta) {
        mPosition = new LatLng(Double.parseDouble(lat), Double.parseDouble(lng));
        mTitle = title;
        mSnippet = snippet;
        dataAggiunta = dataOraAggiunta;
    }

    @Override
    public LatLng getPosition() {
        return mPosition;
    }

    @Override
    public String getTitle() {
        return mTitle;
    }

    @Override
    public String getSnippet() {
        return mSnippet;
    }

    public boolean respectsFilters(List<String> filters) {
        return filters.contains(mSnippet);
    }

    public String getDataAggiunta(){
        return dataAggiunta;
    }

}
