/*
 * Copyright (c) 2023 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.roomprofile.polls.detail

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.airbnb.mvrx.args
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import dagger.hilt.android.AndroidEntryPoint
import im.vector.app.R
import im.vector.app.core.extensions.configureWith
import im.vector.app.core.platform.VectorBaseFragment
import im.vector.app.databinding.FragmentRoomPollDetailBinding
import im.vector.app.features.roomprofile.polls.RoomPollsType
import im.vector.app.features.roomprofile.polls.RoomPollsViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@Parcelize
data class RoomPollDetailArgs(
        val roomId: String,
        val pollId: String,
) : Parcelable

@AndroidEntryPoint
class RoomPollDetailFragment : VectorBaseFragment<FragmentRoomPollDetailBinding>() {

    @Inject lateinit var roomPollDetailController: RoomPollDetailController

    private val viewModel: RoomPollDetailViewModel by fragmentViewModel()
    private val roomPollDetailArgs: RoomPollDetailArgs by args()

    override fun getBinding(inflater: LayoutInflater, container: ViewGroup?): FragmentRoomPollDetailBinding {
        return FragmentRoomPollDetailBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        views.pollDetailRecyclerView.configureWith(
                roomPollDetailController,
                hasFixedSize = true,
        )
    }

    private fun setupToolbar(isEnded: Boolean) {
        val title = if (isEnded) getString(R.string.room_polls_ended)
                else getString(R.string.room_polls_active)

        setupToolbar(views.roomPollDetailToolbar)
                .setTitle(title)
                .allowBack()
    }

    override fun invalidate() = withState(viewModel) { state ->
        state.pollDetail ?: return@withState

        setupToolbar(state.pollDetail.isEnded)

        /*
        state.getSelectedPoll()?.let { _ ->
            roomPollDetailController.setData(state)
        }
        Unit
         */
    }
}
