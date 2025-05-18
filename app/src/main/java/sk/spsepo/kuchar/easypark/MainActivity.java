package sk.spsepo.kuchar.easypark;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.app.AlertDialog;
import android.widget.Toast;

import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonLayer;

import org.json.JSONException;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng presov = new LatLng(49.0038, 21.2396);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(presov, 12));

        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        try {
            GeoJsonLayer layer = new GeoJsonLayer(mMap, R.raw.data, getApplicationContext());

            layer.setOnFeatureClickListener(new GeoJsonLayer.OnFeatureClickListener() {
                @Override
                public void onFeatureClick(Feature feature) {
                    StringBuilder attributes = new StringBuilder();
                    for (String key : feature.getPropertyKeys()) {
                        attributes.append(key).append(": ").append(feature.getProperty(key)).append("\n");
                    }

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Atribúty")
                            .setMessage(attributes.toString())
                            .setPositiveButton("OK", null)
                            .show();
                }
            });

            layer.addLayerToMap();

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "Chyba pri načítaní GeoJSON vrstvy", Toast.LENGTH_LONG).show();
        }
    }
}
