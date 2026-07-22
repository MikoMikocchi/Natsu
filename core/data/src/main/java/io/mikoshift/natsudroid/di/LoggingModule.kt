package io.mikoshift.natsudroid.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsudroid.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsudroid.core.common.logging.AppLogger
import io.mikoshift.natsudroid.data.analytics.LoggingAnalyticsTracker
import io.mikoshift.natsudroid.data.logging.AndroidAppLogger
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    @Binds
    @Singleton
    abstract fun bindAppLogger(impl: AndroidAppLogger): AppLogger

    @Binds
    @Singleton
    abstract fun bindAnalyticsTracker(impl: LoggingAnalyticsTracker): AnalyticsTracker
}
