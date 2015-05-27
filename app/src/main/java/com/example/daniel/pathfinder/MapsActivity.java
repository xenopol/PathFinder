package com.example.daniel.pathfinder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends ActionBarActivity {
    private static final int CONTACT_PICKER = 1;
    private static final int CURRENT_LOCATION = 2;


    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private Location myLocation;

    private String contactName;
    private String contactPhoneNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();

        File f = getFilesDir();
        String path = f.getAbsolutePath();
        //Toast.makeText(this, path, Toast.LENGTH_LONG).show();

        //String test = getResources().getString(R.string.test);
        //Toast.makeText(this, test, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
// Enable MyLocation Layer of Google Map
        mMap.setMyLocationEnabled(true);
// Get LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
// Create a criteria object to retrieve provider
        Criteria criteria = new Criteria();
// Get the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);
// Get Current Location
        myLocation = locationManager.getLastKnownLocation(provider);
// set map type
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
// Get latitude of the current location
        double latitude = myLocation.getLatitude();
// Get longitude of the current location
        double longitude = myLocation.getLongitude();
// Create a LatLng object for the current location
        LatLng latLng = new LatLng(latitude, longitude);
// Show the current location in Google Map
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
// Zoom in the Google Map
        mMap.animateCamera(CameraUpdateFactory.zoomTo(16));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.mipmap.ic_launcher);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                    Contacts.CONTENT_URI);
            startActivityForResult(contactPickerIntent, CONTACT_PICKER);
        }

        return super.onOptionsItemSelected(item);
    }

    public String getLocation() {
        return "http://maps.google.com/?q=" + myLocation.getLatitude() + "," + myLocation.getLongitude();
    }

    public void sendMyLocation(View v) {
        String location = getLocation();

        Intent selectContact = new Intent();
        selectContact.setAction(Intent.ACTION_SEND);
        selectContact.putExtra(Intent.EXTRA_TEXT, location);
        selectContact.setType("text/plain");
        startActivityForResult(selectContact, CURRENT_LOCATION);
    }


    public void dangerLocation(View v) throws IOException, JSONException {
        String location = getLocation();

        readFile();

        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(contactPhoneNumber, null, "I am in danger, this is my location: " + location, null, null);
            Toast.makeText(getApplicationContext(), "SMS Sent!",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(),
                    "SMS faild, please try again later!",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }

        String strAddress = geoCoding();

        Intent successActivity = new Intent(MapsActivity.this, SuccessfulDangerLocation.class);
        successActivity.putExtra("StrAddress", strAddress);
        startActivity(successActivity);

    }

    public String geoCoding() throws IOException {
        Geocoder geocoder = new Geocoder(this, Locale.ENGLISH);
        List<Address> addresses = geocoder.getFromLocation(myLocation.getLatitude(), myLocation.getLongitude(), 1);
        StringBuilder strAddress;

        if (addresses != null) {
            Address fetchedAddress = addresses.get(0);
            strAddress = new StringBuilder();

            for (int i = 0; i < fetchedAddress.getMaxAddressLineIndex(); i++) {
                strAddress.append(fetchedAddress.getAddressLine(i)).append("\n");
            }
        } else {
            strAddress = new StringBuilder();
            strAddress.append("No location found!");
        }

        return strAddress.toString();
    }

    public void createFile() throws IOException, JSONException {

        JSONArray data = new JSONArray();
        JSONObject object;

        object = new JSONObject();
        object.put("name", contactName);
        object.put("phoneNumber", contactPhoneNumber);
        data.put(object);

        String text = data.toString();

        FileOutputStream fos = openFileOutput("name.txt", MODE_PRIVATE);
        fos.write(text.getBytes());
        fos.close();
    }

    public void readFile() throws IOException, JSONException {
        FileInputStream fis = openFileInput("name.txt");
        BufferedInputStream bis = new BufferedInputStream(fis);
        StringBuffer b = new StringBuffer();
        while (bis.available() != 0) {
            char c = (char) bis.read();
            b.append(c);
        }
        bis.close();
        fis.close();

        JSONArray data = new JSONArray(b.toString());

        contactName = data.getJSONObject(0).getString("name");
        contactPhoneNumber = data.getJSONObject(0).getString("phoneNumber");

    }

    public void doLaunchContactPicker(View view) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER);
    }

    public void doLaunchContactPicker() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER);
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (CONTACT_PICKER):
                if (resultCode == Activity.RESULT_OK) {

                    Uri contactData = data.getData();
                    Cursor c = managedQuery(contactData, null, null, null, null);
                    if (c.moveToFirst()) {


                        String id = c.getString(c.getColumnIndexOrThrow(Contacts._ID));

                        String hasPhone = c.getString(c.getColumnIndex(Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                    null, null);
                            phones.moveToFirst();
                            String cNumber = phones.getString(phones.getColumnIndex("data1"));
                            contactPhoneNumber = cNumber;
                            //Toast.makeText(this, cNumber, Toast.LENGTH_LONG).show();
                        }
                        String name = c.getString(c.getColumnIndex(Contacts.DISPLAY_NAME));
                        contactName = name;
                        //Toast.makeText(this, name, Toast.LENGTH_LONG).show();
                        try {
                            createFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case (CURRENT_LOCATION):
                // Make sure the request was successful
                if (resultCode == RESULT_OK) {
                    try {
                        String strAddress = geoCoding();

                        Intent successActivity = new Intent(MapsActivity.this, SuccesfulCurrentLocation.class);
                        successActivity.putExtra("StrAddress", strAddress);
                        startActivity(successActivity);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }
}
