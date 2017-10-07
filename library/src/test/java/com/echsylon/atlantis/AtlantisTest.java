package com.echsylon.atlantis;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 16)
public class AtlantisTest {
    private Proxy proxy;
    private Configuration configuration;
    private MockResponse response;
    private MockRequest request;
    private Atlantis atlantis;
    private Context context;

    @Before
    public void before() {
        response = new MockResponse.Builder()
                .setStatus(1, "phrase")
                .setSetting("key1", "value1")
                .build();

        request = new MockRequest.Builder()
                .setMethod("GET")
                .setUrl("/url")
                .setSetting("key2", "value2")
                .addResponse(response)
                .build();

        configuration = new Configuration.Builder()
                .setSetting(SettingsManager.FALLBACK_BASE_URL, "host")
                .setSetting("key3", "value3")
                .addRequest(request)
                .build();

        context = mock(Context.class);
        proxy = mock(Proxy.class);
        when(proxy.getMockResponse(any(), any(), any(), any(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(response);
    }

    @Test
    public void internal_willMergeSettingsForResponse() throws Exception {
        atlantis = new Atlantis(context, new Configuration.Builder()
                .setSetting("key1", "value1")
                .addRequest(new MockRequest.Builder()
                        .setMethod("GET")
                        .setUrl("/url")
                        .setSetting("key2", "value2")
                        .addResponse(new MockResponse.Builder()
                                .setStatus(1, "phrase")
                                .setSetting("key3", "value3")
                                .build())
                        .build())
                .build());

        atlantis.start();
        atlantis.setRecordServedRequestsEnabled(true);

        // Make request (Atlantis should catch up on this).
        URL url = new URL("http://localhost:8080/url");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.getResponseCode();

        List<MockRequest> servedRequests = atlantis.servedRequests();
        SettingsManager settingsManager = servedRequests.get(0).response().settingsManager();
        assertThat(settingsManager.entryCount(), is(3));
        assertThat(settingsManager.get("key1"), is("value1"));
        assertThat(settingsManager.get("key2"), is("value2"));
        assertThat(settingsManager.get("key3"), is("value3"));
    }

    @Test
    public void public_willNotBlockOnReadingEmptyBody() throws Exception {
        atlantis = new Atlantis(context, new Configuration.Builder()
                .addRequest(new MockRequest.Builder()
                        .setMethod("GET")
                        .setUrl("/url")
                        .addResponse(new MockResponse.Builder()
                                .setStatus(1, "phrase")
                                .build())
                        .build())
                .build());

        atlantis.start();

        // Make request (Atlantis should catch up on this).
        URL url = new URL("http://localhost:8080/url");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.getResponseCode();

        final InputStream inputStream = connection.getInputStream();
        int result = Executors
                .newFixedThreadPool(1)
                .submit((Callable<Integer>) inputStream::read)
                .get(100, TimeUnit.MILLISECONDS);

        assertThat(result, is(-1)); // -1 = End-of-stream
        assertThat(connection.getResponseCode(), is(1));
        assertThat(connection.getResponseMessage(), is("phrase"));
    }

    @Test
    public void public_canStartWithConfigurationInputStream() {
        String json = "{" +
                "\"fallbackBaseUrl\": \"host\"" +
                "}";

        ByteArrayInputStream inputStream = new ByteArrayInputStream(json.getBytes());
        atlantis = new Atlantis(context, inputStream);
        atlantis.start();
        assertThat(atlantis.isRunning(), is(true));
    }

    @Test
    public void public_canStartWithConfigurationObject() {
        atlantis = new Atlantis(context, configuration);
        atlantis.start();
        assertThat(atlantis.isRunning(), is(true));
    }

    @Test
    public void public_canRecordServedRequests() throws IOException {
        // Verify possible to start recording.
        atlantis = new Atlantis(context, configuration);
        atlantis.start();
        atlantis.setRecordServedRequestsEnabled(true);
        assertThat(atlantis.isRecordingServedRequests(), is(true));

        // Make request (Atlantis should catch up on this).
        URL url = new URL("http://localhost:8080/url");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.getResponseCode();

        // Verify served request is recorded.
        atlantis.setRecordServedRequestsEnabled(false);
        List<MockRequest> served = atlantis.servedRequests();
        assertThat(atlantis.isRecordingServedRequests(), is(false));
        assertThat(served.size(), is(1));
        assertThat(served.get(0).responses().get(0).code(), is(1));
        assertThat(served.get(0).responses().get(0).phrase(), is("phrase"));
    }

    @Test
    public void public_canRecordMissingRequests() throws IOException {
        // Verify possible to start recording.
        atlantis = new Atlantis(context, proxy, configuration);
        atlantis.start();
        atlantis.setRecordMissingRequestsEnabled(true);
        assertThat(atlantis.isRecordingMissingRequests(), is(true));
    }

    @Test
    public void public_canNotRecordMissingRequestsWithoutFallbackBaseUrl() {
        atlantis = new Atlantis(context, new Configuration.Builder().build());
        atlantis.start();
        atlantis.setRecordMissingRequestsEnabled(true);
        assertThat(atlantis.isRecordingMissingRequests(), is(false));
    }

    @After
    @SuppressWarnings("ResultOfMethodCallIgnored") // Ignore file.delete()
    public void after() {
        context = null;
        configuration = null;
        if (atlantis != null) {
            deleteRecursively(atlantis.workingDirectory());
            atlantis.stop();
            atlantis = null;
        }
    }

    private void deleteRecursively(final File file) {
        if (file != null) {
            if (file.isDirectory()) {
                for (File f : file.listFiles()) {
                    deleteRecursively(f);
                }
            }
            file.delete();
        }
    }
}
