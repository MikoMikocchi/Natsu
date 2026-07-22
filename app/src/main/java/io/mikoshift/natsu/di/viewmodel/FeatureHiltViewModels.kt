package io.mikoshift.natsu.di.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.mikoshift.natsu.core.domain.repository.DocumentPackageRepository
import io.mikoshift.natsu.core.domain.usecase.ChangePasswordUseCase
import io.mikoshift.natsu.core.domain.usecase.DeleteAccountUseCase
import io.mikoshift.natsu.core.domain.usecase.EnsurePackageDownloadedUseCase
import io.mikoshift.natsu.core.domain.usecase.ForgotPasswordUseCase
import io.mikoshift.natsu.core.domain.usecase.ImportDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.ListDictionariesUseCase
import io.mikoshift.natsu.core.domain.usecase.LoginUseCase
import io.mikoshift.natsu.core.domain.usecase.LogoutUseCase
import io.mikoshift.natsu.core.domain.usecase.LookupWordUseCase
import io.mikoshift.natsu.core.domain.usecase.MarkDocumentDeletedUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveDocumentUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveLibraryUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveReaderSettingsUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSessionsUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveSyncStatusUseCase
import io.mikoshift.natsu.core.domain.usecase.ObserveUserProfileUseCase
import io.mikoshift.natsu.core.domain.usecase.OpenDocumentPackageUseCase
import io.mikoshift.natsu.core.domain.usecase.RegisterUseCase
import io.mikoshift.natsu.core.domain.usecase.ResetPasswordUseCase
import io.mikoshift.natsu.core.domain.usecase.RevokeSessionUseCase
import io.mikoshift.natsu.core.domain.usecase.SearchDocumentsUseCase
import io.mikoshift.natsu.core.domain.usecase.SyncDocumentsUseCase
import io.mikoshift.natsu.core.domain.usecase.ToggleDictionaryUseCase
import io.mikoshift.natsu.core.domain.usecase.UpdateReaderSettingsUseCase
import io.mikoshift.natsu.core.domain.usecase.UpdateReadingProgressUseCase
import io.mikoshift.natsu.ui.auth.ForgotPasswordViewModel
import io.mikoshift.natsu.ui.auth.LoginViewModel
import io.mikoshift.natsu.ui.auth.RegisterViewModel
import io.mikoshift.natsu.ui.auth.ResetPasswordViewModel
import io.mikoshift.natsu.ui.library.LibraryViewModel
import io.mikoshift.natsu.ui.profile.ChangePasswordViewModel
import io.mikoshift.natsu.ui.profile.ProfileViewModel
import io.mikoshift.natsu.ui.reader.ReaderViewModel
import javax.inject.Inject

@HiltViewModel
class HiltLoginViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    login: LoginUseCase,
) : LoginViewModel(context, login)

@HiltViewModel
class HiltRegisterViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    register: RegisterUseCase,
) : RegisterViewModel(context, register)

@HiltViewModel
class HiltForgotPasswordViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    forgotPassword: ForgotPasswordUseCase,
) : ForgotPasswordViewModel(context, forgotPassword)

@HiltViewModel
class HiltResetPasswordViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    resetPassword: ResetPasswordUseCase,
    savedStateHandle: SavedStateHandle,
) : ResetPasswordViewModel(context, resetPassword, savedStateHandle)

@HiltViewModel
class HiltLibraryViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    observeLibrary: ObserveLibraryUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    syncDocuments: SyncDocumentsUseCase,
    importDocument: ImportDocumentUseCase,
    searchDocuments: SearchDocumentsUseCase,
    markDocumentDeleted: MarkDocumentDeletedUseCase,
) : LibraryViewModel(
    context,
    observeLibrary,
    observeSyncStatus,
    syncDocuments,
    importDocument,
    searchDocuments,
    markDocumentDeleted,
)

@HiltViewModel
class HiltProfileViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    observeUserProfile: ObserveUserProfileUseCase,
    observeSessions: ObserveSessionsUseCase,
    logoutUseCase: LogoutUseCase,
    deleteAccount: DeleteAccountUseCase,
    revokeSession: RevokeSessionUseCase,
    listDictionaries: ListDictionariesUseCase,
    toggleDictionary: ToggleDictionaryUseCase,
) : ProfileViewModel(
    context,
    observeUserProfile,
    observeSessions,
    logoutUseCase,
    deleteAccount,
    revokeSession,
    listDictionaries,
    toggleDictionary,
)

@HiltViewModel
class HiltChangePasswordViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    changePassword: ChangePasswordUseCase,
) : ChangePasswordViewModel(context, changePassword)

@HiltViewModel
class HiltReaderViewModel
@Inject
constructor(
    @ApplicationContext context: Context,
    observeDocument: ObserveDocumentUseCase,
    ensurePackageDownloaded: EnsurePackageDownloadedUseCase,
    openDocumentPackage: OpenDocumentPackageUseCase,
    updateReadingProgress: UpdateReadingProgressUseCase,
    observeReaderSettings: ObserveReaderSettingsUseCase,
    updateReaderSettings: UpdateReaderSettingsUseCase,
    lookupWord: LookupWordUseCase,
    listDictionaries: ListDictionariesUseCase,
    documentPackageRepository: DocumentPackageRepository,
    savedStateHandle: SavedStateHandle,
) : ReaderViewModel(
    context,
    observeDocument,
    ensurePackageDownloaded,
    openDocumentPackage,
    updateReadingProgress,
    observeReaderSettings,
    updateReaderSettings,
    lookupWord,
    listDictionaries,
    documentPackageRepository,
    savedStateHandle,
)
