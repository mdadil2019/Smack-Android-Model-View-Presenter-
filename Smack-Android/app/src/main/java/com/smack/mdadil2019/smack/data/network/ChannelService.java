package com.smack.mdadil2019.smack.data.network;

import com.smack.mdadil2019.smack.data.network.model.ChannelResponse;
import com.smack.mdadil2019.smack.data.network.model.CreateUserRequest;

import java.util.ArrayList;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;

public interface ChannelService {
    @GET(ApiEndPoint.ENDPOINT_GET_ALL_CHANNELS)
    Observable<ArrayList<ChannelResponse>> getAllChannels(@Header("Authorization") String token,
                                                          @Header("Content-Type") String type);
}
