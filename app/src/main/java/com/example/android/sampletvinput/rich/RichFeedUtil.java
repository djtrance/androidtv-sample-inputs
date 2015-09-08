/*
 * Copyright 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sampletvinput.rich;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.example.android.sampletvinput.R;
import com.example.android.sampletvinput.xmltv.XmlTvParser;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Static helper methods for fetching the channel feed.
 */
public class RichFeedUtil {
    private static final String TAG = "RichFeedUtil";

    // A key for the channel display number used in the app link intent from the xmltv_feed.
    public static final String EXTRA_DISPLAY_NUMBER = "display-number";

    private static XmlTvParser.TvListing sSampleTvListing;

    private static final boolean USE_LOCAL_XML_FEED = true;
    private static final int URLCONNECTION_CONNECTION_TIMEOUT_MS = 3000;  // 3 sec
    private static final int URLCONNECTION_READ_TIMEOUT_MS = 10000;  // 10 sec

    private RichFeedUtil() {
    }

    public static XmlTvParser.TvListing getRichTvListings(Context context) {
        Uri catalogUri =
                USE_LOCAL_XML_FEED ?
                        Uri.parse("android.resource://" + context.getPackageName() + "/"
                                + R.raw.rich_tv_input_xmltv_feed)
                        : Uri.parse(context.getResources().getString(
                                R.string.rich_input_feed_url)).normalizeScheme();
        if (sSampleTvListing != null) {
            return sSampleTvListing;
        }

        try (InputStream inputStream = getInputStream(context, catalogUri)) {
            sSampleTvListing = XmlTvParser.parse(inputStream);
        } catch (IOException e) {
            Log.e(TAG, "Error in fetching " + catalogUri, e);
        }
        return sSampleTvListing;
    }

    /**
     * Sets the target component for the app-link intents of the channels which RichTvInput
     * provides.
     *
     * @param context A Context for the package implementing the component, from which the actual
     *        package name will be retrieved.
     * @param targetClass The Class object of the desired component, from which the actual class
     *        name will be retrieved.
     */
    public static void setAppLinkActivity(Context context, Class targetClass) {
        if (sSampleTvListing.channels.size() == 0) {
            return;
        }

        for (XmlTvParser.XmlTvChannel channel : sSampleTvListing.channels) {
            if (channel.appLink == null) {
                continue;
            }
            if (channel.appLink.intentUri == null) {
                continue;
            }
            String intentUri = channel.appLink.intentUri;
            Intent intent;
            try {
                intent = Intent.parseUri(intentUri, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException e) {
                Log.w(TAG, "Invalid intent uri: " + intentUri, e);
                continue;
            }
            intent.setComponent(new ComponentName(context, targetClass));
            channel.appLink.intentUri = intent.toUri(Intent.URI_INTENT_SCHEME);
        }
    }

    public static InputStream getInputStream(Context context, Uri uri) throws IOException {
        InputStream inputStream;
        if (ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
                || ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
                || ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            inputStream = context.getContentResolver().openInputStream(uri);
        } else {
            URLConnection urlConnection = new URL(uri.toString()).openConnection();
            urlConnection.setConnectTimeout(URLCONNECTION_CONNECTION_TIMEOUT_MS);
            urlConnection.setReadTimeout(URLCONNECTION_READ_TIMEOUT_MS);
            inputStream = urlConnection.getInputStream();
        }
        return new BufferedInputStream(inputStream);
    }
}
