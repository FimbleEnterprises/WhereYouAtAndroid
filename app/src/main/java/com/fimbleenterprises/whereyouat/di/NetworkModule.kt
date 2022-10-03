package com.fimbleenterprises.whereyouat.di

import android.app.Application
import android.content.Context
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.data.remote.HostSelectionInterceptor
import com.fimbleenterprises.whereyouat.data.remote.WhereYouAtWebApi
import com.fimbleenterprises.whereyouat.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
/*    @Singleton
    @Provides
    fun provideHttpClient(): OkHttpClient {
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient
            .Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            *//*.addInterceptor(interceptor)*//*
            .build()
    }*/

    @Singleton
    @Provides
    fun provideHttpClient(hostApplicationInterceptor: HostSelectionInterceptor): OkHttpClient {
        val interceptor = hostApplicationInterceptor
        return OkHttpClient
            .Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(interceptor)
            .build()
    }

    @Singleton
    @Provides
    fun provideHostSelectionInterceptor(app: Application): HostSelectionInterceptor {
        return HostSelectionInterceptor(app, true)
    }

    @Singleton
    @Provides
    fun provideConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

    @Singleton
    @Provides
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        gsonConverterFactory: GsonConverterFactory
    ): Retrofit {

        return Retrofit.Builder()
            .baseUrl(Constants.DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
    }

    @Singleton
    @Provides
    fun provideWhereYouAtWebApi(retrofit: Retrofit): WhereYouAtWebApi = retrofit.create(WhereYouAtWebApi::class.java)

}