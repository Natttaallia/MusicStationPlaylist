package com.example.lab4kulbaka;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import javax.net.ssl.HttpsURLConnection;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase db;
    long lastInsertedId;
    ListView listView;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);
        db = getBaseContext().openOrCreateDatabase("app.db", MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS records (" +
                           "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                           "performer_name TEXT, " +
                           "record_name TEXT, " +
                           "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)");
        Cursor query = db.rawQuery("SELECT * FROM records;", null);

        final ArrayList<String> data = new ArrayList<>();
        while (query.moveToNext()) {
            String performer_name = query.getString(1);
            String record_name = query.getString(2);
            data.add(getString(
                    R.string.data_item,
                    performer_name,
                    record_name,
                    query.getString(3)));
        }
        query.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);
        if (!hasConnection()) {
            Toast.makeText(this,
                           getText(R.string.bad_connection_msg),
                           Toast.LENGTH_LONG)
                    .show();
        }
        final HttpRequestTask httpRequestTask = new HttpRequestTask();
        httpRequestTask.execute();
    }

    private class HttpRequestTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    handler.postDelayed(this, 20000);
                    new RequestTask().execute();
                }
            }, 0);
        }
    }

    class RequestTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... uri) {
            StringBuilder buf = new StringBuilder();
            try {
                URL url = new URL("https://media.itmo.ru/api_get_current_song.php");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setReadTimeout(10000);

                connection.setDoInput(true);
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                writer.write(getQuery());
                writer.flush();
                writer.close();
                os.close();

                connection.connect();
                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                String line;
                while ((line = reader.readLine()) != null) {
                    buf.append(line).append("\n");
                }
                reader.close();
                stream.close();
                connection.disconnect();
            } catch (ProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return (buf.toString());
        }

        private String getQuery() throws UnsupportedEncodingException {
            return "&" +
                    URLEncoder.encode("login", "UTF-8") +
                    "=" +
                    URLEncoder.encode("4707login", "UTF-8") +
                    "&" +
                    URLEncoder.encode("password", "UTF-8") +
                    "=" +
                    URLEncoder.encode("4707pass", "UTF-8");
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.e("request", result);
            try {
                JSONObject obj = obj = new JSONObject(result);
                String info = obj.getString("info");
                String[] data = info.split(" - ");
                if (lastInsertedId > 0) {
                    Cursor query = db.rawQuery("SELECT * FROM records WHERE ID = '" + lastInsertedId + "';", null);
                    query.moveToNext();
                    String performer_name = query.getString(1);
                    String record_name = query.getString(2);
                    query.close();
                    if (!performer_name.equals(data[0]) && !record_name.equals(data[1])) {
                        insert(data);
                    }
                } else {
                    insert(data);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void insert(String[] data) {
        ContentValues values = new ContentValues();
        values.put("performer_name", data[0]);
        values.put("record_name", data[1]);
        lastInsertedId = db.insert("records", "", values);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        String currentDateandTime = sdf.format(Calendar.getInstance().getTime());
        adapter.add(getString(
                R.string.data_item,
                data[0],
                data[1],
                currentDateandTime));
    }

    public boolean hasConnection() {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        NetworkInfo activeNW = cm.getActiveNetworkInfo();
        return activeNW != null && activeNW.isConnected();
    }
}