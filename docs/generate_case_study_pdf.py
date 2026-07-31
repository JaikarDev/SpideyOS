#!/usr/bin/env python3
"""Generate SpideyOS LinkedIn case-study PDF."""

from pathlib import Path

from reportlab.lib.colors import Color, HexColor, white, black
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import (
    SimpleDocTemplate,
    Paragraph,
    Spacer,
    PageBreak,
    Table,
    TableStyle,
    KeepTogether,
    ListFlowable,
    ListItem,
)
from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_JUSTIFY

OUT = Path(__file__).resolve().parent / "SpideyOS-Case-Study.pdf"

RED = HexColor("#C41E3A")
BLUE = HexColor("#0B1D36")
NAVY = HexColor("#102A43")
WEB = HexColor("#E8EEF5")
ACCENT = HexColor("#F4C430")
MUTED = HexColor("#627D98")


def styles():
    base = getSampleStyleSheet()
    return {
        "cover_title": ParagraphStyle(
            "cover_title",
            parent=base["Title"],
            fontName="Helvetica-Bold",
            fontSize=36,
            textColor=white,
            alignment=TA_CENTER,
            spaceAfter=12,
            leading=42,
        ),
        "cover_sub": ParagraphStyle(
            "cover_sub",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=14,
            textColor=WEB,
            alignment=TA_CENTER,
            leading=20,
        ),
        "h1": ParagraphStyle(
            "h1",
            parent=base["Heading1"],
            fontName="Helvetica-Bold",
            fontSize=22,
            textColor=RED,
            spaceBefore=6,
            spaceAfter=14,
            leading=26,
        ),
        "h2": ParagraphStyle(
            "h2",
            parent=base["Heading2"],
            fontName="Helvetica-Bold",
            fontSize=14,
            textColor=NAVY,
            spaceBefore=10,
            spaceAfter=8,
            leading=18,
        ),
        "body": ParagraphStyle(
            "body",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=11,
            textColor=NAVY,
            alignment=TA_JUSTIFY,
            leading=16,
            spaceAfter=8,
        ),
        "bullet": ParagraphStyle(
            "bullet",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=11,
            textColor=NAVY,
            leading=15,
            leftIndent=8,
        ),
        "quote": ParagraphStyle(
            "quote",
            parent=base["Normal"],
            fontName="Helvetica-Oblique",
            fontSize=16,
            textColor=RED,
            alignment=TA_CENTER,
            leading=22,
            spaceBefore=16,
            spaceAfter=16,
        ),
        "footer": ParagraphStyle(
            "footer",
            parent=base["Normal"],
            fontName="Helvetica",
            fontSize=8,
            textColor=MUTED,
            alignment=TA_CENTER,
        ),
        "cta": ParagraphStyle(
            "cta",
            parent=base["Normal"],
            fontName="Helvetica-Bold",
            fontSize=12,
            textColor=NAVY,
            alignment=TA_CENTER,
            leading=18,
        ),
    }


def draw_cover(canvas, doc):
    canvas.saveState()
    w, h = letter
    canvas.setFillColor(BLUE)
    canvas.rect(0, 0, w, h, fill=1, stroke=0)
    # web-ish diagonals
    canvas.setStrokeColor(HexColor("#1F3A5F"))
    canvas.setLineWidth(0.6)
    for i in range(0, int(w) + 80, 40):
        canvas.line(i, 0, i - 120, h)
    for i in range(0, int(h) + 80, 40):
        canvas.line(0, i, w, i - 80)
    canvas.setFillColor(RED)
    canvas.rect(0, h - 18, w, 18, fill=1, stroke=0)
    canvas.rect(0, 0, w, 18, fill=1, stroke=0)
    canvas.restoreState()


def draw_inner(canvas, doc):
    canvas.saveState()
    w, h = letter
    canvas.setFillColor(RED)
    canvas.rect(0, h - 10, w, 10, fill=1, stroke=0)
    canvas.setFillColor(MUTED)
    canvas.setFont("Helvetica", 8)
    canvas.drawString(0.75 * inch, 0.45 * inch, "SpideyOS Case Study  |  Jaikar  |  Unofficial fan project")
    canvas.drawRightString(w - 0.75 * inch, 0.45 * inch, f"Page {doc.page}")
    canvas.restoreState()


def build():
    s = styles()
    story = []

    # Cover content (drawn on blue bg via first page template)
    story.append(Spacer(1, 2.2 * inch))
    story.append(Paragraph("SPIDEYOS", s["cover_title"]))
    story.append(Paragraph("Android Spider-Man Experience", s["cover_sub"]))
    story.append(Spacer(1, 0.35 * inch))
    story.append(
        Paragraph(
            "A companion suite for Android 15+ phones &amp; tablets<br/>"
            "Launcher · Gemini Assistant · Web Notifications · Messages · Mail · Peter Camera",
            s["cover_sub"],
        )
    )
    story.append(Spacer(1, 1.1 * inch))
    story.append(Paragraph("Case Study by <b>Jaikar</b>", s["cover_sub"]))
    story.append(Paragraph("Open Source · No Root · Snapdragon &amp; MediaTek", s["cover_sub"]))
    story.append(Spacer(1, 0.8 * inch))
    story.append(
        Paragraph(
            "Unofficial fan / inspired project. Not affiliated with Marvel Entertainment or Sony.",
            s["footer"],
        )
    )
    story.append(PageBreak())

    # Page 2 — Idea
    story.append(Paragraph("1. The Idea", s["h1"]))
    story.append(
        Paragraph(
            "What if your Android phone didn’t just <i>look</i> like Spidey — what if Spidey "
            "<i>lived</i> on it? SpideyOS turns the device into a character-driven companion: "
            "the home screen wears a web motif, notifications arrive in Spidey’s voice, and a "
            "Gemini-powered assistant greets you by name.",
            s["body"],
        )
    )
    story.append(Paragraph("“Hey Jaikar, you’ve got mail.”", s["quote"]))
    story.append(
        Paragraph(
            "The product goal is presence: a personal assistant that swings through messages, "
            "mail, camera, and system alerts — not a static theme pack.",
            s["body"],
        )
    )
    story.append(PageBreak())

    # Page 3 — Problem
    story.append(Paragraph("2. The Problem", s["h1"]))
    story.append(
        Paragraph(
            "Stock Android is powerful but emotionally flat. Icon packs change colors; they don’t "
            "change how the phone talks to you. Generic assistants sit in a separate app and ignore "
            "the rest of the system. Fans want immersion; engineers want installable software that "
            "works without rooting every OEM skin.",
            s["body"],
        )
    )
    bullets = [
        "Themes don’t speak — they only recolor.",
        "Assistants are generic and siloed from notifications, mail, and camera.",
        "Custom ROMs exclude most users and break OEM updates.",
        "Portfolio projects rarely show end-to-end system UX + AI product thinking.",
    ]
    for b in bullets:
        story.append(Paragraph(f"• {b}", s["bullet"]))
    story.append(PageBreak())

    # Page 4 — Solution
    story.append(Paragraph("3. The Solution", s["h1"]))
    story.append(
        Paragraph(
            "SpideyOS is a single sideloadable APK companion suite for Android 15 (API 35)+. "
            "No custom ROM. No Magisk required. Users set Spidey Launcher as home, grant "
            "notification and overlay permissions, and Spidey becomes part of daily flow.",
            s["body"],
        )
    )
    data = [
        [Paragraph("<b>Module</b>", s["bullet"]), Paragraph("<b>What it does</b>", s["bullet"])],
        [Paragraph("Spidey Launcher", s["bullet"]), Paragraph("Web-themed home, app grid, open animations", s["bullet"])],
        [Paragraph("Spidey Assistant", s["bullet"]), Paragraph("Floating overlay + Gemini persona for Jaikar", s["bullet"])],
        [Paragraph("Notification Rewriter", s["bullet"]), Paragraph("Spidey-voiced alerts for mail &amp; messages", s["bullet"])],
        [Paragraph("Web Messages", s["bullet"]), Paragraph("Spider-web bubbles + send-line animation", s["bullet"])],
        [Paragraph("Mail Digest", s["bullet"]), Paragraph("New mail announced by Spidey", s["bullet"])],
        [Paragraph("Peter Camera", s["bullet"]), Paragraph("CameraX comic UI + Spidey photo reactions", s["bullet"])],
    ]
    t = Table(data, colWidths=[1.8 * inch, 4.7 * inch])
    t.setStyle(
        TableStyle(
            [
                ("BACKGROUND", (0, 0), (-1, 0), RED),
                ("TEXTCOLOR", (0, 0), (-1, 0), white),
                ("BACKGROUND", (0, 1), (-1, -1), WEB),
                ("GRID", (0, 0), (-1, -1), 0.4, MUTED),
                ("VALIGN", (0, 0), (-1, -1), "TOP"),
                ("LEFTPADDING", (0, 0), (-1, -1), 8),
                ("RIGHTPADDING", (0, 0), (-1, -1), 8),
                ("TOPPADDING", (0, 0), (-1, -1), 6),
                ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
            ]
        )
    )
    story.append(t)
    story.append(PageBreak())

    # Page 5 — Architecture
    story.append(Paragraph("4. Architecture", s["h1"]))
    story.append(
        Paragraph(
            "SpideyOS is modular inside one application process. The launcher hosts navigation; "
            "the assistant overlay and notification listener run as services; feature modules "
            "(messages, mail, camera) share theme tokens and a user profile (default display name: "
            "<b>Jaikar</b>).",
            s["body"],
        )
    )
    story.append(Paragraph("Data flow (simplified)", s["h2"]))
    story.append(
        Paragraph(
            "User → Launcher → Modules<br/>"
            "Spidey Overlay → Gemini API (persona system prompt)<br/>"
            "System Notifications → NotificationListener → Spidey voice copy → UI / overlay<br/>"
            "Mail / Messages → Digest → Spidey announcements<br/>"
            "CameraX capture → Spidey reaction prompts",
            s["body"],
        )
    )
    story.append(
        Paragraph(
            "This architecture prioritizes universal installability over full OS replacement — "
            "the honest path to “works on all Android 15+ phones and tablets.”",
            s["body"],
        )
    )
    story.append(PageBreak())

    # Page 6 — Features
    story.append(Paragraph("5. Feature Walkthrough", s["h1"]))
    story.append(Paragraph("Spidey voice notifications", s["h2"]))
    story.append(
        Paragraph(
            "With notification access enabled, SpideyOS rewrites selected alerts into character: "
            "“Spidey here — Jaikar, you’ve got mail.” Fun first, then actionable — tap through to "
            "the source app or Mail Digest.",
            s["body"],
        )
    )
    story.append(Paragraph("Web messages", s["h2"]))
    story.append(
        Paragraph(
            "Message bubbles use web-inspired geometry and a web-line send animation so "
            "conversation feels kinetic — matching the launcher’s motion language.",
            s["body"],
        )
    )
    story.append(Paragraph("Peter Camera", s["h2"]))
    story.append(
        Paragraph(
            "A CameraX viewfinder with comic-frame overlays and a web shutter. After capture, "
            "Spidey can react (“Nice shot — Daily Bugle material?”) via the assistant pipeline.",
            s["body"],
        )
    )
    story.append(PageBreak())

    # Page 7 — Stack
    story.append(Paragraph("6. Tech Stack &amp; Android 15", s["h1"]))
    story.append(
        Paragraph(
            "Minimum SDK: <b>Android 15 (API 35)</b>. Language: <b>Kotlin</b>. UI: <b>Jetpack Compose</b> "
            "+ Material 3. AI: <b>Google Gemini API</b>. Camera: <b>CameraX</b>. Persistence: DataStore. "
            "CI: GitHub Actions for debug/release APK artifacts.",
            s["body"],
        )
    )
    story.append(Paragraph("Performance story", s["h2"]))
    for b in [
        "Compose-first UI with restrained animation budgets for mid-range MediaTek devices.",
        "Same APK path for Snapdragon flagships — no vendor-specific forks required for MVP.",
        "Adaptive layouts for tablets (width windows) without a separate tablet codebase.",
        "Baseline profile / R8 ready for release builds.",
    ]:
        story.append(Paragraph(f"• {b}", s["bullet"]))
    story.append(PageBreak())

    # Page 8 — Open source
    story.append(Paragraph("7. Open Source &amp; Install", s["h1"]))
    story.append(
        Paragraph(
            "Distribution target: <b>GitHub Releases</b> APK. Users enable Unknown Sources / "
            "sideload, install SpideyOS, optionally set as default launcher, and grant "
            "Notification access + Display over other apps for the full Spidey experience.",
            s["body"],
        )
    )
    for b in [
        "No root / Magisk required for core features.",
        "Works across OEM skins that ship Android 15+.",
        "MIT-style code license with clear Marvel non-affiliation disclaimer.",
        "Install guide and architecture docs live in the repository.",
    ]:
        story.append(Paragraph(f"• {b}", s["bullet"]))
    story.append(PageBreak())

    # Page 9 — Skills
    story.append(Paragraph("8. What I Built &amp; Learned", s["h1"]))
    story.append(
        Paragraph(
            "SpideyOS is a portfolio-grade demonstration of end-to-end Android product engineering:",
            s["body"],
        )
    )
    for b in [
        "Kotlin &amp; modern Android (API 35)",
        "Jetpack Compose UI + custom motion (web motifs)",
        "System integration: NotificationListenerService, launcher HOME intent, overlays",
        "Generative AI product design (Gemini persona, latency-aware UX)",
        "CameraX custom camera experience",
        "Cross-SoC thinking (Snapdragon + MediaTek) and tablet adaptivity",
        "Open-source release engineering &amp; technical storytelling",
    ]:
        story.append(Paragraph(f"• {b}", s["bullet"]))
    story.append(PageBreak())

    # Page 10 — CTA
    story.append(Paragraph("9. Call to Action", s["h1"]))
    story.append(
        Paragraph(
            "Star the repo. Install the APK. Tell me what Spidey should say next.",
            s["cta"],
        )
    )
    story.append(Spacer(1, 0.3 * inch))
    story.append(
        Paragraph(
            "GitHub: <b>YOUR_GITHUB_URL</b><br/>Author: <b>Jaikar</b><br/>"
            "Open to Android / mobile / AI product engineering conversations.",
            s["cta"],
        )
    )
    story.append(Spacer(1, 0.5 * inch))
    story.append(Paragraph("Disclaimer", s["h2"]))
    story.append(
        Paragraph(
            "SpideyOS is an unofficial, fan-inspired project created for learning, portfolio, and "
            "community use. It is not affiliated with, endorsed by, or sponsored by Marvel "
            "Entertainment, Sony Pictures, or related trademark holders. Use original or licensed "
            "assets only; do not redistribute copyrighted Marvel artwork.",
            s["body"],
        )
    )
    story.append(Spacer(1, 0.4 * inch))
    story.append(
        Paragraph(
            "With great power comes great responsibility — and great commit messages.",
            s["quote"],
        )
    )

    doc = SimpleDocTemplate(
        str(OUT),
        pagesize=letter,
        leftMargin=0.75 * inch,
        rightMargin=0.75 * inch,
        topMargin=0.7 * inch,
        bottomMargin=0.7 * inch,
        title="SpideyOS Case Study — Jaikar",
        author="Jaikar",
    )

    def first_page(canvas, doc_):
        draw_cover(canvas, doc_)

    def later_pages(canvas, doc_):
        draw_inner(canvas, doc_)

    doc.build(story, onFirstPage=first_page, onLaterPages=later_pages)
    print(f"Wrote {OUT}")


if __name__ == "__main__":
    build()
