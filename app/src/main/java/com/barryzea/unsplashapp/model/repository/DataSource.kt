package com.barryzea.unsplashapp.model.repository

import com.barryzea.unsplashapp.model.*
import com.barryzea.unsplashapp.model.server.RetrofitServiceInterface
import com.barryzea.unsplashapp.ui.common.SingleMutableLiveData

/****
 * Project UnsplashApp
 * Created by Barry Zea H. on 24/05/2022.
 * Copyright (c)  All rights reserved.
 ***/
interface DataSource {
    suspend fun callIUnsplashImages(apiKey:String, retrofitService:RetrofitServiceInterface, currentPage:Int, orderBy:String)
    fun getUnsplashImages():SingleMutableLiveData<ArrayList<UnsplashResult>>
    //para realizar la búsqueda
    suspend fun callSearchUnsplashImages(apiKey:String, retrofitService: RetrofitServiceInterface, currentPage: Int, searchValue:String)
    fun getUnsplashImagesFound():SingleMutableLiveData<UnsplashSearchResult>

    suspend fun callWallHavenImages(retrofitService:RetrofitServiceInterface, currentPage:Int, sorting:String)
    fun getWallHavenImages():SingleMutableLiveData<WallHavenResult>

    suspend fun callSearchWallHavenImages(retrofitService:RetrofitServiceInterface, currentPage: Int, searchValue:String)
    //traer datos detalle de usuario en wallhaven
    suspend fun callWallHavenUserDetail(retrofitService: RetrofitServiceInterface,idImage:String)
    fun getWHUserDetail():SingleMutableLiveData<WallHavenImageById>

    suspend fun callDownloadLocation(retrofitService:RetrofitServiceInterface, idImage:String, ixId:String)
    fun getDownloadLocation():SingleMutableLiveData<DownloadLocation>

    //para traer los detalles de la imágen de unsplash
    suspend fun callImageDetailUnsplash(retrofitService: RetrofitServiceInterface,idImage:String)
    fun getImageDetailUnsplash():SingleMutableLiveData<UnsplashResult>
}