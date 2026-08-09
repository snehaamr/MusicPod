#!/usr/bin/env python3

import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request


BASE_URL = os.getenv(
    "MUSICPOD_BASE_URL",
    "http://localhost:8080"
).rstrip("/")

TOKEN = os.getenv("MUSICPOD_TOKEN")


CATALOG = [
    {
        "artist": "Asha Bhosle",
        "album": "Umrao Jaan",
        "releaseDate": "1981-01-01",
        "tracks": [
            {
                "title": "In Aankhon Ki Masti",
                "trackNumber": 1,
                "durationMs": 307000,
                "explicit": False
            },
            {
                "title": "Dil Cheez Kya Hai",
                "trackNumber": 2,
                "durationMs": 360000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Lata Mangeshkar",
        "album": "Guide",
        "releaseDate": "1965-01-01",
        "tracks": [
            {
                "title": "Aaj Phir Jeene Ki Tamanna Hai",
                "trackNumber": 1,
                "durationMs": 238000,
                "explicit": False
            },
            {
                "title": "Piya Tose Naina Laage Re",
                "trackNumber": 2,
                "durationMs": 510000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Kishore Kumar",
        "album": "Aradhana",
        "releaseDate": "1969-01-01",
        "tracks": [
            {
                "title": "Mere Sapno Ki Rani",
                "trackNumber": 1,
                "durationMs": 300000,
                "explicit": False
            },
            {
                "title": "Roop Tera Mastana",
                "trackNumber": 2,
                "durationMs": 225000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Shreya Ghoshal",
        "album": "Devdas",
        "releaseDate": "2002-01-01",
        "tracks": [
            {
                "title": "Bairi Piya",
                "trackNumber": 1,
                "durationMs": 320000,
                "explicit": False
            },
            {
                "title": "Dola Re Dola",
                "trackNumber": 2,
                "durationMs": 390000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "S. P. Balasubrahmanyam",
        "album": "Maine Pyar Kiya",
        "releaseDate": "1989-01-01",
        "tracks": [
            {
                "title": "Mere Rang Mein Rangne Wali",
                "trackNumber": 1,
                "durationMs": 410000,
                "explicit": False
            },
            {
                "title": "Dil Deewana",
                "trackNumber": 2,
                "durationMs": 315000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Arijit Singh",
        "album": "Aashiqui 2",
        "releaseDate": "2013-01-01",
        "tracks": [
            {
                "title": "Tum Hi Ho",
                "trackNumber": 1,
                "durationMs": 262000,
                "explicit": False
            },
            {
                "title": "Hum Mar Jayenge",
                "trackNumber": 2,
                "durationMs": 305000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Sonu Nigam",
        "album": "Kal Ho Naa Ho",
        "releaseDate": "2003-01-01",
        "tracks": [
            {
                "title": "Kal Ho Naa Ho",
                "trackNumber": 1,
                "durationMs": 321000,
                "explicit": False
            },
            {
                "title": "Maahi Ve",
                "trackNumber": 2,
                "durationMs": 365000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Ben E. King",
        "album": "Don't Play That Song!",
        "releaseDate": "1962-01-01",
        "tracks": [
            {
                "title": "Stand by Me",
                "trackNumber": 1,
                "durationMs": 180000,
                "explicit": False
            },
            {
                "title": "Don't Play That Song",
                "trackNumber": 2,
                "durationMs": 173000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Bee Gees",
        "album": "Spirits Having Flown",
        "releaseDate": "1979-01-01",
        "tracks": [
            {
                "title": "Tragedy",
                "trackNumber": 1,
                "durationMs": 305000,
                "explicit": False
            },
            {
                "title": "Too Much Heaven",
                "trackNumber": 2,
                "durationMs": 295000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Jim Croce",
        "album": "You Don't Mess Around with Jim",
        "releaseDate": "1972-01-01",
        "tracks": [
            {
                "title": "Time in a Bottle",
                "trackNumber": 1,
                "durationMs": 148000,
                "explicit": False
            },
            {
                "title": "Operator",
                "trackNumber": 2,
                "durationMs": 225000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "The Beatles",
        "album": "Abbey Road",
        "releaseDate": "1969-01-01",
        "tracks": [
            {
                "title": "Come Together",
                "trackNumber": 1,
                "durationMs": 259000,
                "explicit": False
            },
            {
                "title": "Here Comes the Sun",
                "trackNumber": 2,
                "durationMs": 185000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Stevie Wonder",
        "album": "Songs in the Key of Life",
        "releaseDate": "1976-01-01",
        "tracks": [
            {
                "title": "Sir Duke",
                "trackNumber": 1,
                "durationMs": 234000,
                "explicit": False
            },
            {
                "title": "Isn't She Lovely",
                "trackNumber": 2,
                "durationMs": 394000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Bryan Adams",
        "album": "Reckless",
        "releaseDate": "1984-01-01",
        "tracks": [
            {
                "title": "Summer of '69",
                "trackNumber": 1,
                "durationMs": 216000,
                "explicit": False
            },
            {
                "title": "Heaven",
                "trackNumber": 2,
                "durationMs": 243000,
                "explicit": False
            }
        ]
    },
    {
        "artist": "Queen",
        "album": "A Night at the Opera",
        "releaseDate": "1975-01-01",
        "tracks": [
            {
                "title": "Bohemian Rhapsody",
                "trackNumber": 1,
                "durationMs": 354000,
                "explicit": False
            },
            {
                "title": "You're My Best Friend",
                "trackNumber": 2,
                "durationMs": 171000,
                "explicit": False
            }
        ]
    }
]


def request(method, path, payload=None):

    url = BASE_URL + path

    headers = {
        "Accept": "application/json"
    }

    if payload is not None:
        headers["Content-Type"] = "application/json"

    if TOKEN:
        headers["Authorization"] = f"Bearer {TOKEN}"

    data = None

    if payload is not None:
        data = json.dumps(payload).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=data,
        headers=headers,
        method=method
    )

    try:
        with urllib.request.urlopen(req) as response:

            body = response.read().decode("utf-8")

            if not body:
                return None

            return json.loads(body)

    except urllib.error.HTTPError as error:

        body = error.read().decode("utf-8")

        print(
            f"\nHTTP {error.code}: {method} {path}"
        )

        if body:
            print(body)

        raise


def extract_items(response):

    if response is None:
        return []

    if isinstance(response, list):
        return response

    if not isinstance(response, dict):
        return []

    # Handles common pagination response formats.
    for key in (
        "content",
        "items",
        "results",
        "data"
    ):

        value = response.get(key)

        if isinstance(value, list):
            return value

    return []


def normalize(value):

    if value is None:
        return ""

    return str(value).strip().casefold()


def find_artist(name):

    response = request(
        "GET",
        "/api/v1/artists?page=0&size=100"
    )

    for artist in extract_items(response):

        if normalize(artist.get("name")) \
                == normalize(name):

            return artist

    return None


def find_album(artist_id, title):

    response = request(
        "GET",
        f"/api/v1/artists/{artist_id}/albums"
        "?page=0&size=100"
    )

    for album in extract_items(response):

        if normalize(album.get("title")) \
                == normalize(title):

            return album

    return None


def find_track(album_id, title):

    response = request(
        "GET",
        f"/api/v1/albums/{album_id}/tracks"
        "?page=0&size=100"
    )

    for track in extract_items(response):

        if normalize(track.get("title")) \
                == normalize(title):

            return track

    return None


def ensure_artist(name):

    existing = find_artist(name)

    if existing:
        print(
            f"  artist exists: {name}"
        )

        return existing

    print(
        f"  creating artist: {name}"
    )

    return request(
        "POST",
        "/api/v1/artists",
        {
            "name": name
        }
    )


def ensure_album(
        artist_id,
        title,
        release_date):

    existing = find_album(
        artist_id,
        title
    )

    if existing:
        print(
            f"    album exists: {title}"
        )

        return existing

    print(
        f"    creating album: {title}"
    )

    return request(
        "POST",
        f"/api/v1/artists/{artist_id}/albums",
        {
            "title": title,
            "releaseDate": release_date
        }
    )


def ensure_track(
        album_id,
        track):

    existing = find_track(
        album_id,
        track["title"]
    )

    if existing:
        print(
            f"      track exists: "
            f"{track['title']}"
        )

        return existing

    print(
        f"      creating track: "
        f"{track['title']}"
    )

    return request(
        "POST",
        f"/api/v1/albums/{album_id}/tracks",
        track
    )


def seed():

    print()
    print(
        f"Seeding MusicPod catalog at "
        f"{BASE_URL}"
    )
    print()

    created_or_found_tracks = 0

    for entry in CATALOG:

        print(
            f"Artist: {entry['artist']}"
        )

        artist = ensure_artist(
            entry["artist"]
        )

        artist_id = artist["id"]

        album = ensure_album(
            artist_id,
            entry["album"],
            entry["releaseDate"]
        )

        album_id = album["id"]

        for track in entry["tracks"]:

            ensure_track(
                album_id,
                track
            )

            created_or_found_tracks += 1

        print()

    print(
        "--------------------------------"
    )

    print(
        f"Demo catalog ready: "
        f"{len(CATALOG)} artists, "
        f"{created_or_found_tracks} tracks"
    )

    print(
        "--------------------------------"
    )


if __name__ == "__main__":

    try:
        seed()

    except urllib.error.URLError as error:

        print()
        print(
            "Unable to reach MusicPod."
        )
        print(error)
        sys.exit(1)

    except Exception as error:

        print()
        print(
            f"Catalog seed failed: {error}"
        )
        sys.exit(1)
