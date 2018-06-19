# Guild-ilvl-note
This is a addon for the game World of Warcraft that fetch the ilvl from all guild memebers in the given guild and puts it in the note of the character.

The ilvl-fetcher is a multi-thread java program that contacts directly with Blizzards database. It processes and saves the data as a ready to go addon LUA file (Core.lua). The addon will use the data to then update all guild memebers notes on character load.

Only works on EU.

Requiers a Blizzard API key.
