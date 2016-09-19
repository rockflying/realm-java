/*
 * Copyright 2016 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.objectserver.internal.network;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.realm.log.RealmLog;
import io.realm.objectserver.ErrorCode;
import io.realm.objectserver.internal.Token;
import io.realm.objectserver.ObjectServerError;
import okhttp3.Response;

/**
 * This class represents the response for a authenticate request.
 */
public class AuthenticateResponse extends AuthServerResponse {

    private static final String JSON_FIELD_ACCESS_TOKEN = "access_token";
    private static final String JSON_FIELD_REFRESH_TOKEN = "refresh_token";

    private final Token accessToken;
    private final Token refreshToken;

    /**
     * Helper method for creating the proper Authenticate response. This method will set the appropriate error
     * depending on any HTTP response codes or IO errors.
     */
    static AuthenticateResponse createFrom(Response response) {
        String serverResponse;
        try {
            serverResponse = response.body().string();
        } catch (IOException e) {
            ObjectServerError error = new ObjectServerError(ErrorCode.IO_EXCEPTION, e);
            return new AuthenticateResponse(error);
        }
        RealmLog.debug("Authenticate response: " + serverResponse);
        if (response.code() != 200) {
            return new AuthenticateResponse(AuthServerResponse.createError(serverResponse, response.code()));
        } else {
            return new AuthenticateResponse(serverResponse);
        }
    }

    /**
     * Creates a unsuccessful authentication response. This should only happen in case of network / IO problems.
     */
    AuthenticateResponse(ObjectServerError error) {
        setError(error);
        this.accessToken = null;
        this.refreshToken = null;
    }

    /**
     * Parses a valid (200) server response. It might still result in a unsuccessful authentication attempt, if the
     * JSON response could not be parsed correctly.
     */
    private AuthenticateResponse(String serverResponse) {
        ObjectServerError error;
        Token accessToken;
        Token refreshToken;
        try {
            JSONObject obj = new JSONObject(serverResponse);
            accessToken = obj.has(JSON_FIELD_ACCESS_TOKEN) ?
                    Token.from(obj.getJSONObject(JSON_FIELD_ACCESS_TOKEN)) : null;
            refreshToken = obj.has(JSON_FIELD_REFRESH_TOKEN) ?
                    Token.from(obj.getJSONObject(JSON_FIELD_REFRESH_TOKEN)) : null;
            error = null;
        } catch (JSONException ex) {
            accessToken = null;
            refreshToken = null;
            //noinspection ThrowableInstanceNeverThrown
            error = new ObjectServerError(ErrorCode.JSON_EXCEPTION, ex);
        }

        setError(error);
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public Token getAccessToken() {
        return accessToken;
    }

    public Token getRefreshToken() {
        return refreshToken;
    }
}
