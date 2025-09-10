package ai.chatrt.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
