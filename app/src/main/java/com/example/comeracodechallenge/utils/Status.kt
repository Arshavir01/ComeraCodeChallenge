package com.example.comeracodechallenge.utils

sealed class Status {
    data object Unknown: Status()
    data object Loading: Status()
    data object Success: Status()
    data class Error(val msg: String): Status()
}
