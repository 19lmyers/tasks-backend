package dev.chara.tasks.backend.test.domain.service

import dev.chara.tasks.backend.data.repository.UserRepository
import io.mockk.mockk
import kotlin.test.Test

class UserServiceTest {

    private val repository: UserRepository = mockk()

    @Test
    fun `create user`() {
        // TODO figure out mockk
    }
}
