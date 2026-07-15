package io.mikoshift.natsu.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.mikoshift.natsu.core.common.analytics.AnalyticsTracker
import io.mikoshift.natsu.core.common.logging.AppLogger
import io.mikoshift.natsu.data.analytics.LoggingAnalyticsTracker
import io.mikoshift.natsu.data.logging.AndroidAppLogger
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
