# This is a dead project. I don't like it, it's not fun, and the story I was building it for shouldn't even be interactive in the first place.

<details>
 <summary>original readme</summary>
 
## Abstract
This is a somewhat interactive simulator for digging through the archives
of a fictional messaging service and reading snippets from old conversations.
The aim is simply to learn about the characters presented through those
conversations.

## Install
From the 'releases' tab on github, you can download a zip file of the newest
version of this program. That file has all the components necessary to run
this program. Simply extract them all into the same folder. You should then have
these three files.
 - `criminal mischief.exe`
 - `config.txt`
 - `jre` folder
 
As long as these all stay within the same folder, everything should run smoothly.

*note 1: This zip file can also be located in out/artifacts/zip, but be aware that
the version located there is going to be newer, and not necessarily stable.*

*note 2: I do not currently have the program set up to use any JRE other than
the included one, so please keep them in the same folder.*

## Usage
The `config.txt` file is how a user will actually interact with the program. I will
add a help feature to the console in a later version for a better explanation,
but I'll include a simple explanation here. When run, the executable will read
the config file and use the filtering settings therein to select a snippet to
display. It will then open a Windows command console and display that snippet.
This terminal is not interactive, though it can be paused by clicking anyhwere
within the window.

### a note about snippet storage
Internally, all snippets are contained in the src/jason/storyteller/snippets
directory. I've added this folder to the .gitignore, though, so that people
can't accidentally (or intentionally) spoil themselves.
 </details>
