package com.ecomap.usuario.di

import com.ecomap.usuario.data.repository.AuthRepositoryImpl
import com.ecomap.usuario.data.repository.BasketRepositoryImpl
import com.ecomap.usuario.data.repository.BusinessRepositoryImpl
import com.ecomap.usuario.data.repository.ProductComplaintRepositoryImpl
import com.ecomap.usuario.data.repository.RatingRepositoryImpl
import com.ecomap.usuario.data.repository.ReportRepositoryImpl
import com.ecomap.usuario.data.repository.UserDataRepositoryImpl
import com.ecomap.usuario.domain.repository.AuthRepository
import com.ecomap.usuario.domain.repository.BasketRepository
import com.ecomap.usuario.domain.repository.BusinessRepository
import com.ecomap.usuario.domain.repository.ProductComplaintRepository
import com.ecomap.usuario.domain.repository.RatingRepository
import com.ecomap.usuario.domain.repository.ReportRepository
import com.ecomap.usuario.domain.repository.UserDataRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindBusinessRepository(
        businessRepositoryImpl: BusinessRepositoryImpl
    ): BusinessRepository

    @Binds
    @Singleton
    abstract fun bindReportRepository(
        reportRepositoryImpl: ReportRepositoryImpl
    ): ReportRepository

    @Binds
    @Singleton
    abstract fun bindUserDataRepository(
        userDataRepositoryImpl: UserDataRepositoryImpl
    ): UserDataRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        productRepositoryImpl: com.ecomap.usuario.data.repository.ProductRepositoryImpl
    ): com.ecomap.usuario.domain.repository.ProductRepository

    @Binds
    @Singleton
    abstract fun bindRatingRepository(
        ratingRepositoryImpl: RatingRepositoryImpl
    ): RatingRepository

    @Binds
    @Singleton
    abstract fun bindBasketRepository(
        basketRepositoryImpl: BasketRepositoryImpl
    ): BasketRepository

    @Binds
    @Singleton
    abstract fun bindProductComplaintRepository(
        productComplaintRepositoryImpl: ProductComplaintRepositoryImpl
    ): ProductComplaintRepository
}
