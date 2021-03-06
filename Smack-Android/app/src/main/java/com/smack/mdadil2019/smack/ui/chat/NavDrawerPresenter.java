package com.smack.mdadil2019.smack.ui.chat;

import android.content.Context;

import com.smack.mdadil2019.smack.data.db.ChannelDatabase;
import com.smack.mdadil2019.smack.data.db.MessageDatabase;
import com.smack.mdadil2019.smack.data.network.ChannelService;
import com.smack.mdadil2019.smack.data.network.MessageService;
import com.smack.mdadil2019.smack.data.network.model.ChannelResponse;
import com.smack.mdadil2019.smack.data.network.model.MessageResponse;
import com.smack.mdadil2019.smack.data.prefs.AppPreferencesHelper;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import io.reactivex.Completable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.IO;
import io.socket.client.Manager;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class NavDrawerPresenter implements NavDrawerMVP.Presenter {
    NavDrawerMVP.View view;
    Socket mSocket;
    Manager manager;
    ChannelResponse mChannelResponse;
    ChannelService mChannelService;
    AppPreferencesHelper sharedPrefs;
    MessageService mMessageService;
    Context mContext;


    ArrayList<ChannelResponse> channels;
    ArrayList<MessageResponse> messages;

    public NavDrawerPresenter(Context context, AppPreferencesHelper appPreferencesHelper, ChannelService channelService, ChannelResponse channelResponse,
                                MessageService messageService){
            manager = new Manager();
            mChannelResponse = channelResponse;
            mChannelService = channelService;
            sharedPrefs = appPreferencesHelper;
            mMessageService = messageService;
            mContext = context;
        try {
            mSocket = IO.socket("https://adilchat.herokuapp.com");
        } catch (URISyntaxException e) {
            view.showUIMessage(e.getMessage());
        }
    }


    @Override
    public void setView(NavDrawerMVP.View view) {
        this.view = view;
        mSocket.connect();

    }

    @Override
    public void loadAddedChannels() {
        if(!mSocket.hasListeners("channelCreated")) {
            mSocket.on("channelCreated", new Emitter.Listener() {
                @Override
                public void call(Object... args) {


                    ChannelResponse channelResponse = new ChannelResponse();
                    channelResponse.setChannelName(String.valueOf(args[0]));
                    channelResponse.setChannelDesc(String.valueOf(args[1]));
                    channelResponse.setChannelId(String.valueOf(args[2]));

                    channels.add(channelResponse);
                    view.addChannelInList(channels);
                    view.hideProgressBar();
                }

            });
        }
    }

    @Override
    public void createChannel() {
        final String channelName = view.getCreateChannelName();
        final String channelDesc = view.getCreateChannelDescription();
        view.showProgressbar();
        mSocket.emit("newChannel",channelName,channelDesc);
        mSocket.on(Socket.EVENT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                view.showUIMessage(String.valueOf(args[0]) );
                view.hideProgressBar();
            }
        });
    }


    @Override
    public void getAllChannels() {
       /*
        1. Create ChannelRequest model with Retrofit
        2. send api request
        3. store response in ChannelResponse Array
        4. return the response

         */
       view.showProgressbar();
       mChannelService.getAllChannels("Bearer "+ sharedPrefs.getAuthToken(),"application/json; charset=utf-8")
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe(new Observer<ArrayList<ChannelResponse>>() {
                   @Override
                   public void onSubscribe(Disposable d) {
                       channels = new ArrayList<>();
                   }

                   @Override
                   public void onNext(final ArrayList<ChannelResponse> channelResponse) {
                        channels = channelResponse;
                       Completable.fromAction(new Action() {
                           @Override
                           public void run() throws Exception {
                               for(ChannelResponse ch: channelResponse){
                                   ChannelDatabase.getInstance(mContext).getChannelDao().insertChannel(ch);
                               }
                           }
                       }).subscribeOn(Schedulers.io())
                               .observeOn(AndroidSchedulers.mainThread())
                               .subscribe();
                   }

                   @Override
                   public void onError(Throwable e) {
                        view.showUIMessage(e.getMessage());
                        view.hideProgressBar();
                   }

                   @Override
                   public void onComplete() {
                        view.addChannelInList(channels);
                        view.hideProgressBar();
                   }
               });

    }


    @Override
    public void openChatRoom(String channelName) {
        view.showProgressbar();
        String id = "";
        for(ChannelResponse res : channels){
            if(res.getChannelName().equals(channelName)){
                id = res.getChannelId();
                break;
            }
        }
        mMessageService.getAllMessages(id,"Bearer "+ sharedPrefs.getAuthToken(),"application/json; charset=utf-8")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ArrayList<MessageResponse>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        messages = new ArrayList<>();
                    }

                    @Override
                    public void onNext(ArrayList<MessageResponse> messageResponses) {
                        messages = messageResponses;
                        Completable.fromAction(new Action() {
                            @Override
                            public void run() throws Exception {
                                for(MessageResponse m: messages){
                                    MessageDatabase.getInstance(mContext).getMessageDao().insertMessage(m);
                                }
                            }
                        }).subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe();
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showUIMessage(e.getMessage());
                        view.hideProgressBar();
                    }

                    @Override
                    public void onComplete() {
                        //pass data to adapter to populate the messages
                        view.updateRecyclerView(messages);
                        view.hideProgressBar();
                    }
                });
    }


    @Override
    public void getMessage() {
        if(!mSocket.hasListeners("messageCreated")) {
            mSocket.on("messageCreated", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    MessageResponse messageResponse = new MessageResponse();
                    messageResponse.setMessageBody(args[0].toString());
                    messageResponse.setChannelId(args[2].toString());
                    messageResponse.setUserName(args[3].toString());
                    messageResponse.setUserAvatar(args[4].toString());
//                messageResponse.setAvatarColor(args[5].toString());
                    messageResponse.setTimeStamp(args[7].toString());
                    messages.add(messageResponse);
                    view.updateRecyclerView(messages);
                }
            });
        }
    }

    @Override
    public void sendMessage(String channelName,String messsge) {
        if(messsge==null || messsge.equals("")){
            view.showUIMessage("Please enter message to send");
        }else{

            String id = "";
            for(ChannelResponse res : channels){
                if(res.getChannelName().equals(channelName)){
                    id = res.getChannelId();
                    break;
                }
            }
            String userId = sharedPrefs.getId();
            String userName = sharedPrefs.getUserName();
            String userAvatarName = sharedPrefs.getAvatarName();
            String userAvatarColor = sharedPrefs.getAvatarColor();
            Date date = new Date();
            String timeStamp = String.valueOf(date.getTime());
            mSocket.emit("newMessage", messsge,userId,id,userName,userAvatarName,userAvatarColor,"",timeStamp);
            view.clearMessageText();
        }
    }

    @Override
    public void logOut() {
        sharedPrefs.setLoggedIn(false);
        view.showLoginScreen();
    }

    @Override
    public void getAllMessagesOffline(String channelName) {
        String id = "";
        for(ChannelResponse res : channels){
            if(res.getChannelName().equals(channelName)){
                id = res.getChannelId();
                break;
            }
        }
        MessageDatabase.getInstance(mContext).getMessageDao().getAllMessages(id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<MessageResponse>>() {
                    @Override
                    public void accept(List<MessageResponse> messageResponses) throws Exception {
                        messages = new ArrayList<>();
                        messages.addAll(messageResponses);
                        view.updateRecyclerView(messages);
                    }
                });
    }

    @Override
    public void getAllChannelsOffline() {
        ChannelDatabase.getInstance(mContext).getChannelDao().getChannels()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<ChannelResponse>>() {
                    @Override
                    public void accept(List<ChannelResponse> channelResponses) throws Exception {
                        channels = new ArrayList<>();
                        channels.addAll(channelResponses);
                        view.addChannelInList(channels);
                    }
                });
    }


}
