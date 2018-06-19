# Guild ilvl
This is a addon for the game World of Warcraft that fetches the ilvl from all guild memebers in the given guild and puts it in the note of the characters.

The ilvl-fetcher is a multi-thread java program that contacts directly with Blizzards database through the Blizzard API. It processes and saves the data as a *ready to go* addon LUA file (Core.lua). The addon will use the data to then update all guild memebers notes on character load.



## Getting Started
Download this repository in your World of Warcraft addon folder, default path:
```
C:\Program Files (x86)\World of Warcraft\Interface\AddOns\Guild_ilvl
```
Next you need the Blizzard API key, you can get one [here](https://dev.battle.net/member/register).

Now you need to insert your API key in the Fetcher.java, example:
```
[line 20] String blizzard_API_key = "arvutnia6ja8wemrn54e8w3xcvy"
```
Finally you can compile:
```
>>> javac *.java
```

## Running
All you need is the realm and guild name, here's an example on my guild:
```
>>> java Main "Ravencrest" "Stand In Fire DPS Higher"
```
The Core.lua file will be created or updated, and is ready to go

## Things you should know
* It only works on EU.
* You need a Blizzard API key
* You need to have permission to edit public guild notes
* I recommend to have the addon enabled on characters in one and the same guild
