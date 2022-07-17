package com.barryzea.unsplashapp.di


import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.barryzea.unsplashapp.BuildConfig
import com.barryzea.unsplashapp.model.database.ImageDatabase
import com.barryzea.unsplashapp.model.database.RoomDataSource
import com.barryzea.unsplashapp.model.repository.DataSource
import com.barryzea.unsplashapp.model.repository.DataSourceImpl
import com.barryzea.unsplashapp.model.repository.LocalDataSource
import com.barryzea.unsplashapp.model.server.RetrofitServiceInterface
import com.barryzea.unsplashapp.model.server.RetrofitServiceModuleImpl
import com.barryzea.unsplashapp.ui.common.Constants.CLIENT_ID
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton


/****
 * Project UnsplashApp
 * Created by Barry Zea H. on 24/05/2022.
 * Copyright (c)  All rights reserved.
 ***/
@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    @Named(CLIENT_ID)
    fun apiKeyProvider():String=BuildConfig.myAPI_KEY

    @Provides
    @Singleton
    fun dataBaseProvider(app:Application)= Room.databaseBuilder(
        app,
        ImageDatabase::class.java,
        "image-db"
    )//.fallbackToDestructiveMigration()
        .build()

    @Provides
    fun localDataSourceProvider(db:ImageDatabase):LocalDataSource = RoomDataSource(db)
    @Provides
    fun retrofitDataSourceProvides():RetrofitServiceInterface=RetrofitServiceModuleImpl()

    @Provides
    fun remoteDatasourceProvides():DataSource=DataSourceImpl()

}