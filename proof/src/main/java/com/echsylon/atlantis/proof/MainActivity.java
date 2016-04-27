package com.echsylon.atlantis.proof;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        output = (TextView) findViewById(R.id.output);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "Send")
                .setIcon(android.R.drawable.ic_menu_upload)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:
                performNetworkRequest();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void performNetworkRequest() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    URL url = new URL("http://localhost:8080/path/to/resource");
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("requestHeaderKey1", "requestHeaderValue1");
                    connection.setRequestProperty("requestHeaderKey2", "requestHeaderValue2");

                    return readNetworkResponse(connection.getInputStream());
                } catch (MalformedURLException e) {
                    return printThrowableToString(e);
                } catch (IOException e) {
                    return printThrowableToString(e);
                }
            }

            @Override
            protected void onPostExecute(String result) {
                output.setText(result);
            }
        }.execute();
    }

    private String readNetworkResponse(InputStream stream) {
        InputStream inputStream = new BufferedInputStream(stream);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        StringBuilder stringBuilder = new StringBuilder();
        String line;

        try {
            while ((line = bufferedReader.readLine()) != null)
                stringBuilder.append(line);
            return stringBuilder.toString();
        } catch (IOException e) {
            return "";
        } finally {
            closeSilently(bufferedReader);
            closeSilently(inputStreamReader);
            closeSilently(inputStream);
        }
    }

    private void closeSilently(Closeable closeable) {
        if (closeable == null)
            return;

        try {
            closeable.close();
        } catch (IOException e) {
            // Respectfully ignore any exceptions
        }
    }

    private String printThrowableToString(Throwable throwable) {
        if (throwable == null)
            return "";

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

}
