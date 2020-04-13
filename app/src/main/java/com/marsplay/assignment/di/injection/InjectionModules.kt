package com.example.daggerexample.di

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marsplay.assignment.di.keys.ViewModelKey
import com.marsplay.assignment.di.module.provides.ViewModelFactory
import com.marsplay.assignment.module.fragments.viewmodels.DetailViewModel
import com.marsplay.assignment.module.fragments.viewmodels.MainViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap


@Module
internal abstract class InjectionModules {

    @Binds
    internal abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory

    /*
     * This method basically says
     * inject this object into a Map using the @IntoMap annotation,
     * with the  MainViewModel.class as key,
     * and a Provider that will build a MainViewModel
     * object.
     *
     * */

    @Binds
    @IntoMap
    @ViewModelKey(MainViewModel::class)
    protected abstract fun bindMainViewModel(moviesListViewModel: MainViewModel): AndroidViewModel

    @Binds
    @IntoMap
    @ViewModelKey(DetailViewModel::class)
    protected abstract fun bindDetailViewModel(moviesListViewModel: DetailViewModel): ViewModel
}