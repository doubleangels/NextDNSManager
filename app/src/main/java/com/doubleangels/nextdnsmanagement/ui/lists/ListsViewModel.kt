package com.doubleangels.nextdnsmanagement.ui.lists

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doubleangels.nextdnsmanagement.api.EntryList
import com.doubleangels.nextdnsmanagement.data.EntryListController

class ListsViewModel : ViewModel() {
    var message by mutableStateOf<String?>(null)

    val denylist = EntryListController(viewModelScope, EntryList.DENYLIST) { message = it }
    val allowlist = EntryListController(viewModelScope, EntryList.ALLOWLIST) { message = it }
}
