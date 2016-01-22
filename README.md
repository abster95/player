#Intro

## What is this

  The <b>Player</b> or <b>PlayerFX</b> is a desktop audio player and audio management application, which also turned into sort of dynamic module system - a multipurpose extensible application capable of compiling and running custom java widgets.

I did not scare you away with that, did I?

## Am I the target group?

There are two reasons to be interested in this project:
- as an <b>audio management system</b>. You may ask, why another audio player? Because there is significant lack of such application for a power user. It's not just about hitting a play button. Ever wanted to use timed comment? Ever lost your entire library song ratings due to corrupted database file? Ever needed multiple playlists or display/manipulate your songs in a way that just wasnt possible for some reason? This audioplayer addresses a lot of such issues.

- as a <b>multiapplication</b> - collection of unrelated miniapplications. Image browser, movie explorer, file renamer and more. And if you know java, you can make your own app with simple text editor and literaly dozen lines of code - without java, without IDE and without hassles of creating and deploying your application - just write your code, hit save and watch as it autocompiles and runs as widget, which you can run as a standalone application! All that with included support for configurations, skins and everything else.

## Motto

- <b>Customizability</b> - User uses the application how he wants, not how it was designed to be used. Therefore emphasis on customization, skins, settings, etc.
- <b>Portability</b> - No installation (or need for java or other programs), run from anywhere, little/no trace, everything is packaged along (no hunting your library database file in hidden Windows directories... 
- <b>Modular functionality</b> - User can launch or use only selected components he is interested in and ignore everything else as if it was never there.
- <b>Modular user interface</b> - User has the ability to 'make his own gui'. Completely custom component layout. He can create all-in-one GUI or use separate windows or - anything really.
- <b>Fancy features</b> like: rating in tag, time comments, good image support, advanced & intuitive library management, etc.
- <b>Library independence<b> - Moving & renaming files will not result in loss of any information. Every single bit is in the tag. Always. If you move on to different application or lose your library - you never lose data. Ever again.
- <b>Usability</b> - Ease of use and efficient workflow due to minimalistic and unobtursive graphical user interface design. Think shortcuts, swiping, icons instead buttons, closing with right click instead of trying to hit small button somewhere in the corner of whatever you are doing..., etc.
- <b>Responsive</b> - fast and responsive. Minimal modal dialogs. No more stuck windows while your library is scanning that big fat audio collection of yours.
- <b>Sexy</b> - your way of sexy if you know tiny bit about css.

  Okey, okey so now, what can this application actually do?
Glad you asked.

## Features

### Play audio 

Filetypes:
- mp3, mp4, m4a, wav, ogg, flac
- possibly more to come

Protocols:
- file
- http: playback over internet. The support is limited to what javaFX can currently do (no flac & ogg).

###Manage audio files

Song database:
- small footprint: in 10s of MBs for 10000s audio files
- big: 40000 files no problem
- fast: library is loaded into main memory (RAM).
- no dependency: song files always store all data in their tag, moving or renaming files poses no problem.
- no inconsistencies: displayed song metadata can only be out of sync with real data if tag is edited by external application (or as a result of a bug)
- no data loss guarantee: losing database has no effect at all, it can be completely rebuilt anytime. The library serves as a persistable cache, not as data storage.

  Management system is only as good as its user interface. There are powerful tables that try to be as flexible as possible. Some of the capabilities that <b>every</b> table boasts:
- big: 30000 songs in playlist no problem (although good luck loading it at the app start...)
- smart columns: set visibility, width, sorting (multiple column), order of any column for any song attribute
- visual searching: by any (textual) attribute (artist, composer, title, etc) simply by writing. Scrolls 1st match (to center) and highlights matches - so they 'pop' visually - shich doesnt strain your eyes that much. Its fast (no CTRL+F, just type...) and convenient.
- powerful filtering - CTRL+F. Shows only matches. Filtering here is basically constructing logical predicates (e.g.: 'year' 'less than' '2004') and it is possible to use <b>any</> combination of attributes (columns), 'comparators' and permissible values. Filters can be inverted (negation) or chained (conjunction).
- group by - e.g. table of groups of songs per attribute (e.g. year or artist) Searching, filtering and sorting fully supported of course.
- multiple column sorting by any attribute (artist, year, rating, bitrate, etc)
- cascading - link tables to other tables as filters and display only selected items (e.g. show songs of autor A's  albums X,D,E in year Y in three linked tables reacting on table selection). Basically library widgets allow linking selection of the table as an input, while simultaneously providing its selection as an output to other tables. Use however library widgets (each having 1 table) you wish and link them up in any way you want.

###Audio tag editing

Application supports 
- <b>reading</b>
- <b>writing</b>

of song tags

- individually
- by group (either using Tagger to tag single value to tag field of multiple songs (songs may share an artist) or Converter to write different value for each song (songs dont share title).

The supported are:
- all file types (see at the top), including wma and mp4 (which normally can not have a tag)
- all fields (comprehensive list later), including rating, playcount, color, timed comments.

The aim is to be interoperable with other players, where possible. Noteworthy or nonstandard supported tags include:

  **Rating** 
- values are in percent values independent of implementation (mp3=0-255, flac/ogg=0-100)
- floating values (0-1). Values like {1,2,3,4,5} are obsolete, illogical and nobody agrees on what they mean. Use full granularity (limited only by tag (1/255 for mp3, 1/100 for other formats)) and pick graphical representation (progress bar or any number of "stars" you want). Basically rate 1) how you want 2) all audio types the same 3) be happy the value is in the tag 4) visualie the rating value as you want - be it 3 stars or 10 or a progress bar.
- interoperable with other players (POPM frame), but most of them will only recognize the value in their own way
  
  **Playcount**
- number of times the song has been played (the exact definition is left upon the user, who can set up the playcount incrementation behavior arbitrarily, or edit the value manually (increment/decrement/set arbitrary number - its your collection, excert your power!).
- the data are written in custom tag (in mp3, writen duplicitly in POPM frame counter)
  
  **Time comments/chapters**
- comments associated with specific time/part of the song. They can be added during playback on the seeker and viewed in popup menus. The length of the comment should be a non-issue (the upper value is unknown, but should be enough).
- The gui makes it really easy to add or edit these and takes no space, since it is using seeker bar and popup windows.
- the data are written in custom tag

  **Color**
- just in case you want to associate songs with a colors, you can.
- using custom tag

  **Cover**
- image in tag can be imported/exported (but I advise against placing images in audio tags, it is impractical (large space requirements - it adds up..) and semantically incorrect (cover is album metadata, not song metadata)).
- cover read from file location is supported too, looking for image files named:
  - song title.filetype
  - song album.filetype
  - cover.filetype or folder.filetype
  
### Configurability

  All settings and entire user interface layout serialize into a human readable and editable files. These can be edited, backed up or switched between applications.

### Modularity

  Most of the functionalitiess are implemented as widgets, that can be loaded, closed, moved and configured separately. Multiple instances of the same widget can run at once in windows, layouts or popups. Widgets' source files can be created and edited in runtime and any changes will be immediatelly reflected in the application. This means that if you are a developer you just edit the file, hit save and watch as the witgets are reloaded with previous state and configuration. 
  
  Some of the existing widgets are:
- Playback & Mini - controls for playback, like seeking. Supports chapters.
- FileInfo - shows cover and information about song (e.g. playing). Allows cover download on drag&drop.
- Tagger - for tagging
- Library & LibraryView - song tables
- ImageViewer - shows images associated with the songs, supports subfolders when discovering the images
- Settings - application settings, but it is also a generic configurator for any object exposing its Configurable API (like widgets or even GUI itself)
- Converter - object -> object converting. Displays objects as text while allowing user to apply functions transformations. Handy file renamer and per-song tagger. Supports object lists, text transformations, manual text editing, regex, writing to file etc.
- Explorer - simple file system browser. Nothing bigCurrently slow for big folders.
- Inspector - displays hierarchies, like file system or gui scene graph. 
- Icon - fully configurable icon bar. Icons can execute any (supported) application action.

### Portability

  The application in its self-contained form:
- has executable .exe
- requires no installation
- requires no software (e.g. java) preinstalled
- runs from anywhere
- does not require internet access
- does not write to registry or create files outside its directory (except for some cache & temporary files)

### GUI

  UI is minimalistic but powerful and fully modular. Modules (widgets) are part of layout hierarchy, which can be manipulated, saved or even loaded as standalone application. 

- minimalistic - shows only whats important, no endless headers and borders taking up important space. With headerless and borderless window mode 100% of the space is given to the widgets.
- powerful - infinite virtual space, horizontally scrollable, zoomable
- layout mode - powerful mode displaying 2nd ui layer allowing user to edit and configure the layout, widgets and more, alleviating normal user interface from all this
- completely skinnable (css)(skin discovery + change + refresh does not require application restart)

Widgets:
- can provide input and output (e.g. playlist table has selected song as output)
- inputs and outputs can be bound - when output value changes, it is passed into the listening input of other widget
- inputs can be set to custom values
- the whole system is displayed visually as editable graph

Layouts:
- widget management: Gui gives the ability to divide layouts into containers for widgets. These allow resizing, positioning, swapping, adding, removing and many more widget operations
- multiple layout support/virtual layout space. Switching layouts by dragging them horizontally (left,right) opens new space for more layouts. This provides virtually infinitely large and conveniently navigable working space.

Windows:
- snap to screen edges and other windows, screen-to-screen edges also supported.
- auto-resize when put into screen edges and corners (altogether 7 different modes - all, left/right half, right half, topleft/topright/bottomleft/bottomright quadrant)
- system tray, taskbar, fullscreen, always on top
- mini mode - a docked bar snapped to the top edge of the screen
- multiple screen support
- multiple windows
- configurable notification positions (corners, center) + window/screen oriented

### Hotkeys

- global hotkey supported - shortcuts dont need application focus if so desired
- media keys supported
- customizable (any combination of keys:  "F5", "CTRL+L", etc)
- large number of actions (playback control, layout, etc)

### Usability
- navigation: No more back and up buttons. Use left and right mouse butttons to quickly and seamlessly navigate within user interface.
- icons: No ugly buttons doing the unexpected. Icons are designed to visually aid user to understand the action. Decorated with tooltips. Some are also skinnable or change to visualize application state.
- tooltips: Everywhere. And big too. Explain all kinds of functionalities, so read to your heart's content. There are also info buttons opening information popups.
- units: No more '5000 of what questions', everything that needs a unit has a unit, e.g. filesize (kB,MB,...), time duration, bitrate, etc. Multiple units are supported when possible, e.g., using 2000ms or 2s has the same effect. This is all supported in application settings or in table filter queries.  
- validation: Designed to eliminate input errors by preventing user to input incorrect data. Warning icons signal incorrect input. Really helps with writing regular exressions.
- defaults: Every settings has a default value you can revert to easily.
- shortcuts: Quick & easy control over the application anytime.
- smart ui: Notifications that can be closed when they get in the way or keep being open when mouse hovers over. Throw notifications manually or put whole widgets in it. Tables or docked window that monitor user activity. Clickless ui reacting on mouse hover rather than click

### More

- configurable playcount incrementing strategy: at specified minimal time or percent of song playback
- cover downloading on drag&drop
- animations & effects
- crisp images in any size, be it 100x100px thumbnail or mammoth image displayed on your new big display. Images load quickly and dont cause any lag (not even the big ones), images around 5000px are handled just fine (just dont look at memory).

Platforms:
- Windows
- Linux (not thoroughly tested, no support for global shortcuts)
- Mac (untested)

## Screenshots

![ScreenShot](/extra/screenshot1.png)

![ScreenShot](/extra/screenshot3.png)

## The Catch XXII

- Some of the widgets or features are **experimental**, buggy or confusing (its being worked on, so stay tuned).
- Linux and Mac not tested for now.
- Memory consumption is worse than what native applications could do. Normally i have get about 250-450MB, but it depends on use case. Lots of widgets will eat more memory. Handling large pictures (4000^2 px) on large monitors can also rapidly increase memory consumption (but picture quality stays great). 32bit is more effective (64bit effectively doubles memory consumption), so ill only provide 32-bit version.
- No playlist  support (.m3u, etc) for now. Maybe later.
- Using shadows on text or icons (in custom skins) can severely impact performance, but i think that goes true for any app.
- no transparent bgr for now (due to java bug causing massive performance degradation)
- visually big tables with lots of text can impact performance (we are talking full-hd and beyond)

## Download & Use

Download link coming soon.

- download zip
- extract anywhere
- run the exe file

  Starting the application for the first time will run a guide. Before you close it, read at least first couple of tips (like where to find the guide if you need it again...).

Tips:
- use tooltips! Cant stress enough.
- If you get 'trapped' and 'locked in' with no idea what to do, press right ALT (layout edit mode) or click anywhere (mouse buttons often navigate) - once you get the hang of it, you will see how convenient it is.
- widgets, popups and containers have informative "i" buttons that provide valuable info on possible course of action

### Contribution

There are several areas that one can contribute to:
- application core - involves java & javaFX code, OOP + Functional + Reactive styles
- skins - requires very basic knowledge of css and a some of patience
- widgets - involves java & javaFX code & knowledge of the APIs
- testing & bug reporting
- feedback and spreading the word

## Development

### Project set up

- JDK

Download and install latest 32-bit java9 build from [java kenai](https://jdk9.java.net/download/). Using 64-bit JDK/JRE is possible, but provides no benefits and causes high memory usage.

- IDE

Any IDE should work. THis project is being developed on Netbeans 9, 32-bit (64-bit is memory hungy for no benefit). Netbeans 9 is mandatory (to run java 9). If you have the IDE, download and open this project in it.

- Dependencies

All dependencies are included in this repository. Import all libraries in /extra/lib directory. Similarly import the projects in /extra/projects as you did the libraries ( in Netbeans its in project > properties > libraries > add project). If any of the projects is mising a library, open the project and import the library from /extra/projects_lib.

The library dependencies are all mandatory for the project to compile. The compilation will succeed without the project dependencies, but they are required for playback to work for certain audio file types (e.g. flac, ogg).

- working directory

It is imperative for working directory to be set up for the project to run successfully. Use /working dir located in this repository.

- widgets

It is required to add widget source codes to the source code of the project. In Netbeans: project > properties > sources > add folder. Add /src widgets directory. You should see 2 source directories in you project: 'src' and 'src widgets'.


- VM run options
  - "-Xmx1g" gets rid of memory errors when loading large images, the 1g (1GB) value can be less, but this works. Not using this parameter at all will result in some functionalities not working properly.

- annotation processing

The project makes heavy use of annotations and even annotation processor. It requires annotation processing to be enabled. In netbeans: project > properties > build > compiling  select both Enable annotation Processing and Enable Annotation Processing in Editor. Should be enabled by default.

- other options (should not be necessary since they are default settings, but just in case)
  - dont binary encode javaFX css files (it will prevent skins from working since this changes .css to .bss and causes paths no longer work
  - assertion dont have to be enabled

- Set main.App class as main class in project properties, if not already set

### Coding style

- Logging

This project uses slf4j logging facade and bindings to logback library. To log, simply create static final instance of org.slf4j.Logger using org.slf4j.LoggerFactory. All logging configuration is in an xml in /working dir/log, where the log files are also located. The logger is configured to log WARN and ERROR levels to the file and in addition log every logging level to console.

- Imports
  - use static imports where possible (aim for short code, IDEs can guide us where the object/method comes from), particularly for enum types, but also utility methods (Math, etc).
  - separate imports and static imports (Netbeans > Tools > Options > Formatting > Imports)
  - separate imports (java, javax, javafx packages or more if you like)
  - no package imports and group imports if possible (causes problems when classes share same name)

Setting IDE to auto-collapse imports is recommended.

- Assertions

This project doess not use any. They are disabled by default, thus unreliable. Use runtime exceptions (e.g. AssertionError) or methods like Objects.requireNonNull(). There is bunch of similar methods in util package.

- Comments

Try to write javadoc for every public method and class. Use simple comments (//) to provide intent of the code.
Setting IDE to auto-collapse javadoc is recommended.

The provided files are
- source files
- working directory containing application data.
- dependencies (libraries and projects to import)

### Skinning

A skin is a single css file that works the same way as if you are skinning a html web site. [javafx css reference guide](http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html) is an official reference guide that contains a lot of useful information.
The application autodiscovers the skins when it starts. The skins are located in Skins directory, each in its own folder. Press F5 (by default) to refresh skin changes.

## Credits & Licence

You are free to use the application or make your own builds of the project for personal use.

The project is to adopt MIT licence in the future, but for now remains personal. I would appreciate to be
informed before taking any actions that could result in publicizing or sharing this project.

The project makes use of work of sevaral other individuals (with their permission), who will be properly credited later as well.

El Psy Congroo
