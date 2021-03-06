package cmput301w18t09.orbid;

import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ToggleButton;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.ExecutionException;

import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 * Uses a google maps API to view some area/location of the planet
 *
 * @author Aidan Kosik, Zach Redfern, Google
 */
public class MapActivity extends Fragment implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleMap mMap;
    private ArrayList<Task> taskList = new ArrayList<>();
    private Location myLocation = null;
    private int isAdd;
    private Task thisTask;
    private LatLng chosenLocation;
    private GoogleApiClient googleApiClient;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_map, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);

        // Get the bundle from the previous activity
        Bundle bundle = getArguments();
        String came_from = bundle.getString("came_from");
        String id = getArguments().getString("_id");
        isAdd = getArguments().getInt("isAdd");
        if (!came_from.equals("recent_listings")) {
            if (isAdd != 1) {
                getThisTask(id);
            }
        }

        // If we came from recent_listings
        if (came_from.equals("recent_listings")) {
            displayAllListings();
        }
        // If we came from a single ad
        else if (came_from.equals("add_task")) {
            changeLocation();
        } else if (came_from.equals("edit_task")) {
            changeLocation();
        } else {
            displaySingleListing(id);
        }
    }


    /**
     * Opens a dialog to confirm if the chosen location is right.
     * If the user chooses yes the location is set as the temporary location
     * and sent back to the addEditActivity with the new location.
     */
    private void changeLocation() {
        Bundle bundle = getArguments();

        if (NavigationActivity.thisLocation != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(NavigationActivity.thisLocation.getLatitude(), NavigationActivity.thisLocation.getLongitude()))
                    .zoom(17)
                    .tilt(30)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }


        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(final LatLng latLng) {
                MarkerOptions marker = new MarkerOptions().position(latLng).title(NavigationActivity.thisUser + "'s Location.");
                mMap.addMarker(marker);
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                        .setTitle("Is this your location?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (isAdd != 1) {
                                    chosenLocation = latLng;
                                    openEditActivty();
                                } else {
                                    chosenLocation = latLng;
                                    openAddActivity();
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mMap.clear();
                                dialog.dismiss();
                            }
                        });
                AlertDialog dialog = builder.create();
                dialog.show();

            }
        });
    }

    /**
     * Creates the intent and puts all the extra information into it before
     * returning to the AddEditActivity to continue creating a task.
     */
    private void openAddActivity() {
        Bundle bundle = getArguments();
        Log.i("MAP", "openAdd");
        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
        intent.putExtra("from_map", "true");
        intent.putExtra("isAdd", 1);
        com.google.maps.model.LatLng newLoc = new com.google.maps.model.LatLng(chosenLocation.latitude, chosenLocation.longitude);
        String stringLocation = null;
        try {
            stringLocation = getAddress(newLoc, getResources());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stringLocation != null) {
            intent.putExtra("location", stringLocation);
        } else {
            Log.e("MAP", "There was no address found to be added to intent");
        }
        if (bundle.getString("title") != null) {
            intent.putExtra("title", bundle.getString("title"));
        }
        if (bundle.getString("description") != null) {
            intent.putExtra("description", bundle.getString("description"));
        }
        if (bundle.getString("price") != null) {
            intent.putExtra("price", bundle.getString("price"));
        }
        getActivity().finish();
        startActivity(intent);
    }


    /**
     * Creates and sets the extra information for the intent before
     * sending it back to the AddEditActivity to continue editing or save
     * the task.
     */
    private void openEditActivty() {
        Intent intent = new Intent(getContext(), AddEditTaskActivity.class);
        Log.i("MAP", "openEdit");
        com.google.maps.model.LatLng newLoc = new com.google.maps.model.LatLng(chosenLocation.latitude, chosenLocation.longitude);
        String stringLocation = null;
        try {
            stringLocation = getAddress(newLoc, getResources());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ApiException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (stringLocation != null) {
            intent.putExtra("location", stringLocation);
        }
        intent.putExtra("isAdd", 0);
        intent.putExtra("from_map", "true");
        intent.putExtra("_id", getArguments().getString("_id"));
        getActivity().finish();
        startActivity(intent);
    }

    /**
     * Displays all the tasks on the map. Used when coming from recent
     * listings list view mode.
     */
    private void displayAllListings() {
        // Todo
        // Get the task using the query

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                Intent intent = new Intent(getContext(), TaskDetailsActivity.class);
                Log.i("TEST OLD TEST", marker.getId());
                intent.putExtra("task_details_layout_id", R.layout.activity_task_details);
                intent.putExtra("_id", marker.getSnippet());
                startActivity(intent);
                return false;
            }
        });

        DataManager.getTasks getTasks = new DataManager.getTasks(getActivity());
        getTasks.execute(new ArrayList<String>());
        try {
            taskList = getTasks.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // Place all of the markers on the map and center on current location
        for (Task task : taskList) {
            if (task.getLocation() != null) {
                Log.i("MAP", "Location " + task.getTitle() + " is " + task.getLocation().toString());
                if (NavigationActivity.thisLocation != null) {
                    if (withinDistance(NavigationActivity.thisLocation.getLatitude(), NavigationActivity.thisLocation.getLongitude(),
                            task.getLocation().lat, task.getLocation().lng)) {
                        LatLng latLng = new LatLng(task.getLocation().lat, task.getLocation().lng);
                        mMap.addMarker(new MarkerOptions().position(latLng).title(task.getTitle()).snippet(task.getID()));
                    }
                }
            }
        }
        if (NavigationActivity.thisLocation != null) {

            Circle circle = mMap.addCircle(new CircleOptions().center(new LatLng(NavigationActivity.thisLocation.getLatitude(), NavigationActivity.thisLocation.getLongitude())).radius(5000).strokeColor(Color.BLUE));
            circle.setVisible(true);

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new LatLng(NavigationActivity.thisLocation.getLatitude(), NavigationActivity.thisLocation.getLongitude()))
                    .zoom(getZoomLevel(circle))
                    .tilt(30)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }


    /**
     * https://stackoverflow.com/questions/12503681/android-mapview-zoom-to-a-radius
     * @param circle
     * @return
     */
    public int getZoomLevel(Circle circle) {
        int zoomLevel = 0;
        if (circle != null){
            double radius = circle.getRadius();
            double scale = radius / 500;
            zoomLevel =(int) (16 - Math.log(scale) / Math.log(2));
        }
        return zoomLevel;
    }

    /**
     * Checks if a task is within 50km of another.
     *
     * @param lat1 - lat of the origin
     * @param lon1 - lon of the origin
     * @param lat2 - lat of task to check if within
     * @param lon2 - lon of task to check if within
     * @return if the task is within the 50km radius of the origin, true within, false outside of
     */
    private boolean withinDistance(double lat1, double lon1, double lat2, double lon2) {
        double d = acos(sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(lon1 - lon2));
        double distance_km = 6371 * d;
        Log.i("Distance", "The distance is: " + d);
        return d <= 5;
    }

    /**
     * Displays the map with just one task. Centers the camera on that task.
     *
     * @param id
     */
    private void displaySingleListing(String id) {
        // Todo
        getThisTask(id);
        LatLng loc = new LatLng(thisTask.getLocation().lat, thisTask.getLocation().lng);
        mMap.addMarker(new MarkerOptions().position(loc).title(thisTask.getTitle()).snippet(thisTask.getID()));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(loc)
                .zoom(17)
                .tilt(30)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /**
     * Finds the location of the user when there is a connection.
     * Sets it to the static variable of the user's location
     * in the navigation activity.
     * @param bundle
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(
                googleApiClient);
        if (location != null) {
            this.myLocation = location;
            Log.i("MAP", "Location is: " + myLocation.toString());
        } else {
            Log.i("MAP", "Location is null:");
        }
    }

    /**
     * When the connetion is suspended we try to reconnect to the client.
     * @param i
     */
    @Override
    public void onConnectionSuspended(int i) {
        googleApiClient.connect();
    }

    /**
     * Report the error when unable to connect to the client.
     * @param connectionResult
     */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e("MAP", "There was an error connecting: " + connectionResult);
    }

    /**
     * Uses Google's API for geocoding to reverse the LatLng location to a formatted address.
     *
     * @param location
     * @return String - the formatted address, or No Address if none was found
     * @throws InterruptedException
     * @throws ApiException
     * @throws IOException
     */
    public static String getAddress(com.google.maps.model.LatLng location, Resources resources) throws InterruptedException, ApiException, IOException {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey(resources.getString(R.string.google_maps_key))
                .build();
        GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, location).await();
        String address = "No Address";
        // Set the address from the results if it did not return null
        if (results.length > 0) {
            if (results[0] != null) {
                address = results[0].formattedAddress;
            }
            return address;
        }
        Log.e("GEO", "There was no location.");
        return "";
    }

    /**
     * Uses Google's API for geocoding to convert a string address to a LatLng location.
     * @param address
     * @param resources
     * @return
     * @throws InterruptedException
     * @throws ApiException
     * @throws IOException
     */
    public static com.google.maps.model.LatLng fromAddress(String address, Resources resources) throws InterruptedException, ApiException, IOException {
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey(resources.getString(R.string.google_maps_key))
                .build();
        GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
        com.google.maps.model.LatLng latLng = new com.google.maps.model.LatLng();
        if (results.length > 0) {
            if (results[0] != null) {
                double lat = results[0].geometry.location.lat;
                double lng = results[0].geometry.location.lng;
                latLng = new com.google.maps.model.LatLng(lat, lng);
            }
        }
        return latLng;
    }


    private void getThisTask(String id) {
        // Get the task using the query
        ArrayList<String> query = new ArrayList<>();
        query.add("_id");
        query.add(id);

        DataManager.getTasks getTasks = new DataManager.getTasks(getActivity());
        getTasks.execute(new ArrayList<String>());
        try {
            taskList = getTasks.get();
            try {
                thisTask = taskList.get(0);
            } catch (Error e) {
                Log.e("GEO", "Task list didn't have anything a index 0.");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}
