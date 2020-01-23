package com.traumtool.utils;

import androidx.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

import java.util.Map;

public class InputStreamVolleyRequest extends Request<byte[]> {
    private Response.Listener<byte[]> mListener;
    private Map<String, String> mParams;

    public Map<String, String> responseHeaders;

    public InputStreamVolleyRequest(int method, String url, Response.Listener<byte[]> listener,
                                    @Nullable Response.ErrorListener errorListener, Map<String, String> params) {
        super(method, url, errorListener);
        //this request would never use cache
        setShouldCache(false);
        mListener = listener;
        mParams = params;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

    @Override
    protected Response<byte[]> parseNetworkResponse(NetworkResponse response) {
        //Initialize local responseHeaders map with response headers received
        responseHeaders = response.headers;
        //Pass the response data here
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }
}
