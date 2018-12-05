package com.smack.mdadil2019.smack.di.root;

import com.smack.mdadil2019.smack.di.LoginActivityModule;
import com.smack.mdadil2019.smack.di.NavDrawerModule;
import com.smack.mdadil2019.smack.di.RegistrationActivityModule;
import com.smack.mdadil2019.smack.ui.LoginActivity;
import com.smack.mdadil2019.smack.ui.NavDrawer;
import com.smack.mdadil2019.smack.ui.RegistrationActivity;

import dagger.Component;

@Component(modules = {ApplicationModule.class, LoginActivityModule.class, RegistrationActivityModule.class,NavDrawerModule.class})
public interface ApplicationComponent {
    void inject(LoginActivity loginActivity);

    void inject(RegistrationActivity registrationActivity);

    void inject(NavDrawer navDrawer);
}
