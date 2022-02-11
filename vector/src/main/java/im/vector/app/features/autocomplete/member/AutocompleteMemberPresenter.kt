/*
 * Copyright 2019 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.autocomplete.member

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.R
import im.vector.app.features.autocomplete.AutocompleteClickListener
import im.vector.app.features.autocomplete.RecyclerViewPresenter
import org.matrix.android.sdk.api.query.QueryStringValue
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.room.members.roomMemberQueryParams
import org.matrix.android.sdk.api.session.room.model.Membership
import org.matrix.android.sdk.api.session.room.model.RoomMemberSummary
import org.matrix.android.sdk.api.util.MatrixItem

class AutocompleteMemberPresenter @AssistedInject constructor(context: Context,
                                                              @Assisted val roomId: String,
                                                              session: Session,
                                                              private val controller: AutocompleteMemberController
) : RecyclerViewPresenter<AutocompleteMemberItem>(context), AutocompleteClickListener<AutocompleteMemberItem> {

    ///////////////////////////////////////////////////////////////////////////
    // FIELDS
    ///////////////////////////////////////////////////////////////////////////

    private val room by lazy { session.getRoom(roomId)!! }

    ///////////////////////////////////////////////////////////////////////////
    // INIT
    ///////////////////////////////////////////////////////////////////////////

    init {
        controller.listener = this
    }

    ///////////////////////////////////////////////////////////////////////////
    // PUBLIC API
    ///////////////////////////////////////////////////////////////////////////

    fun clear() {
        controller.listener = null
    }

    @AssistedFactory
    interface Factory {
        fun create(roomId: String): AutocompleteMemberPresenter
    }

    ///////////////////////////////////////////////////////////////////////////
    // SPECIALIZATION
    ///////////////////////////////////////////////////////////////////////////

    override fun instantiateAdapter(): RecyclerView.Adapter<*> {
        return controller.adapter
    }

    override fun onItemClick(t: AutocompleteMemberItem) {
        dispatchClick(t)
    }

    override fun onQuery(query: CharSequence?) {
        val queryParams = roomMemberQueryParams {
            displayName = if (query.isNullOrBlank()) {
                QueryStringValue.IsNotEmpty
            } else {
                QueryStringValue.Contains(query.toString(), QueryStringValue.Case.INSENSITIVE)
            }
            memberships = listOf(Membership.JOIN)
            excludeSelf = true
        }

        val membersHeader = AutocompleteMemberItem.Header(
                ID_HEADER_MEMBERS,
                context.getString(R.string.room_message_autocomplete_users)
        )
        val members = room.getRoomMembers(queryParams)
                .asSequence()
                .sortedBy { it.displayName }
                .disambiguate()
                .map { AutocompleteMemberItem.RoomMember(it) }
                .toList()

        // TODO check if user can notify everyone => compare user role to room permission setting: PowerLevelsContent
        val everyone = room.roomSummary()
                ?.takeIf { query.isNullOrBlank() || MatrixItem.NOTIFY_EVERYONE.startsWith("@$query") }
                ?.let {
                    AutocompleteMemberItem.Everyone(it)
                }

        val items = mutableListOf<AutocompleteMemberItem>().apply {
            if(members.isNotEmpty()) {
                add(membersHeader)
                addAll(members)
            }
            everyone?.let {
                val everyoneHeader = AutocompleteMemberItem.Header(
                        ID_HEADER_EVERYONE,
                        context.getString(R.string.room_message_autocomplete_notification)
                )
                add(everyoneHeader)
                add(it)
            }
        }

        controller.setData(items)
    }

    ///////////////////////////////////////////////////////////////////////////
    // CONST
    ///////////////////////////////////////////////////////////////////////////

    companion object {
        private const val ID_HEADER_MEMBERS = "ID_HEADER_MEMBERS"
        private const val ID_HEADER_EVERYONE = "ID_HEADER_EVERYONE"
    }
}

private fun Sequence<RoomMemberSummary>.disambiguate(): Sequence<RoomMemberSummary> {
    val displayNames = hashMapOf<String, Int>().also { map ->
        for (item in this) {
            item.displayName?.lowercase()?.also { displayName ->
                map[displayName] = map.getOrPut(displayName, { 0 }) + 1
            }
        }
    }

    return map { roomMemberSummary ->
        if (displayNames[roomMemberSummary.displayName?.lowercase()] ?: 0 > 1) {
            roomMemberSummary.copy(displayName = roomMemberSummary.displayName + " " + roomMemberSummary.userId)
        } else roomMemberSummary
    }
}
