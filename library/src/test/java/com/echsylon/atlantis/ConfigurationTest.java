package com.echsylon.atlantis;

import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigurationTest {

    @Test
    public void public_canBuildValidConfigurationFromCode() {
        MockRequest.Filter filter = mock(MockRequest.Filter.class);
        MockRequest request = mock(MockRequest.class);

        Configuration configuration = new Configuration.Builder()
                .setSetting(SettingsManager.FALLBACK_BASE_URL, "url")
                .setRequestFilter(filter)
                .addRequest(request)
                .build();

        assertThat(configuration.fallbackBaseUrl(), is("url"));
        assertThat(configuration.requestFilter(), is(filter));
        assertThat(configuration.requests().size(), is(1));
        assertThat(configuration.requests().get(0), is(request));
    }

    /**
     * This feature is used when inflating a Configuration object from a JSON
     * string and there is a request filter stated.
     */
    @Test
    public void internal_canOverrideRequestFilter() {
        MockRequest.Filter filter = mock(MockRequest.Filter.class);
        Configuration configuration = new Configuration.Builder()
                .setRequestFilter(mock(MockRequest.Filter.class))
                .build();
        assertThat(configuration.requestFilter(), is(not(filter)));

        configuration.setRequestFilter(filter);
        assertThat(configuration.requestFilter(), is(filter));
    }

    /**
     * This feature is used when Atlantis is in a recording mode and a mock
     * request (with a mock response) has been created from real world data.
     */
    @Test
    public void internal_canAddNewMockRequest() {
        MockRequest request = mock(MockRequest.class);
        Configuration configuration = new Configuration.Builder()
                .addRequest(mock(MockRequest.class))
                .build();
        assertThat(configuration.requests().size(), is(1));
        assertThat(configuration.requests().get(0), is(not(request)));

        configuration.addRequest(request);
        assertThat(configuration.requests().size(), is(2));
        assertThat(configuration.requests().get(1), is(request));
    }

    @Test
    public void internal_canGetRequestWhenNoFilterSpecified() {
        HeaderManager headerManager = mock(HeaderManager.class);
        when(headerManager.keyCount()).thenReturn(0);

        Meta meta = mock(Meta.class);
        when(meta.method()).thenReturn("get");
        when(meta.headerManager()).thenReturn(headerManager);
        when(meta.url()).thenReturn("url");

        MockRequest request = mock(MockRequest.class);
        when(request.method()).thenReturn("get");
        when(request.headerManager()).thenReturn(headerManager);
        when(request.url()).thenReturn("url");

        Configuration configuration = new Configuration.Builder()
                .addRequest(mock(MockRequest.class))
                .addRequest(request)
                .addRequest(mock(MockRequest.class))
                .build();

        assertThat(configuration.findRequest(meta), is(request));
    }

    @Test
    public void internal_preventsModifyingMockedRequestsList() {
        Configuration configuration = new Configuration.Builder().build();
        List<MockRequest> requests = configuration.requests();

        assertThatThrownBy(() -> requests.add(mock(MockRequest.class)))
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> requests.remove(0))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
