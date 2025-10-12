package com.android.mygarden.ui.profile

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun NewProfileScreen(
    newProfileViewModel: NewProfileViewModel = viewModel(),
    onRegisterPressed: () -> Unit
) {}
