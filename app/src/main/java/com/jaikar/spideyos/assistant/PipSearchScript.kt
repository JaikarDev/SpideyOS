package com.jaikar.spideyos.assistant

/** Scripted Search Companion menus — no cloud / no generative AI. */
object PipSearchScript {
    sealed class Pane {
        data class Menu(
            val title: String,
            val subtitle: String,
            val options: List<Option>,
        ) : Pane()

        data class SearchApps(
            val prompt: String = "Type an app name — Pip filters your installed apps.",
        ) : Pane()

        data class Tip(
            val lines: List<String>,
        ) : Pane()
    }

    data class Option(
        val id: String,
        val label: String,
        val hint: String = "",
    )

    fun home(userName: String): Pane.Menu = Pane.Menu(
        title = "Hey $userName — what should Pip find?",
        subtitle = "Local Search Companion · scripted · no AI",
        options = listOf(
            Option("search_apps", "Search apps", "Filter installed apps by name"),
            Option("modules", "WeaveHome modules", "ThreadBox · Inbox · SnapBooth · Nest"),
            Option("tips", "How Pip helps", "Drag me · tap menus · no cloud"),
            Option("hide", "Hide Pip", "Stop the floating companion"),
        ),
    )

    fun modules(): Pane.Menu = Pane.Menu(
        title = "Open a WeaveHome module",
        subtitle = "Tap one — Pip opens it inside the nest",
        options = listOf(
            Option("mod_assistant", "Pip chat", "Talk to Pip in-app"),
            Option("mod_messages", "ThreadBox", "Messages"),
            Option("mod_mail", "Inbox Pulse", "Mail digest"),
            Option("mod_camera", "SnapBooth", "Photo + video"),
            Option("mod_settings", "Nest", "Settings"),
            Option("back", "Back", "Return to main menu"),
        ),
    )

    fun tips(): Pane.Tip = Pane.Tip(
        lines = listOf(
            "Drag Pip anywhere — like a desktop search buddy.",
            "Tap Pip to open or close this bubble.",
            "Search apps is on-device only — no internet required.",
            "Pip never calls cloud AI from this companion.",
        ),
    )
}
