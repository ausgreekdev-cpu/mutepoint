package com.mutepoint.app.core.automation.di

import com.mutepoint.app.core.automation.DefaultWorkflows
import com.mutepoint.app.core.automation.engine.AppEventBus
import com.mutepoint.app.core.automation.engine.ActionHandler
import com.mutepoint.app.core.automation.engine.ActionHandlerImpl
import com.mutepoint.app.core.automation.engine.ConditionEvaluator
import com.mutepoint.app.core.automation.engine.WorkflowEngine
import com.mutepoint.app.core.automation.engine.WorkflowTriggerListener
import com.mutepoint.app.core.automation.engine.TriggerScheduler
import com.mutepoint.app.core.automation.repository.WorkflowRepository
import com.mutepoint.app.core.automation.repository.WorkflowRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomationModule {

    @Binds
    @Singleton
    abstract fun bindWorkflowRepository(impl: WorkflowRepositoryImpl): WorkflowRepository

    @Binds
    @Singleton
    abstract fun bindActionHandler(impl: ActionHandlerImpl): ActionHandler

    companion object {
        @Provides
        @Singleton
        fun provideAppEventBus(): AppEventBus = AppEventBus()

        @Provides
        @Singleton
        fun provideDefaultWorkflows(
            workflowRepository: WorkflowRepository,
        ): DefaultWorkflows = DefaultWorkflows(workflowRepository)

        @Provides
        @Singleton
        fun provideWorkflowTriggerListener(
            appEventBus: AppEventBus,
            workflowRepository: WorkflowRepository,
            workflowEngine: WorkflowEngine,
            actionHandler: ActionHandler,
        ): WorkflowTriggerListener = WorkflowTriggerListener(
            appEventBus, workflowRepository, workflowEngine, actionHandler,
        )

        @Provides
        @Singleton
        fun provideConditionEvaluator(): ConditionEvaluator = ConditionEvaluator()

        @Provides
        @Singleton
        fun provideWorkflowEngine(conditionEvaluator: ConditionEvaluator): WorkflowEngine =
            WorkflowEngine(conditionEvaluator)

        @Provides
        @Singleton
        fun provideTriggerScheduler(@ApplicationContext context: android.content.Context): TriggerScheduler =
            TriggerScheduler(context)
    }
}
