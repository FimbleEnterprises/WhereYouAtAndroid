package com.fimbleenterprises.whereyouat.di

import com.fimbleenterprises.whereyouat.data.MainRepository
import com.fimbleenterprises.whereyouat.data.usecases.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Singleton
    @Provides
    fun provideSaveLocationUseCase(mainRepository: MainRepository):
            SaveMemberLocsToDbUseCase {
        return SaveMemberLocsToDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetMemberLocationUseCase(mainRepository: MainRepository):
            GetMemberLocsFromApiUseCase {
        return GetMemberLocsFromApiUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideCreateTripWithApiUseCase(mainRepository: MainRepository):
            CreateTripWithApiUseCase {
        return CreateTripWithApiUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideUploadMyLocationToApiUseCase(mainRepository: MainRepository):
            UploadMyLocToApiUseCase {
        return UploadMyLocToApiUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideClearAllMemberLocationsFromDatabaseUseCase(mainRepository: MainRepository):
            DeleteAllMemberLocsFromDbUseCase {
        return DeleteAllMemberLocsFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetMemberLocationsFromDatabaseUseCase(mainRepository: MainRepository):
            GetMemberLocsFromDbUseCase {
        return GetMemberLocsFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetMyLocFromDbUseCase(mainRepository: MainRepository):
            GetMyLocFromDbUseCase {
        return GetMyLocFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideDeleteMyLocFromDbUseCase(mainRepository: MainRepository):
            DeleteMyLocFromDbUseCase {
        return DeleteMyLocFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideSaveMyLocToDbUseCase(mainRepository: MainRepository):
            SaveMyLocToDbUseCase {
        return SaveMyLocToDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetTripcodeIsActiveWithApiUseCase(mainRepository: MainRepository):
            ValidateTripCodeAgainstApiUseCase {
        return ValidateTripCodeAgainstApiUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideDeleteServiceStatusUseCase(mainRepository: MainRepository):
            DeleteServiceStatusUseCase {
        return DeleteServiceStatusUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideSaveServiceStatusUseCase(mainRepository: MainRepository):
            SaveServiceStatusUseCase {
        return SaveServiceStatusUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideGetServiceStatusUseCase(mainRepository: MainRepository):
            GetServiceStatusUseCase {
        return GetServiceStatusUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideValidateClientTripCodeUseCase():
            ValidateClientTripCodeUseCase {
        return ValidateClientTripCodeUseCase()
    }

}