package com.knewto.www.sunshowers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private final String LOG_TAG = MainActivityFragment.class.getSimpleName();
    ArrayList<String> forecastList = new ArrayList<String>(); // list of forecast data
    ArrayAdapter<String> ForecastAdapter; //array adapter for forecast list

    public MainActivityFragment() {
    }
    // onstart method
    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    // method to update weather from server
    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        String units = prefs.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_default));
        weatherTask.execute(location, units);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Boolean to inflate fragment options menu
        setHasOptionsMenu(true);

        // Create Array Adapter and associate with data and item template
        ForecastAdapter = new ArrayAdapter<String> (
                getActivity(),  // Context
                R.layout.list_item_forecast, // Name of item layout file
                R.id.forecast_item, // ID of the text view to populate
                forecastList //Forecast Data
        ) ;


        View MainActivityFragmentView = inflater.inflate(R.layout.fragment_main, container, false);

        // Bind view to adapter
        ListView listview = (ListView) MainActivityFragmentView.findViewById(R.id.forecast_list);
        listview.setAdapter(ForecastAdapter);

        // Set up button listener
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                // Add current item text to intent
                String itemText = ForecastAdapter.getItem(position);
                detailIntent.putExtra(Intent.EXTRA_TEXT, itemText);
                // Start activity
                getActivity().startActivity(detailIntent);
            }
        });

        return MainActivityFragmentView;
    }

    // Adds refresh to menu items for testing.
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_fragment, menu);
    }

    // Handler for refresh menu item being clicked.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                // Display toast if refresh is selected
                Context context = getActivity(); // the current context
                int duration = Toast.LENGTH_SHORT; // how long the toast should display
                String toastText = "Ahhh refreshing!"; // what the toast should display
                Toast toast = Toast.makeText(context, toastText, duration);  // create the toast
                toast.show(); // display the toast
                updateWeather();
                return true;
            case R.id.action_map:
                // get the location from preferences
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
                // Create intent, set data, and start if there is an application that can handle
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("geo:0,0?q=" + location));
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(intent);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A method to load dummy data used into the array list used by the
     * array adapter to display a list of forecasts.
     */
    public void loadData(){
        forecastList = new ArrayList<String>();
        forecastList.add("Today - Sunny - 88/63");
        forecastList.add("Tomorrow - Foggy - 70/46");
        forecastList.add("Weds - Cloudy - 72/63");
        forecastList.add("Thurs - Rainy - 64/51");
        forecastList.add("Fri - Foggy - 70/46");
        forecastList.add("Sat - Sunny - 76/68");
        forecastList.add("Today - Sunny - 68/54");
        forecastList.add("Tomorrow - Foggy - 75/54");
        forecastList.add("Weds - Cloudy - 74/68");
        forecastList.add("Thurs - Rainy - 66/61");
        forecastList.add("Fri - Foggy - 60/47");
        forecastList.add("Sat - Sunny - 76/65");
    }


public class FetchWeatherTask extends AsyncTask<String, String, String[]> {

    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

    @Override
    protected String[] doInBackground(String... params) {

        // HTML Load Code
        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String forecastJsonStr = null;

        // Obtain values from preferences
 //       SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
//        String postCode = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
//        String units = prefs.getString(getString(R.string.pref_units_key), getString(R.string.pref_units_default));

        String postCode = params[0];
        String units = params[1];

        // URI building inputs
        // Construct the URL for the OpenWeatherMap query
        // Possible parameters are avaiable at OWM's forecast API page, at
        // http://openweathermap.org/API#forecast
        final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
        final String QUERY_PARAM = "q";
        final String FORMAT_PARAM = "mode";
        final String UNIT_PARAM = "units";
        final String DAYS_PARAM = "cnt";
        String format = "json";
        int numDays = 7;

        try {
            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, postCode)
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNIT_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .build();

            URL url = new URL(builtUri.toString());

            //    Log.v(LOG_TAG, "Built URI " + url);

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            forecastJsonStr = buffer.toString();
            try {
                String[] dayForecast = getWeatherDataFromJson(forecastJsonStr, numDays);
//                Log.v(LOG_TAG, "Forecast String Array: " + dayForecast.toString());
                return dayForecast;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error JSON ", e);
                // Handles error returned by JSON parser.
                return null;
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attemping
            // to parse it.
            return null;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    // Three helper methods to parse forecast data

    /**
     * Take the String representing the complete forecast in JSON Format and
     * pull out the data we need to construct the Strings needed for the wireframes.
     * <p/>
     * Fortunately parsing is easy:  constructor takes the JSON string and converts it
     * into an Object hierarchy for us.
     */
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {

        // These are the names of the JSON objects that need to be extracted.
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);

        // OWM returns daily forecasts based upon the local time of the city that is being
        // asked for, which means that we need to know the GMT offset to translate this data
        // properly.

        // Since this data is also sent in-order and the first day is always the
        // current day, we're going to take advantage of that to get a nice
        // normalized UTC date for all of our weather.

        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();

        String[] resultStrs = new String[numDays];

        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        for (int i = 0; i < weatherArray.length(); i++) {
            // Get the JSON object representing the day
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            // For now, using the format "Day, description, hi/low"
            String day;
            String description;
            String highAndLow;

            // The date/time is returned as a long.  We need to convert that
            // into something human-readable, since most people won't read "1400356800" as
            // "this saturday".
            long dateTime;
            // Cheating to convert this to UTC time, which is what we want anyhow
            dateTime = dayTime.setJulianDay(julianStartDay + i);
            day = getReadableDateString(dateTime);

            // description is in a child array called "weather", which is 1 element long.
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);

            // Temperatures are in a child object called "temp".  Try not to name variables
            // "temp" when working with temperature.  It confuses everybody.
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);

            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }

        for (String s : resultStrs) {
            //    Log.v(LOG_TAG, "Forecast entry: " + s);
        }
        return resultStrs;

    }

    /* The date/time conversion code is going to be moved outside the asynctask later,
      * so for convenience we're breaking it out into its own method now.
      */
    private String getReadableDateString(long time) {
        // Because the API returns a unix timestamp (measured in seconds),
        // it must be converted to milliseconds in order to be converted to valid date.
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }

    /**
     * Prepare the weather high/lows for presentation.
     */
    private String formatHighLows(double high, double low) {
        // For presentation, assume the user doesn't care about tenths of a degree.
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);

        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }

    @Override
    protected void onPostExecute(String[] results) {
        if (results != null)
        {
            ForecastAdapter.clear();
            for(String dayForeCast : results) {
                ForecastAdapter.add(dayForeCast);
            }
        }
    }

}
}
