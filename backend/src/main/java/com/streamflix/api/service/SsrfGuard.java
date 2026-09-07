package com.streamflix.api.service;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

/**
 * The stream proxy fetches URLs that ultimately come from a crowd-sourced playlist,
 * so treat every target as untrusted input. This is a minimal SSRF guard - not
 * exhaustive, but enough to stop the proxy being used to reach loopback/internal
 * network addresses.
 */
@Component
public class SsrfGuard {

    public void assertSafeToFetch(URI uri) {
        if (uri == null || uri.getHost() == null) {
            throw new IllegalArgumentException("A fully-qualified http(s) URL is required");
        }

        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Only http/https URLs may be proxied");
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isLoopbackAddress()
                        || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress()
                        || address.isAnyLocalAddress()
                        || address.isMulticastAddress()) {
                    throw new SecurityException("Refusing to proxy a request to a private/internal address");
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host: " + uri.getHost());
        }
    }
}
