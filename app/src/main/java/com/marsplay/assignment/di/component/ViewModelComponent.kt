package com.marsplay.assignment.di.component

import android.app.Application
import com.marsplay.assignment.application.ApplicationShared
import com.marsplay.assignment.di.module.fragment.FragmentModule
import com.example.daggerexample.di.ActivityModule
import com.example.daggerexample.di.InjectionModules
import dagger.BindsInstance
import dagger.Component
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton


@Component(
    modules = [
        InjectionModules::class,
        ActivityModule::class,
        FragmentModule::class,
        AndroidSupportInjectionModule::class
    ]
)
@Singleton
interface ViewModelComponent {

    /*
     * We will call this builder interface from our custom Application class.
     * This will set our application object to the AppComponent.
     * So inside the AppComponent the application instance is available.
     * So this application instance can be accessed by our modules
     * such as ApiModule when needed
     *
     * */
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): ViewModelComponent
    }

    /*
     * This is our custom Application class
     * */
    fun inject(appController: ApplicationShared)
}