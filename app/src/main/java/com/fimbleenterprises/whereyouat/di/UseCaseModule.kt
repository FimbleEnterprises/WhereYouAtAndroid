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
    fun provideLeaveTripWithApiUseCase(mainRepository: MainRepository): LeaveTripWithApiUseCase {
        return LeaveTripWithApiUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provide(mainRepository: MainRepository): DeleteMemberLocFromDbUseCase {
        return DeleteMemberLocFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideClearAllMemberLocationsFromDatabaseUseCase(mainRepository: MainRepository):
            DeleteAllMemberLocsFromDbUseCase {
        return DeleteAllMemberLocsFromDbUseCase(mainRepository)
    }

    @Singleton
    @Provides
    fun provideValidateApiServerRunningUseCaseUseCase(mainRepository: MainRepository):
            ValidateApiServerRunningUseCase {
        return ValidateApiServerRunningUseCase(mainRepository)
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
    fun provideValidateClientTripCodeUseCase():
            ValidateClientTripCodeUseCase {
        return ValidateClientTripCodeUseCase()
    }

    @Singleton
    @Provides
    fun provideServiceStateUseCases(mainRepository: MainRepository):
        ServiceStateUseCases{
        return ServiceStateUseCases(mainRepository)
    }

    @Singleton
    @Provides
    fun provideRemoveMemberFromTripInApiUseCase(mainRepository: MainRepository):
            RemoveMemberFromTripInApiUseCase{
        return RemoveMemberFromTripInApiUseCase(mainRepository)
    }

}