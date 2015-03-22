package com.access.accesscontrol;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by JSeptien on 3/21/2015.
 */
public class HttpServiceHandler {
    static String response = null;
    public final static int GET = 1;
    public final static int POST = 2;

    public HttpServiceHandler() {

    }

    public String downloadUrl(String urlString) throws IOException {

        InputStream is = null;
        int len = 500;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            conn.connect();
            int response = conn.getResponseCode();
            is = conn.getInputStream();

            String contentAsString = readIt(is, len);
            return contentAsString;

            //http  client

        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    public String readIt(InputStream is, int len)  throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(is, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
}
