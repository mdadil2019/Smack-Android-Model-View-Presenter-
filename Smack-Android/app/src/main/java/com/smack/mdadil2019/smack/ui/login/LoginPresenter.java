package com.smack.mdadil2019.smack.ui.login;


import com.smack.mdadil2019.smack.data.network.LoginService;
import com.smack.mdadil2019.smack.data.network.model.LoginRequest;
import com.smack.mdadil2019.smack.data.network.model.LoginResponse;
import com.smack.mdadil2019.smack.data.prefs.AppPreferencesHelper;
import com.smack.mdadil2019.smack.ui.login.LoginActivityMVP.View;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class LoginPresenter implements LoginActivityMVP.Presenter {

    LoginService loginService;
    LoginRequest loginRequest;
    AppPreferencesHelper mPrefs;
    Disposable disposable;

    public LoginPresenter(LoginService loginApiService, LoginRequest loginReq, AppPreferencesHelper preferencesHelper){
        loginService = loginApiService;
        loginRequest = loginReq;
        mPrefs = preferencesHelper;
    }

    View view;

    @Override
    public void setView(View view) {
        this.view = view;
        if(mPrefs.getLoggedInStatus())
            view.openNavigationDrawer();
    }

    @Override
    public boolean checkLoginStatus() {
        return false;
    }

    @Override
    public void setLoginStatus(boolean loginStatus) {

    }

    @Override
    public void login(String userName, String password) {
        if(userName!=null && password!=null){
            view.showProgressbar();
            loginRequest.setEmail(userName);
            loginRequest.setPassword(password);
            Observable<LoginResponse> loginResponse = loginService.loginRequest(loginRequest);
            loginResponse.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Observer<LoginResponse>() {
                @Override
                public void onSubscribe(Disposable d) {
                    disposable = d;
                }

                @Override
                public void onNext(LoginResponse loginResponse) {
                    view.showMessage("Welcome " + loginResponse.getUser());
                }

                @Override
                public void onError(Throwable e) {
                    view.showMessage(e.getMessage());
                    view.hideProgressBar();
                }

                @Override
                public void onComplete() {
                    mPrefs.setLoggedIn(true);
                    view.showMessage("Login successful");
                    disposable.dispose();
                    view.openNavigationDrawer();
                    view.hideProgressBar();
                }
            });
        }else{
            view.showMessage("Please enter credentials to login");
        }
    }

    @Override
    public void openSignUpActivity() {
        view.signUpActivity();
    }
}
