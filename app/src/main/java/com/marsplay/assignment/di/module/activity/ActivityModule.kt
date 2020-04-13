package com.example.daggerexample.di

import com.marsplay.assignment.module.activity.DisplayImagesActivity
import com.marsplay.assignment.module.activity.MainActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ActivityModule {

    @ContributesAndroidInjector()
    abstract fun contributeMainActivity(): MainActivity

    @ContributesAndroidInjector()
    abstract fun contributeDisplayImagesActivity(): DisplayImagesActivity
}
