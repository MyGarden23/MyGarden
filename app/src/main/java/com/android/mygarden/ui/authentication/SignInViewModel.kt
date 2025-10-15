package com.android.mygarden.ui.authentication

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.mygarden.R
import com.android.mygarden.model.authentication.AuthRepository
import com.android.mygarden.model.authentication.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for authentication.
 *
 * @property isLoading When an authentication operation is in progress.
 * @property user The currently signed-in [FirebaseUser], or null if no user is signed in.
 * @property errorMsg An error message when there is one, or null if there is no error.
 * @property signedOut True if the user is signed out, false otherwise.
 */
data class AuthUIState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false
)

private const val TAG = "SignIn"

/**
 * ViewModel for the Sign-In view.
 *
 * @property repository The repository used to perform authentication operations.
 */
class SignInViewModel(private val repository: AuthRepository = AuthRepositoryFirebase()) :
    ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState

  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  private fun getSignInOptions(context: Context) =
      GetSignInWithGoogleOption.Builder(
              serverClientId = context.getString(R.string.default_web_client_id))
          .build()

  private fun signInRequest(signInOptions: GetSignInWithGoogleOption) =
      GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

  private suspend fun getCredential(
      context: Context,
      request: GetCredentialRequest,
      credentialManager: CredentialManager
  ) = credentialManager.getCredential(context, request).credential

  fun signIn(context: Context, credentialManager: CredentialManager) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }
      val signInOptions = getSignInOptions(context)
      val signInRequest = signInRequest(signInOptions)

      try {
        val credential = getCredential(context, signInRequest, credentialManager)

        repository.signInWithGoogle(credential).fold({ user ->
          _uiState.update {
            it.copy(isLoading = false, user = user, errorMsg = null, signedOut = false)
          }
        }) { failure ->
          _uiState.update {
            it.copy(
                isLoading = false,
                errorMsg = failure.localizedMessage,
                signedOut = true,
                user = null)
          }
        }
      } catch (e: GetCredentialCancellationException) {
        Log.w(TAG, "User cancelled sheet", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Sign-in cancelled", signedOut = true, user = null)
        }
      } catch (e: NoCredentialException) {
        Log.w(TAG, "No credentials available (no Google account / provider not ready)", e)
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "No Google account found on device",
              signedOut = true,
              user = null)
        }
      } catch (e: GetCredentialException) {
        Log.w(TAG, "Other credential error: ${e::class.simpleName} ${e.message}", e)
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Failed to get credentials",
              signedOut = true,
              user = null)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Unexpected error: ${e.message}", e)
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Unexpected error", signedOut = true, user = null)
        }
      }
    }
  }
}
