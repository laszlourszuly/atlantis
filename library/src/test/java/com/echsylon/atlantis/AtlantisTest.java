package com.echsylon.atlantis;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

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
                .build();

        request = new MockRequest.Builder()
                .setMethod("GET")
                .setUrl("/url")
                .addResponse(response)
                .build();

        configuration = new Configuration.Builder()
                .setFallbackBaseUrl("host")
                .addRequest(request)
                .build();

        context = mock(Context.class);
        proxy = mock(Proxy.class);
        when(proxy.getMockRequest(any(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(request);
    }

    @Test
    public void public_canStartWithConfigurationInputStream() {
        String json = "{'fallbackBaseUrl': 'host'}";
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

        // Make request (Atlantis should catch up on this) and verify that the
        // expected response is delivered.
        URL url = new URL("http://localhost:8080/real");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        assertThat(connection.getResponseCode(), is(response.code()));
        assertThat(connection.getResponseMessage(), is(response.phrase()));
    }

    @Test
    public void public_canNotRecordMissingRequestsWithoutFallbackBaseUrl() {
        atlantis = new Atlantis(context, new Configuration.Builder().build());
        atlantis.start();
        atlantis.setRecordMissingRequestsEnabled(true);
        assertThat(atlantis.isRecordingMissingRequests(), is(false));
    }

    @After
    public void after() {
        context = null;
        configuration = null;
        if (atlantis != null) {
            atlantis.stop();
            atlantis = null;
        }
    }
}
