package com.jaikar.spideyos.assistant

import com.jaikar.spideyos.companion.DashSearchKind

/** Rover-style Search Companion menus for SpideyDashPip — scripted, no AI. */
object PipSearchScript {
    sealed class Pane {
        data class Menu(
            val title: String,
            val subtitle: String,
            val options: List<Option>,
        ) : Pane()

        data class Search(
            val kind: DashSearchKind,
            val prompt: String,
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

    /** Classic Rover “What would you like to search for?” hub. */
    fun home(userName: String): Pane.Menu = Pane.Menu(
        title = "What can SpideyDashPip find, $userName?",
        subtitle = "Search Companion · like Rover · on your phone’s OS",
        options = listOf(
            Option("search_apps", "Apps", "Find and open installed apps"),
            Option("search_pics", "Pictures", "Search photos on this phone"),
            Option("search_music", "Music", "Find songs · earbuds ready"),
            Option("search_video", "Video", "Search videos in your gallery"),
            Option("search_files", "Files & folders", "Mixed media search"),
            Option("search_people", "People", "Search contacts"),
            Option("search_web", "Search the Internet", "Open web search"),
            Option("modules", "WeaveHome modules", "ThreadBox · Inbox · SnapBooth · Nest"),
            Option("music", "Now playing · earbuds", "Spotify · YT Music · Amazon · TikTok…"),
            Option("wave", "Say hi / wave", "Friendly buddy wave"),
            Option("listen", "Listen to me", "Or just say Spidy out loud"),
            Option("suggest", "Suggest something", "Helpful tip for right now"),
            Option("tips", "How I help", "Say Spidy · open apps · mail · on-device"),
            Option("hide", "Wave goodbye", "Hide SpideyDashPip"),
        ),
    )

    fun modules(): Pane.Menu = Pane.Menu(
        title = "Open a WeaveHome module",
        subtitle = "Tap one — SpideyDashPip opens it",
        options = listOf(
            Option("mod_assistant", "Chat", "Talk inside WeaveHome"),
            Option("mod_messages", "ThreadBox", "Messages"),
            Option("mod_mail", "Inbox Pulse", "Mail digest"),
            Option("mod_camera", "SnapBooth", "Photo + video"),
            Option("mod_settings", "Nest", "Settings"),
            Option("back", "Back", "Return to search menu"),
        ),
    )

    fun searchPane(kind: DashSearchKind): Pane.Search {
        val prompt = when (kind) {
            DashSearchKind.APPS -> "Type an app name — I’ll dig through your apps."
            DashSearchKind.PICTURES -> "Type a photo name — I’ll dig through pictures."
            DashSearchKind.MUSIC -> "Type a song name — earbuds on when you play."
            DashSearchKind.VIDEO -> "Type a video name — I’ll sniff it out."
            DashSearchKind.FILES -> "Type a file name — pictures, songs, videos…"
            DashSearchKind.PEOPLE -> "Type a name — I’ll search your contacts."
            DashSearchKind.WEB -> "Type anything — I’ll open Internet search."
        }
        return Pane.Search(kind = kind, prompt = prompt)
    }

    fun tips(): Pane.Tip = Pane.Tip(
        lines = listOf(
            "Say “Spidy” anytime — I wake up and listen (on this phone only).",
            "Then: open apps · open WhatsApp · open Gmail · mail · messages.",
            "I tell you: here you got mail / messages — memory stays on your phone.",
            "Privacy chain: no location · no phone numbers · no hidden/sensitive files.",
            "Tap me for the menu. I walk on your real home screen — not a theme.",
            "Mic + notification access stay local. End-to-end on your device.",
        ),
    )
}
