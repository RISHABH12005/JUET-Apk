#!/usr/bin/env python3
"""Sync public JUET announcements into a small JSON feed for the Android app."""
import json
import re
from datetime import datetime, timezone
from html.parser import HTMLParser
from urllib.parse import urljoin
from urllib.request import Request, urlopen

BASE = "https://www.juet.ac.in"
PAGES = [
    ("announcements", BASE + "/jaypee/Announcement"),
    ("calendar", BASE + "/jaypee/Acalendar"),
]
OUT = "juet_public_info.json"

class AnchorParser(HTMLParser):
    def __init__(self):
        super().__init__()
        self.href = None
        self.parts = []
        self.items = []
    def handle_starttag(self, tag, attrs):
        if tag == "a":
            self.href = dict(attrs).get("href", "")
            self.parts = []
    def handle_data(self, data):
        if self.href is not None:
            self.parts.append(data.strip())
    def handle_endtag(self, tag):
        if tag == "a" and self.href is not None:
            title = re.sub(r"\s+, " ", " ".join(x for x in self.parts if x)).strip()
            if title and self.href and not title.lower() in {"view file", "click here"}:
                self.items.append((title, urljoin(BASE, self.href)))
            self.href = None
            self.parts = []

def fetch(url):
    req = Request(url, headers={"User-Agent": "JUET-App-public-info-sync/1.0"})
    with urlopen(req, timeout=25) as response:
        return response.read().decode("utf-8", "replace")

def main():
    announcements = []
    for section, page in PAGES:
        try:
            parser = AnchorParser()
            parser.feed(fetch(page))
            for title, url in parser.items:
                if section == "announcements":
                    if any(word in title.lower() for word in ("result", "hostel", "fee", "registration", "internship", "calendar", "exam", "scholarship", "notice", "placement", "accident", "announcement")):
                        announcements.append({"title": title, "date": "From official JUET public website", "url": url})
                elif "academic calendar" in title.lower():
                    announcements.append({"title": title, "date": "Academic calendar", "url": url})
        except Exception as exc:
            print(f"Warning: could not fetch {page}: {exc}")
    # Stable de-duplication, newest page links first.
    seen, unique = set(), []
    for item in announcements:
        key = (item["title"].casefold(), item["url"])
        if key not in seen:
            seen.add(key)
            unique.append(item)
    if not unique:
        raise SystemExit("No official JUET links were extracted; refusing to overwrite feed with empty data.")
    payload = {
        "updatedAt": datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC"),
        "source": BASE + "/jaypee/Announcement",
        "announcements": unique[:60],
    }
    with open(OUT, "w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)
        handle.write("\n")
    print(f"Wrote {len(payload['announcements'])} public links to {OUT}")

if __name__ == "__main__":
    main()
