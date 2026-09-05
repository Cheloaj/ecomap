package com.ecomap.socio.di

import com.ecomap.socio.data.local.SubscriptionManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SubscriptionEntryPoint {
    fun subscriptionManager(): SubscriptionManager
}
